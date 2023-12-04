package org.patonki.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.patonki.blocks.Blocks;
import org.patonki.data.BlockSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Supplier;

public class Executor {
    private final String lazFile;
    private final BlockSerializer serializer;
    private final ExecutionStep[] cached;
    private Blocks blocks;
    private final ExecutionStep startStep;
    private final ExecutionStep endStep;
    private final ExecutionStep[] skipped;
    private final String cacheFolderPath;

    private final boolean startCacheExists;
    private static final Logger LOGGER = LogManager.getLogger(Executor.class);

    public Executor(String lazFile, ExecutionStep start, ExecutionStep end, ExecutionStep[] skipped, String cacheFolderPath, BlockSerializer serializer, ExecutionStep[] cached) {
        this.lazFile = lazFile;
        this.startStep = start;
        this.endStep = end;
        this.skipped = skipped;
        this.cached = cached;
        this.cacheFolderPath = cacheFolderPath;
        this.serializer = serializer;
        this.startCacheExists = getCacheFile(lazFile, startStep).exists();
    }
    private File getCacheFile(String lazFile, ExecutionStep step) {
        String withoutExtension = lazFile.substring(0, lazFile.lastIndexOf('.'));
        new File(this.cacheFolderPath+"/"+withoutExtension).mkdirs();
        return new File(this.cacheFolderPath + "/"+withoutExtension+"/"+ step.name()+".dat");
    }
    private Blocks getCachedBlocks(String lazFile, ExecutionStep step) throws IOException {
        File file = getCacheFile(lazFile, step);
        try (FileInputStream in = new FileInputStream(file)) {
            return serializer.deserialize(in.readAllBytes());
        } catch (IOException e) {
            LOGGER.error("Error while trying to read cached object ");
            LOGGER.error(e);
            throw e;
        }
    }
    private void cacheBlocks(String lazFile, ExecutionStep step, Blocks blocks) throws IOException {
        File cacheFile = getCacheFile(lazFile, step);

        try (FileOutputStream out = new FileOutputStream(cacheFile)) {
            out.write(serializer.serialize(blocks));
        } catch (IOException e) {
            LOGGER.error("Error while trying to write cached object to file");
            LOGGER.error(e);
            throw e;
        }
    }
    private boolean shouldIgnore(ExecutionStep step) {
        return Arrays.stream(skipped).anyMatch(s -> s == step);
    }

    private boolean shouldCache(ExecutionStep step) {
        return Arrays.stream(cached).anyMatch(s -> s == step);
    }
    private interface Runnable {
        void run() throws Exception;
    }
    private void execute(Runnable runnable, ExecutionStep step) throws Exception {
        if (shouldIgnore(step)) {
            LOGGER.info("Ignoring step: " + step.name());
            return;
        }
        if (endStep.number < step.number) return;


        if (startStep.number > step.number && startCacheExists) {
            return;
        }
        if (startStep == step && startCacheExists) {
            this.blocks = getCachedBlocks(lazFile, startStep);
            LOGGER.info("Step retrieved from cache: " + step.name());
            return;
        }
        runnable.run();

        if (endStep == step || shouldCache(step) || startStep == step) {
            cacheBlocks(lazFile, endStep, blocks);
            LOGGER.info("Step cached " + step.name());
        }
        LOGGER.info("Step completed " + step.name());
    }
    public interface BlocksHandler {
        void handle(Blocks blocks) throws Exception;
    }
    public void exec(Supplier<Blocks> supplier, ExecutionStep step) throws Exception {
        execute(() -> {
            this.blocks = supplier.get();
        }, step);
    }


    public void exec(BlocksHandler consumer, ExecutionStep step) throws Exception {
        execute(() -> {
            consumer.handle(blocks);
        }, step);
    }
}
