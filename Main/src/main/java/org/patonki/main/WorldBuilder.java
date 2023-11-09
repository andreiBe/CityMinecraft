package org.patonki.main;

import org.patonki.blocks.ArrayBlocks;
import org.patonki.blocks.Blocks;
import org.patonki.blocks.OctTreeBlocks;
import org.patonki.citygml.endpoint.CityGmlEndpoint;
import org.patonki.converter.MinecraftWorldWriter;
import org.patonki.converter.SchematicCreator;
import org.patonki.data.BlockSerializer;
import org.patonki.data.Classification;
import org.patonki.decorator.WorldDecorator;
import org.patonki.openstreetmap.OsmEndPoint;
import org.patonki.las.LASEndPoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.patonki.settings.Settings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.*;

public class WorldBuilder {
    private final LASEndPoint LASEndPoint;
    private final ExecutionStep[] skippedSteps;
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

    private WorldBuilder(Settings settings, MinecraftWorldWriter worldWriter,
                         String cacheFolderPath, String landUsePath,
                         String roadsPath, String waterwaysPath, String cityGmlDownloadFolder, String texturesPath,
                         ExecutionStep startStep, ExecutionStep endStep, ExecutionStep[] skippedSteps, boolean multiThreaded) {
        this.LASEndPoint = new LASEndPoint(settings.getLasSettings());
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
    public static void runSingle(
            Settings settings,
            ExecutionStep startStep,
            ExecutionStep endStep,
            ExecutionStep[] skippedSteps,
            String lazFile,
            String cacheFolderPath,
            String schematicsFolder,
            String templateMinecraftWorldPath,
            String cityGmlDownloadPath,
            String landUsePath,
            String roadsPath,
            String waterwaysPath,
            String texturePackPath,
            String absolutePathToResultingMinecraftWorld, boolean copyToMinecraftFolder) throws Exception {

        MinecraftWorldWriter writer = null;

        if (endStep.number > ExecutionStep.SCHEMATIC.number) {
            MinCoordinates minCoordinates = findMinimumCoordinates(new File(lazFile));
            writer = new MinecraftWorldWriter(absolutePathToResultingMinecraftWorld, minCoordinates.x(), minCoordinates.y());
            writer.copyTemplateWorld(templateMinecraftWorldPath);
        }

        WorldBuilder builder = new WorldBuilder(settings, writer, cacheFolderPath,landUsePath,
                roadsPath, waterwaysPath, cityGmlDownloadPath,texturePackPath, startStep, endStep,skippedSteps, true);

        builder.run(lazFile, schematicsFolder);

        if (copyToMinecraftFolder && writer != null) {
            writer.copyWorldToMinecraftWorldsFolder();
        }
    }


    public static void runAll(
            Settings settings,
            ExecutionStep startStep,
            ExecutionStep endStep,
            ExecutionStep[] skippedSteps,
            String lazFileFolder,
            String cacheFolderPath,
            String landUsePath,
            String roadsPath,
            String waterwaysPath,
            String schematicsFolder,
            String templateMinecraftWorldPath,
            String cityGmlDownloadPath,
            String texturePackPath,
            String absolutePathToResultingMinecraftWorld,
            boolean copyToMinecraft) throws IOException, LasFileFormatException {
        File lasFolder = new File(lazFileFolder);
        File[] files = lasFolder.listFiles();
        if (files == null) {
            throw new IOException("Can't open las file folder!");
        }
        ExecutorService executor = Executors.newFixedThreadPool(3);
        ArrayList<Future<?>> futures = new ArrayList<>();
        MinecraftWorldWriter writer = null;
        if (endStep.number > ExecutionStep.SCHEMATIC.number) {
            MinCoordinates minCoordinates = findMinimumCoordinates(files);
            writer = new MinecraftWorldWriter(absolutePathToResultingMinecraftWorld, minCoordinates.x(), minCoordinates.y());
            writer.copyTemplateWorld(templateMinecraftWorldPath);
        }
        final var finalWriter = writer;
        for (File file : files) {
            String lazFile = file.getAbsolutePath();
            Future<?> future = executor.submit((Callable<Void>) () -> {
                WorldBuilder builder = new WorldBuilder(
                        settings, finalWriter, cacheFolderPath,
                        landUsePath,roadsPath,waterwaysPath,
                        cityGmlDownloadPath,texturePackPath, startStep, endStep, skippedSteps, false);
                builder.run(lazFile, schematicsFolder);
                return null;
            });
            futures.add(future);
        }
        executor.shutdown();
        try {
            int hours = 10;
            boolean success = executor.awaitTermination(hours, TimeUnit.HOURS);
            if (!success) {
                LOGGER.error("TIMEOUT! The program did not finish in " + hours + " hours");
            }
            for (Future<?> future : futures) {
                if (future.isDone()) {
                    try {
                        future.get();
                    } catch (ExecutionException e) {
                        LOGGER.error(e);
                    }
                }
            }
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
        Executor executor = new Executor(lazFile, this.startStep, this.endStep, this.skippedSteps, this.cacheFolderPath, this.serializer);

        LOGGER.info("World builder starting...");
        executor.execStart(() -> LASEndPoint.convertLazDataToBlocks(lazFile), ExecutionStep.READ_LAS);
        LOGGER.info("Las points have been calculated!");
        executor.exec(LASEndPoint::fixProblemsWithLASData, ExecutionStep.FIX_LAS);
        LOGGER.info("Problems with the LAS data have been fixed!");
        executor.exec(osmEndPoint::addOsmFeatures, ExecutionStep.OSM);
        LOGGER.info("Osm features have been added!");
        executor.exec(cityGmlEndpoint::applyBuildings, ExecutionStep.GML);
        LOGGER.info("CityGml buildings have been added");
        executor.exec(decorator::decorate, ExecutionStep.DECORATE);
        LOGGER.info("Additional decorations have been added!");

        executor.exec((blocks) -> {
            File schematicsFolderFile = new File(schematicsFolder);
            if (!schematicsFolderFile.exists()) schematicsFolderFile.mkdirs();

            Blocks.BlockData data = blocks.getBlockData();
            schematicCreator.writeSchematic(getSchematicFileName(blocks, schematicsFolder),
                    data.blockIds(), data.blockData(), data.width(), data.length(), data.height());
        }, ExecutionStep.SCHEMATIC);
        LOGGER.info("Schematic has been written!");

        executor.exec(blocks -> {
            worldWriter.writeSchematicToWorld(getSchematicFileName(blocks, schematicsFolder));
        }, ExecutionStep.MINECRAFT_WORLD);
        LOGGER.info("Schematic has been written to the minecraft world!");

        LOGGER.info("World builder exiting...");
    }
}
