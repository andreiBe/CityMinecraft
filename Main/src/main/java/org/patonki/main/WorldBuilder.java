package org.patonki.main;

import blocks.ArrayBlocks;
import blocks.Blocks;
import blocks.OctTreeBlocks;
import converter.MinecraftWorldWriter;
import converter.SchematicCreator;
import data.BlockSerializer;
import decorator.WorldDecorator;
import endpoint.OsmEndPoint;
import interfaces.LASEndPoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.patonki.settings.Settings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.*;

public class WorldBuilder {
    private final LASEndPoint LASEndPoint;
    private final OsmEndPoint osmEndPoint;
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
                         String roadsPath, String waterwaysPath,
                         ExecutionStep startStep, ExecutionStep endStep) {
        this.LASEndPoint = new LASEndPoint(settings.getLasSettings());
        this.osmEndPoint = new OsmEndPoint(settings.getOsmSettings(), landUsePath, roadsPath, waterwaysPath);
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

    public static void runOne(
            Settings settings,
            ExecutionStep startStep,
            ExecutionStep endStep,
            String lazFile,
            String cacheFolderPath,
            String schematicsFolder,
            String templateMinecraftWorldPath,
            String landUsePath,
            String roadsPath,
            String waterwaysPath) throws Exception {
        MinecraftWorldWriter writer = new MinecraftWorldWriter();
        WorldBuilder builder = new WorldBuilder(settings, writer, cacheFolderPath,landUsePath,
                roadsPath, waterwaysPath, startStep, endStep);

        builder.run(lazFile, schematicsFolder, templateMinecraftWorldPath);
    }

    private String getSchematicFileName(Blocks data, String schematicsFolder) {
        return schematicsFolder + "/"
                + data.getMinX() + "_" + data.getMinY() + "_" + data.getMinZ()
                + "_" + data.getWidth() + "_" + data.getLength() + "_" + data.getHeight() + ".schematic";
    }
    private void run(String lazFile,
                     String schematicsFolder,
                     String templateMinecraftWorldPath) throws Exception {
        Executor executor = new Executor(lazFile, this.startStep, this.endStep, this.cacheFolderPath, this.serializer);

        LOGGER.info("World builder starting...");
        executor.execStart(() -> LASEndPoint.convertLazDataToBlocks(lazFile), ExecutionStep.READ_LAS);
        LOGGER.info("Las points have been calculated!");
        executor.execStart(LASEndPoint::fixProblemsWithLASData, ExecutionStep.FIX_LAS);
        LOGGER.info("Problems with the LAS data have been fixed!");
        executor.execStart(osmEndPoint::addOsmFeatures, ExecutionStep.OSM);
        LOGGER.info("Osm features have been added!");
        executor.execStart(decorator::decorate, ExecutionStep.DECORATE);
        LOGGER.info("Additional decorations have been added!");

        executor.execStart((blocks) -> {
            Blocks.BlockData data = blocks.getBlockData();
            schematicCreator.writeSchematic(getSchematicFileName(blocks, schematicsFolder),
                    data.blockIds(), data.blockData(), data.width(), data.length(), data.height());
        }, ExecutionStep.SCHEMATIC);
        LOGGER.info("Schematic has been written!");

        executor.execStart(blocks -> {
            worldWriter.writeSchematicToWorld(getSchematicFileName(blocks, schematicsFolder), templateMinecraftWorldPath);
        }, ExecutionStep.MINECRAFT_WORLD);
        LOGGER.info("Schematic has been written to the minecraft world!");

        LOGGER.info("World builder exiting...");
    }

    public static void runAll(
            Settings settings,
            ExecutionStep startStep,
            ExecutionStep endStep,
            String lazFileFolder,
            String cacheFolderPath,
            String landUsePath,
            String roadsPath,
            String waterwaysPath,
            String schematicsFolder,
            String templateMinecraftWorldPath) throws IOException {
        File lasFolder = new File(lazFileFolder);
        File[] files = lasFolder.listFiles();
        if (files == null) {
            throw new IOException("Can't open las file folder!");
        }
        ExecutorService executor = Executors.newFixedThreadPool(3);
        ArrayList<Future<?>> futures = new ArrayList<>();

        MinecraftWorldWriter writer = new MinecraftWorldWriter();
        for (File file : files) {
            String lazFile = file.getAbsolutePath();
            Future<?> future = executor.submit((Callable<Void>) () -> {
                WorldBuilder builder = new WorldBuilder(settings, writer, cacheFolderPath,landUsePath,roadsPath,waterwaysPath, startStep, endStep);
                builder.run(lazFile, schematicsFolder, templateMinecraftWorldPath);
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
    }
}
