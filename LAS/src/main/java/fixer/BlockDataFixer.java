package fixer;

import blocks.Blocks;
import blocks.XYZBlock;
import data.Block;
import data.Classification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class BlockDataFixer {
    private Blocks blocks;
    private static final Logger LOGGER = LogManager.getLogger(BlockDataFixer.class);

    private final Block buildingBlock;
    private final Block waterBlock;

    public BlockDataFixer(Block buildingBlock, Block waterBlock) {
        this.buildingBlock = buildingBlock;
        this.waterBlock = waterBlock;
    }

    public void improveData(Blocks blocks) {
        this.blocks = blocks;
        LOGGER.info("Improving the data");
        removeFloatingBuildings();
        removeFloatingPlantsAndUnknownBlocks();
        extendBuildings();
        removePlantsAndUnknownBlocksNearBuildings();
        mergeUnknownBlocksToPlants();
        floodWater();
        fillInGround();
        fillBottom();
    }

    private boolean hasBlocksOfAirBelow(int x, int y, int z, int amount) {
        int res = 0;
        for (z -= 1; z >= 0; z--) {
            if (!this.blocks.isAir(x, y, z)) {
                return res >= amount;
            }
            res++;
        }
        return false;
    }

    private void removeFloatingBuildings() {
        LOGGER.debug("Removing floating building blocks");
        int amount = 0;
        for (XYZBlock XYZBlock : this.blocks) {
            Classification classification = XYZBlock.block().classification();
            if (classification != Classification.BUILDING) continue;
            if (
                    //The building block is floating with no other building blocks connected to it
                    this.blocks.numberOfNeighboringBlocksWithClassification(XYZBlock,classification) <= 1
                    //The block has very few other building blocks nearby
                    || blocks.numberOfBlocksWithClassificationInRadius(XYZBlock,classification,3) < 6
                    || this.hasBlocksOfAirBelow(XYZBlock.x(), XYZBlock.y(), XYZBlock.z(), 20)
            ) {
                amount++;
                this.blocks.remove(XYZBlock);
            }

        }
        LOGGER.debug("Removed " + amount + " blocks");
    }

    private void removeFloatingPlantsAndUnknownBlocks() {
        LOGGER.debug("Removing floating plants and unknown blocks");
        int amount = 0;
        //Removing plants and unknown blocks that have two or less plant neighbours
        for (XYZBlock XYZBlock : this.blocks) {
            Classification cf = XYZBlock.block().classification();
            //if a plant has less than three plants next to it, it is removed
            if ((cf.isPlant() && this.blocks.numberOfNeighboringBlocksWithClassification(XYZBlock, cf) < 3)
                    //if a block classified as unknown has less than two other unknown blocks next to it
                    //then it is removed
                    || (cf == Classification.UNKNOWN && this.blocks.numberOfNeighboringBlocksWithClassification(XYZBlock, cf) <= 1)) {
                this.blocks.remove(XYZBlock);
                amount++;
            }
        }
        LOGGER.debug("Removed: " + amount + " blocks");
    }
    //The laz data does not have enough points on the side of buildings to fill them up
    //,so we just add blocks below every building block
    private void extendBuildings() {
        LOGGER.debug("Extending buildings to the ground");
        int amount = 0;
        for (XYZBlock XYZBlock : this.blocks) {
            Classification cf = XYZBlock.block().classification();
            if (cf == Classification.BUILDING
                    && this.blocks.numberOfNeighboringBlocksWithClassification(XYZBlock, cf) >= 2) {
                for (int z = XYZBlock.z(); z >= 0; z--) {
                    this.blocks.set(XYZBlock.x(), XYZBlock.y(), z, XYZBlock.block());
                    amount++;
                }
            }
        }
        LOGGER.debug("Extended buildings for " + amount + " blocks");
    }

    private void removePlantsAndUnknownBlocksNearBuildings() {
        int amount = 0;
        LOGGER.debug("Removing plants and unknown blocks near buildings");
        for (XYZBlock XYZBlock : this.blocks) {
            Classification cf = XYZBlock.block().classification();
            if (!cf.isPlant() && cf != Classification.UNKNOWN) continue;
            //comparing the number of building blocks nearby with the number of blocks that
            //have the same classification as the block in question (plant or unknown)

            int similarBlocksNearby = this.blocks.numberOfBlocksWithClassificationInRadius(XYZBlock,cf, 4);
            int buildingNeighbors = this.blocks.numberOfBlocksWithClassificationInRadius(XYZBlock, Classification.BUILDING, 2);
            if (similarBlocksNearby > buildingNeighbors) continue;
            if (buildingNeighbors >= 7) {
                blocks.set(XYZBlock, this.buildingBlock);
                amount++;
            } else if (buildingNeighbors >= 3) {
                blocks.remove(XYZBlock);
                amount++;
            }
        }
        LOGGER.debug("Changed " + amount + " blocks");
    }
    //Some trees have blocks classified as unknown on top of them for some reason
    //this fixes that
    private void mergeUnknownBlocksToPlants() {
        LOGGER.debug("Merging unknown blocks to plants");
        int amount = 0;
        for (XYZBlock XYZBlock : this.blocks) {
            Classification cf = XYZBlock.block().classification();
            if (cf != Classification.UNKNOWN) continue;
            if (this.blocks.numberOfBlocksWithClassificationInRadius(XYZBlock, cf, 3) > 10) continue;
            List<XYZBlock> neighbors = this.blocks.neighborsList(XYZBlock);
            if (neighbors.isEmpty()) continue;
            for (XYZBlock neighbor : neighbors) {
                if (neighbor.block().classification().isPlant()) {
                    this.blocks.set(XYZBlock, neighbor.block());
                    amount++;
                    break;
                }
            }
        }
        LOGGER.debug("Changed " + amount + " blocks");
    }
    private void addIfNull(List<XYZBlock> list, int x, int y, XYZBlock[][] ground) {
        if (x < 0 || y < 0 || x >= ground.length || y >= ground[0].length) {
            return;
        }
        if (ground[x][y].block() != null) return;
        list.add(ground[x][y]);
    }
    private List<XYZBlock> nullNeighbors(int x, int y, XYZBlock[][] ground) {
        ArrayList<XYZBlock> ret = new ArrayList<>(4);
        addIfNull(ret, x-1, y, ground);
        addIfNull(ret, x+1, y, ground);
        addIfNull(ret, x, y-1, ground);
        addIfNull(ret, x, y+1, ground);
        return ret;
    }
    private void fillInGround() {
        XYZBlock[][] ground = blocks.getGroundLayerIncomplete();
        PriorityQueue<XYZBlock> edges = new PriorityQueue<>(Comparator.comparingInt(XYZBlock::z));
        for (int x = 0; x < blocks.getWidth(); x++) {
            for (int y = 0; y < blocks.getLength(); y++) {
                List<XYZBlock> nullNeighbors = nullNeighbors(x, y, ground);
                if (!nullNeighbors.isEmpty()) edges.add(ground[x][y]);
            }
        }
        while (!edges.isEmpty()) {
            XYZBlock edge = edges.poll();
            List<XYZBlock> nullNeighbors = nullNeighbors(edge.x(), edge.y(), ground);
            for (XYZBlock nullNeighbor : nullNeighbors) {
                XYZBlock newXYZBlock = new XYZBlock(nullNeighbor.x(), nullNeighbor.y(), nullNeighbor.z(), edge.block());
                ground[nullNeighbor.x()][nullNeighbor.y()] = newXYZBlock;
                blocks.set(newXYZBlock, edge.block());
                edges.add(newXYZBlock);
            }
        }
    }

    private void fillBottom() {
        LOGGER.debug("Filling the bottom blocks");
        int amount = 0;
        for (int x = 0; x < blocks.getWidth(); x++) {
            for (int y = 0; y < blocks.getLength(); y++) {
                for (int z = 0; z < blocks.getHeight(); z++) {
                    if (this.blocks.isAir(x, y, z)) continue;
                    Block block = this.blocks.get(x, y, z);
                    for (; z >= 0; z--) {
                        blocks.set(x, y, z, block);
                        amount++;
                    }
                    break;
                }
            }
        }
        LOGGER.debug("Filled " + amount + " blocks");
    }


    private static class Coordinate {
        int x, y, z;
        Classification classification;

        public Coordinate(int x, int y, int z, Classification classification) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.classification = classification;
        }

        public Coordinate(XYZBlock XYZBlock) {
            this.x = XYZBlock.x();
            this.y = XYZBlock.y();
            this.z = XYZBlock.z();
            this.classification = XYZBlock.block().classification();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Coordinate that = (Coordinate) o;
            return x == that.x && y == that.y && z == that.z && classification == that.classification;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z, classification);
        }
    }

    private void add(int x, int y, int z, Queue<Coordinate> q) {
        if (x < 0 || y < 0 || z < 0 || x >= blocks.getWidth() || y >= blocks.getLength() || z >= blocks.getHeight())
            return;
        Block block = blocks.get(x, y, z);
        if (block != null && block.classification() == Classification.WATER) return;
        if (block != null) q.add(new Coordinate(x, y, z, block.classification()));
        else q.add(new Coordinate(x, y, z, null));
    }

    //Flood fill algorithm: https://en.wikipedia.org/wiki/Flood_fill
    private void floodWater() {
        Block waterBlock = this.waterBlock;

        LOGGER.debug("Starting to fill water");
        int amount = 0;
        HashSet<Coordinate> visited = new HashSet<>();
        Queue<Coordinate> q = new ArrayDeque<>();
        for (XYZBlock block : blocks) {
            if (block.block().classification() != Classification.WATER) continue;
//            Block blockBelow = blocks.get(block.x, block.y, block.z-1);
//            if (blockBelow != null && blockBelow.getClassification() == Classification.WATER) return;
            q.add(new Coordinate(block));
            while (!q.isEmpty()) {
                Coordinate n = q.poll();
                if (!visited.contains(n) && (n.classification == null || n.classification.isPlant() || n.classification == Classification.WATER)) {
                    blocks.set(n.x, n.y, n.z, waterBlock);
                    amount++;
                    visited.add(n);
                    add(n.x - 1, n.y, n.z, q);
                    add(n.x + 1, n.y, n.z, q);
                    add(n.x, n.y - 1, n.z, q);
                    add(n.x, n.y + 1, n.z, q);
                }
            }

        }
        LOGGER.debug("Changed " + amount + " blocks to water");
    }


}
