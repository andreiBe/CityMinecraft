package org.patonki.reader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.patonki.blocks.Blocks;
import org.patonki.data.Block;
import org.patonki.data.Classification;

import java.util.HashMap;
import java.util.Map;

public class LasDataToBlocks {
    private final BlockSupplier supplier;
    private static final Logger LOGGER = LogManager.getLogger(LasDataToBlocks.class);

    public LasDataToBlocks(BlockSupplier supplier) {
        this.supplier = supplier;
    }

    public interface BlockSupplier {
        Block get(Classification classification);
    }
    public interface BlockMaker {
        Blocks create(int w, int l, int h, int x, int y, int z);
    }
    public Blocks convert(LasReader.LazData data, BlockMaker maker) {
        LOGGER.info("Starting to convert las data to minecraft blocks."
        + "MinX: " + data.minXCord() + " MinY: " + data.minYCord() + " MinZ: " + data.minZCord());

        Blocks blocks = maker.create(data.width(), data.length(), data.height(), data.minXCord(), data.minYCord(), data.minZCord());
        //A single block can contain multiple points with different classifications, so we need to choose one of them.
        //The classification of blocks is selected based on this map.
        //The more points of a specific classification, the likelier it is that the classification is chosen
        HashMap<Classification, Integer> clAmount = new HashMap<>();
        int numberOfBlocks = 0;
        int lastX=0; int lastY=0; int lastZ=0;
        //the points are sorted so points inside one block will be after one another
        LasReader.LazPoint[] points = data.points();
        for (int i = 0; i < points.length; i++) {
            LasReader.LazPoint point = points[i];
            //encountering a new block
            if (point.x != lastX || point.y != lastY || point.z != lastZ || i == points.length-1) {
                Block block = getBlockBasedOnClassifiedPoints(clAmount);
                if (block != null) {
                    blocks.set(lastX, lastY, lastZ, block);
                    numberOfBlocks++;
                }
                clAmount.clear();
            }
            //increasing the count of a classification by one
            clAmount.putIfAbsent(point.classification, 0);
            clAmount.put(point.classification, clAmount.get(point.classification) + 1);
            lastX = point.x;
            lastY = point.y;
            lastZ = point.z;
        }
        int fillPercentage = (int) (100* numberOfBlocks / (double)(blocks.getWidth()* blocks.getLength()* blocks.getHeight()));
        LOGGER.info("Las data has been converted to minecraft blocks. Block count: " + numberOfBlocks
        + " Fill percentage: " + fillPercentage +"%");
        return blocks;
    }

    private Block getBlockBasedOnClassifiedPoints(Map<Classification, Integer> clAmount) {
        Classification mostMatches = null; //the classification of the final block
        int sum = 0; //number of points inside the block
        for (Classification cl : clAmount.keySet()) {
            if (mostMatches == null) mostMatches = cl;
            int bestScore = clAmount.get(mostMatches) * Classification.importance(mostMatches);
            int score = clAmount.get(cl) * Classification.importance(cl);
            if (score >= bestScore) {
                mostMatches = cl;
            }
            sum += clAmount.get(cl);
        }
        //points like these are most likely mistakes in the data
        if (sum <= 5 && mostMatches == Classification.UNKNOWN) return null;

        return this.supplier.get(mostMatches);
    }



}
