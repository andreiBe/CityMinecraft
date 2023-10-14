package reader;

import blocks.Blocks;
import data.Block;
import data.Classification;

import java.util.HashMap;
import java.util.Map;

public class LasDataToBlocks {
    private final BlockSupplier supplier;

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
        Blocks blocks = maker.create(data.width(), data.length(), data.height(), data.minXCord(), data.minYCord(), data.minZCord());
        int lx=0; int ly=0; int lz=0;
        //a single block can contain multiple points with different classifications, so we need to choose one of them
        //the final classification of the block is selected based on this map
        //the more points of a specific classification, the likelier it is that that classification is chosen
        HashMap<Classification, Integer> clAmount = new HashMap<>();

        //the points will be sorted so points inside one block will be after one another
        LasReader.LazPoint[] points = data.points();
        for (int i = 0; i < points.length; i++) {
            LasReader.LazPoint point = points[i];
            //encountering a new block
            if (point.x != lx || point.y != ly || point.z != lz || i == points.length-1) {
                Block block = correctBlock(clAmount);
                if (block != null) {
                    blocks.set(lx, ly, lz, block);
                }
                clAmount.clear();
            }
            //increasing the count of a specific classification by one
            clAmount.putIfAbsent(point.classification, 0);
            clAmount.put(point.classification, clAmount.get(point.classification) + 1);
            lx = point.x;
            ly = point.y;
            lz = point.z;
        }
        return blocks;
    }

    private Block correctBlock(Map<Classification, Integer> clAmount) {
        Classification mostMatches = null; //the classification of the final block
        int sum = 0; //sum of points inside the block
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
