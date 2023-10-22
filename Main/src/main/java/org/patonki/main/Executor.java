package org.patonki.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.patonki.blocks.Blocks;
import org.patonki.data.BlockSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.function.Supplier;

public class Executor {
    private final String lazFile;
    private final BlockSerializer serializer;
    private Blocks blocks;
    private final ExecutionStep startStep;
    private final ExecutionStep endStep;
    private final String cacheFolderPath;
    
    private static final Logger LOGGER = LogManager.getLogger(Executor.class);

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

    public Executor(String lazFile, ExecutionStep start, ExecutionStep end, String cacheFolderPath, BlockSerializer serializer) {
        this.lazFile = lazFile;
        this.startStep = start;
        this.endStep = end;
        this.cacheFolderPath = cacheFolderPath;
        this.serializer = serializer;
    }
    public void execStart(Supplier<Blocks> supplier, ExecutionStep step) throws IOException {
        if (startStep.number > step.number || endStep.number < step.number) {
            return;
        }
        if (startStep == step) {
            this.blocks = getCachedBlocks(lazFile, startStep);
            return;
        }
        Blocks blocks = supplier.get();
        this.blocks = blocks;
        if (endStep == ExecutionStep.READ_LAS) {
            cacheBlocks(lazFile, endStep, blocks);
        }
        this.blocks = blocks;
    }
    public interface BlocksHandler {
        void handle(Blocks blocks) throws Exception;
    }

    public void execStart(BlocksHandler consumer, ExecutionStep step) throws Exception {
        if (startStep.number > step.number || endStep.number < step.number) {
            return;
        }
        if (startStep == step) {
            this.blocks = getCachedBlocks(lazFile, startStep);
            return;
        }
        consumer.handle(blocks);
        if (endStep == ExecutionStep.READ_LAS) {
            cacheBlocks(lazFile, endStep, blocks);
        }
    }
}
