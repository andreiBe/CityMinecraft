package org.patonki.citygml;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.patonki.color.BlackAndWhiteBlocks;
import org.patonki.color.ColorToMinecraftBlock;
import org.patonki.citygml.endpoint.BlockLocations;
import org.patonki.citygml.endpoint.GmlOptions;
import org.patonki.citygml.features.Building;
import org.patonki.citygml.features.BuildingCollection;
import org.patonki.color.IColorToBlockConverter;
import org.patonki.util.ProgressLogger;

import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class GmlFeaturesInArea {
    private static final Logger LOGGER = LogManager.getLogger(GmlFeaturesInArea.class);

    public void process(BuildingCollection collection, GmlOptions options, Consumer<BlockLocations> consumer, String texturesPath, boolean multiThreaded) throws Exception {
        BuildingToCubes converter = new BuildingToCubes();

        IColorToBlockConverter colorConverter = switch (options.texturingType()) {
            case USE_MINECRAFT_TEXTURES -> new ColorToMinecraftBlock(texturesPath, options.bannedBlocks(), options.colorConvert());
            case USE_GRAY_SCALE -> new BlackAndWhiteBlocks(texturesPath, options.blackAndWhiteColorOptions());
        };

        boolean oneColor = switch (options.coloringType()) {
            case ALL_DIFFERENT -> false;
            case STRUCTURES_SAME -> true;
        };
        LOGGER.info("Parsing buildings");
        ProgressLogger progressLogger = new ProgressLogger(collection.buildings().length, LOGGER, "Buildings parser", 10);
        if (!multiThreaded) {
            for (Building building : collection.buildings()) {
                BlockLocations locations = converter.convert(building, oneColor, colorConverter);
                consumer.accept(locations);
                progressLogger.increment();
            }
            LOGGER.info("Finished parsing buildings");
            return;
        }
        ExecutorService executor = Executors.newFixedThreadPool(3);
        ArrayList<Future<Void>> futures = new ArrayList<>();
        for (Building building : collection.buildings()) {
            Future<Void> future = executor.submit(() -> {
                try {
                    BlockLocations locations = converter.convert(building, oneColor, colorConverter);
                    consumer.accept(locations);
                } finally {
                    progressLogger.increment();
                }
                return null;
            });
            futures.add(future);
        }
        executor.shutdown();

        boolean elapsed = executor.awaitTermination(1, TimeUnit.DAYS);

        if (!elapsed) {
            LOGGER.error("The process took a day and never finished. There might be a problem...");
        }
        for (Future<?> future : futures) {
            if (future.isDone()) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    e.getCause().printStackTrace();
                }
            }
        }
        LOGGER.info("Finished parsing buildings");
    }
}