package org.patonki.citygml;

import org.patonki.blocks.Blocks;
import org.patonki.blocks.GroundLayer;
import org.patonki.blocks.XYZBlock;
import org.patonki.citygml.citygml.BlockLocation;
import org.patonki.citygml.citygml.BlockLocations;
import org.patonki.data.Block;
import org.patonki.data.BoundingBox3D;
import org.patonki.data.Classification;
import org.patonki.data.IntBoundingBox;
import org.patonki.util.BoundsCalculator2D;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class BuildingReplacer implements Consumer<BlockLocations> {
    private final Blocks blocks;

    private final ReentrantLock lock = new ReentrantLock();

    private final Block buildingBlock;
    private final Block roofBlock;

    private final boolean[][] check;

    private final HashSet<Coordinate> visitedCoordinates = new HashSet<>();
    private final GroundLayer groundLayer;

    private record Coordinate(int x, int y, int z) {}

    public BuildingReplacer(Blocks blocks, Block buildingBlock, Block roofBlock) {
        this.blocks = blocks;
        this.groundLayer = blocks.getGroundLayer();

        this.buildingBlock = buildingBlock;
        this.roofBlock = roofBlock;
        this.check = new boolean[blocks.getWidth()][blocks.getLength()];
    }

    private boolean blockIsPartOfOldBuilding(Block block) {
        if (block == null) return false;
        if (block.classification() == Classification.UNKNOWN) return true;

        return block.classification() == Classification.BUILDING &&
                (block.equals(this.buildingBlock) || block.equals(this.roofBlock));
    }
    private void removeOldBuilding(int x, int y, int z) {
        Queue<Coordinate> queue = new LinkedList<>();
        queue.add(new Coordinate(x,y,z));

        while (!queue.isEmpty()) {
            Coordinate coordinate = queue.poll();
            if (visitedCoordinates.contains(coordinate)) continue;
            visitedCoordinates.add(coordinate);

            blocks.remove(coordinate.x, coordinate.y, coordinate.z);
            List<XYZBlock> neighbors = blocks.neighborsList(coordinate.x, coordinate.y, coordinate.z);
            for (XYZBlock neighbor : neighbors) {
                if (blockIsPartOfOldBuilding(neighbor.block())) {
                    queue.add(new Coordinate(neighbor.x(), neighbor.y(), neighbor.z()));
                }
            }
        }
    }
    private void acceptLocations(BlockLocations locations) {

        if (locations.getLocations().size() == 0) return;

        BoundingBox3D boundingBox = locations.getBoundingBox();
        int minX = (int) (boundingBox.x() - blocks.getMinX());
        int minY = (int) (boundingBox.y() - blocks.getMinY());
        int minZ = (int) (boundingBox.z() - blocks.getMinZ());

        this.visitedCoordinates.clear();
        for (boolean[] booleans : this.check) {
            Arrays.fill(booleans, false);
        }
        BoundsCalculator2D boundsCalc = new BoundsCalculator2D();
        int radius = 5;
        for (BlockLocation location : locations) {
            int x = location.x() + minX;
            int y = location.y() + minY;
            int z = location.z() + minZ;
            //marking the area that will be checked for the old buildings
            //this is done for performance reasons
            for (int xs = x - radius; xs <= x + radius; xs++) {
                for (int ys = y - radius; ys <= y + radius; ys++) {
                    if (xs < 0 || ys < 0 || xs >= check.length || ys >= check[0].length) {
                        continue;
                    }
                    boundsCalc.add(xs,ys);
                    check[xs][ys] = true;
                }
            }
            Block b = new Block(location.id(), location.data(), Classification.BUILDING);
            Block previousBlock = blocks.get(x,y,z);
            //do not replace ground blocks
            if (groundLayer.inRange(x,y) && groundLayer.getXYZBlock(x,y).z() != z
                    && (previousBlock == null || previousBlock.classification() != Classification.GROUND)) {
                blocks.set(x,y,z, b);
            }
        }
        IntBoundingBox bBox = boundsCalc.get();
        for (int x = bBox.minX(); x <= bBox.maxX(); x++) {
            for (int y = bBox.minY(); y < bBox.maxY(); y++) {
                if (!check[x][y]) continue;
                for (int z = 0; z < blocks.getHeight(); z++) {
                    Block b = blocks.get(x,y,z);
                    if (blockIsPartOfOldBuilding(b)) {
                        removeOldBuilding(x,y,z);
                    }
                }
            }
        }
    }
    @Override
    public void accept(BlockLocations locations) {
        lock.lock(); //only one thread allowed at once
        try {
            acceptLocations(locations);
        } finally {
            lock.unlock();
        }
    }
}
