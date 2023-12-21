package org.patonki.main;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.patonki.blocks.ArrayBlocks;
import org.patonki.blocks.Blocks;
import org.patonki.blocks.OctTreeBlocks;
import org.patonki.citygml.citygml.CityGmlEndpoint;
import org.patonki.converter.MinecraftWorldWriter;
import org.patonki.converter.SchematicCreator;
import org.patonki.data.BlockSerializer;
import org.patonki.data.Classification;
import org.patonki.decorator.WorldDecorator;
import org.patonki.groundcolor.GroundColorEndpoint;
import org.patonki.las.LASEndPoint;
import org.patonki.openstreetmap.OsmEndPoint;
import org.patonki.settings.Settings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class WorldBuilder {
    private final LASEndPoint LASEndPoint;
    private final GroundColorEndpoint groundColorEndpoint;
    private final ExecutionStep[] skippedSteps;

    private final ExecutionStep[] cachedSteps;
    private final OsmEndPoint osmEndPoint;

    private final CityGmlEndpoint cityGmlEndpoint;
    private final WorldDecorator decorator;
    private final SchematicCreator schematicCreator;
    private final MinecraftWorldWriter worldWriter;

    private static final Logger LOGGER = LogManager.getLogger(WorldBuilder.class);
    private final String cacheFolderPath;
    private final BlockSerializer serializer;
    private final ExecutionStep startStep;
    private final ExecutionStep endStep;

    private static final Level STEP = Level.forName("STEP", 250);
    private final boolean overwrite;
    private final boolean deleteOldCache;

    private WorldBuilder(Settings settings, MinecraftWorldWriter worldWriter,
                         String cacheFolderPath, String aerialImagePath, String landUsePath,
                         String roadsPath, String waterwaysPath, String cityGmlDownloadFolder, String texturesPath,
                         ExecutionStep startStep, ExecutionStep endStep, ExecutionStep[] skippedSteps,  ExecutionStep[] cachedSteps, boolean multiThreaded,
                         boolean overwrite, boolean deleteOldCache) {
        this.overwrite = overwrite;
        this.deleteOldCache = deleteOldCache;
        this.LASEndPoint = new LASEndPoint(settings.getLasSettings());
        this.groundColorEndpoint = new GroundColorEndpoint(settings.getGroundColorSettings(), texturesPath, aerialImagePath);
        this.cachedSteps = cachedSteps;
        this.skippedSteps = skippedSteps;
        this.osmEndPoint = new OsmEndPoint(settings.getOsmSettings(), landUsePath, roadsPath, waterwaysPath);
        this.cityGmlEndpoint = new CityGmlEndpoint(cityGmlDownloadFolder, texturesPath,
                settings.getGmlSettings(), multiThreaded, settings.getLasSettings().mapToBlock(Classification.BUILDING), settings.getLasSettings().getRoofBlock());
        this.startStep = startStep;
        this.endStep = endStep;
        this.decorator = new WorldDecorator();
        this.schematicCreator = new SchematicCreator();
        this.worldWriter = worldWriter;
        this.cacheFolderPath = cacheFolderPath;
        this.serializer = settings.getLasSettings().useOctTree()
                ? new OctTreeBlocks.OctTreeBlocksSerializer()
                : new ArrayBlocks.ArrayBlockSerializer();
    }

    public static void deleteCache(ExecutionStep[] steps, String cacheFolderPath) {
        File cacheFolder = new File(cacheFolderPath);
        File[] files = cacheFolder.listFiles();
        if (files == null) {
            throw new IllegalArgumentException("Could not list files in folder " + cacheFolder);
        }
        for (File file : files) {
            Executor.deleteCache(file.getName()+".laz", cacheFolderPath, steps);
        }
    }

    private record MinCoordinates(int x, int y) {}

    public static class LasFileFormatException extends Exception{
        public LasFileFormatException(String message) {
            super(message);
        }
    }
    private static MinCoordinates findMinimumCoordinates(File... lasFiles) throws LasFileFormatException {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;

        for (File file : lasFiles) {
            String[] name = file.getName().substring(0, file.getName().lastIndexOf('.')).split("_");
            try {
                int x = Integer.parseInt(name[0]);
                int y = Integer.parseInt(name[1]);
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                throw new LasFileFormatException("File name not in correct format: " + file.getName());
            }
        }
        return new MinCoordinates(minX, minY);
    }

    public static void runAll(
            Settings settings,
            ExecutionStep startStep,
            ExecutionStep endStep,
            ExecutionStep[] skippedSteps,
            ExecutionStep[] cachedSteps,
            String[] lasFiles,
            String lazFileFolder,
            String cacheFolderPath,
            String aerialImagePath,
            String landUsePath,
            String roadsPath,
            String waterwaysPath,
            String schematicsFolder,
            String templateMinecraftWorldPath,
            String cityGmlDownloadPath,
            String texturePackPath,
            String absolutePathToResultingMinecraftWorld,
            boolean copyToMinecraft,
            boolean overwrite,
            boolean deleteOldCache) throws IOException, LasFileFormatException {
        File lasFolder = new File(lazFileFolder);
        File[] files;

        if (lasFiles == null) {
            files = lasFolder.listFiles();
            if (files == null) {
                throw new IOException("Can't open las file folder!");
            }
        } else {
            LOGGER.log(STEP, "Running " + lasFiles.length + " las files");
            files = Arrays.stream(lasFiles).map((l) -> new File(lazFileFolder+"/"+ l)).toArray(File[]::new);
        }

        int threadCount = settings.getThreadCount();
        if (threadCount <= 0) {
            LOGGER.warn("Negative thread count: " + threadCount + " changing to 1");
            threadCount = 1;
        }
        if (threadCount > 10) {
            LOGGER.warn("More than ten threads: " + threadCount +" changing to 10" );
            threadCount = 10;
        }
        LOGGER.log(STEP, "Running " + threadCount + " threads");

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        ArrayList<Future<?>> futures = new ArrayList<>();
        MinecraftWorldWriter writer = null;
        if (endStep.number > ExecutionStep.SCHEMATIC.number) {
            MinCoordinates minCoordinates = findMinimumCoordinates(files);
            writer = new MinecraftWorldWriter(absolutePathToResultingMinecraftWorld, minCoordinates.x(), minCoordinates.y(), files.length);
            writer.copyTemplateWorld(templateMinecraftWorldPath);
            writer.start();
        }
        boolean multiThreadBuildings = files.length == 1;
        final var finalWriter = writer;
        AtomicInteger totalDone = new AtomicInteger();

        for (File file : files) {
            String lazFile = file.getAbsolutePath();
            Future<?> future = executor.submit((Callable<Void>) () -> {
                try {
                    WorldBuilder builder = new WorldBuilder(
                            settings, finalWriter, cacheFolderPath,aerialImagePath,
                            landUsePath,roadsPath,waterwaysPath,
                            cityGmlDownloadPath,texturePackPath, startStep, endStep, skippedSteps,cachedSteps, multiThreadBuildings,overwrite,deleteOldCache);
                    builder.run(lazFile, schematicsFolder);

                    totalDone.getAndIncrement();
                    LOGGER.log(STEP, "Done " + totalDone.get() + "/" + files.length);
                } catch (Exception e) {
                    e.printStackTrace();
                    LOGGER.error("Error in las file " + lazFile);
                    LOGGER.error(e);
                }
                return null;
            });
            futures.add(future);
        }
        executor.shutdown();
        try {
            int hours = 30;
            boolean success = executor.awaitTermination(hours, TimeUnit.HOURS);
            if (!success) {
                LOGGER.error("TIMEOUT! The program did not finish in " + hours + " hours");
            }
            for (Future<?> future : futures) {
                if (future.isDone()) {
                    try {
                        future.get();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                        LOGGER.error(e);
                    }
                }
            }
            if (writer != null)
                writer.join();

        } catch (InterruptedException e) {
            LOGGER.error("Thread interrupted error!");
            LOGGER.error(e);
        }

        if (copyToMinecraft && writer != null) {
            writer.copyWorldToMinecraftWorldsFolder();
        }
    }

    private String getSchematicFileName(Blocks data, String schematicsFolder) {
        return schematicsFolder + "/"
                + data.getMinX() + "_" + data.getMinY() + "_" + data.getMinZ()
                + "_" + data.getWidth() + "_" + data.getLength() + "_" + data.getHeight() + ".schematic";
    }
    private void run(String lazFile,
                     String schematicsFolder) throws Exception {
        Executor executor = new Executor(lazFile, this.startStep, this.endStep,
                this.skippedSteps, this.cacheFolderPath,
                this.serializer, this.cachedSteps, this.overwrite, this.deleteOldCache);

        LOGGER.info("World builder starting...");
        executor.exec(() -> LASEndPoint.convertLazDataToBlocks(lazFile), ExecutionStep.READ_LAS);

        executor.exec(LASEndPoint::fixProblemsWithLASData, ExecutionStep.FIX_LAS );

        executor.exec(groundColorEndpoint::colorGround, ExecutionStep.AERIAL_IMAGES);

        executor.exec(osmEndPoint::addOsmFeatures, ExecutionStep.OSM);

        executor.exec(cityGmlEndpoint::applyBuildings, ExecutionStep.GML);

        executor.exec(decorator::decorate, ExecutionStep.DECORATE);

        executor.exec((blocks) -> {
            File schematicsFolderFile = new File(schematicsFolder);
            if (!schematicsFolderFile.exists()) schematicsFolderFile.mkdirs();

            Blocks.BlockData data = blocks.getBlockData();
            schematicCreator.writeSchematic(getSchematicFileName(blocks, schematicsFolder),
                    data.blockIds(), data.blockData(), data.width(), data.length(), data.height());

        }, ExecutionStep.SCHEMATIC, false);

        executor.exec(blocks -> {
            worldWriter.writeSchematicToWorld(getSchematicFileName(blocks, schematicsFolder));
        }, ExecutionStep.MINECRAFT_WORLD, false);

        LOGGER.info("World builder exiting...");
    }
}
