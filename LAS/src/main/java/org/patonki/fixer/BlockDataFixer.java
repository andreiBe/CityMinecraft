package org.patonki.fixer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.patonki.blocks.Blocks;
import org.patonki.blocks.XYZBlock;
import org.patonki.data.Block;
import org.patonki.data.Classification;

import java.util.*;

public class BlockDataFixer {
    private Blocks blocks;
    private static final Logger LOGGER = LogManager.getLogger(BlockDataFixer.class);

    private final Block buildingBlock;
    private final Block waterBlock;
    private final Block seaBottomBlock;
    private final Block unknownBlock;

    private final Block roofBlock;
    public BlockDataFixer(Block buildingBlock, Block waterBlock, Block seaBottomBlock, Block unknownBlock, Block roofBlock) {
        this.buildingBlock = buildingBlock;
        this.waterBlock = waterBlock;
        this.seaBottomBlock = seaBottomBlock;
        this.unknownBlock = unknownBlock;
        this.roofBlock = roofBlock;
    }

    public void improveData(Blocks blocks) {
        this.blocks = blocks;
        LOGGER.info("Improving the data");
        removeFloatingBuildings();
        removeFloatingPlantsAndUnknownBlocks();
        extendBuildings();
        removePlantsAndUnknownBlocksNearBuildings();
        mergeUnknownBlocksToPlants();
        randomFixes();
        floodWater();
        fillInGround();
        fillBottom();
    }


    private boolean hasBlocksOfAirBelow(int x, int y, int z, int amount, Classification[] ignored, boolean falseWhenEndlessAir) {
        int res = 0;
        for (z -= 1; z >= 0; z--) {
            Block b = this.blocks.get(x, y, z);
            if (!(b == null || Arrays.stream(ignored).anyMatch(c -> c == b.classification()))) {
                return res >= amount;
            }
            res++;
        }

        return !falseWhenEndlessAir && res >= amount;
    }
    //the data may have some random floating building blocks
    private void removeFloatingBuildings() {
        LOGGER.debug("Removing floating building blocks");
        int amount = 0;
        for (XYZBlock block : this.blocks) {
            Classification classification = block.block().classification();
            if (classification != Classification.BUILDING) continue;
            if (
                    //The building block is floating with no other building blocks connected to it
                    this.blocks.numberOfNeighboringBlocksWithClassification(block,classification) <= 1
                    //The block has very few other building blocks nearby
                    || blocks.numberOfBlocksWithClassificationInRadius(block,classification,3) < 6
                    //floating high in the air
                    || this.hasBlocksOfAirBelow(block.x(), block.y(), block.z(), 20,
                            new Classification[]{Classification.LOW_VEGETATION, Classification.MEDIUM_VEGETATION,
                                    Classification.HIGH_VEGETATION, Classification.UNKNOWN}, true)
            ) {
                amount++;
                this.blocks.remove(block);
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
            //if the plant is too far in the skye it should be marked as unknown because it probably isn't a plant
            if (cf.isPlant() && hasBlocksOfAirBelow(XYZBlock.x(), XYZBlock.y(), XYZBlock.z(), 30,
                    new Classification[]{Classification.UNKNOWN, Classification.BUILDING}, false)) {
                this.blocks.set(XYZBlock, this.unknownBlock);
            }
            //if a plant has less than three plants next to it, it is removed
            else if ((cf.isPlant() && this.blocks.numberOfNeighboringBlocksWithClassification(XYZBlock, cf) < 3)
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
    //Therefore, we just add blocks below every building block to extend them to the ground
    //This does generate some problems, for example, with building that have overhangs or
    //roads going under them.
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
    private record Coordinate(int x, int y, int z) {
        public Coordinate(XYZBlock block) {
            this(block.x(), block.y(), block.z());
        }
    }
    //Some trees have blocks classified as unknown on top of them for some reason
    //this fixes that
    private void mergeUnknownBlocksToPlants() {
        LOGGER.debug("Merging unknown blocks to plants");
        int amount = 0;


        Queue<Coordinate> queue = new ArrayDeque<>();
        for (XYZBlock XYZBlock : this.blocks) {
            Classification cf = XYZBlock.block().classification();
            if (cf != Classification.UNKNOWN) continue;
            queue.add(new Coordinate(XYZBlock));
        }

        HashMap<Coordinate, Integer> alreadyChanged = new HashMap<>();
        while (!queue.isEmpty()) {
            Coordinate coordinate = queue.poll();

            List<XYZBlock> neighbors = this.blocks.neighborsList(coordinate.x, coordinate.y, coordinate.z);
            for (XYZBlock neighbor : neighbors) {
                if (!neighbor.block().classification().isPlant()) continue;

                int iterationNum = alreadyChanged.getOrDefault(new Coordinate(neighbor.x(), neighbor.y(), neighbor.z()), 0);
                if (iterationNum > 3) continue;
                this.blocks.set(coordinate.x, coordinate.y, coordinate.z, neighbor.block());
                alreadyChanged.put(coordinate, iterationNum+1);
                for (XYZBlock b : neighbors) {
                    if (b.block().classification() == Classification.UNKNOWN) {
                        queue.add(new Coordinate(b));
                    }
                }

                amount++;
                break;
            }
        }
        LOGGER.debug("Changed " + amount + " blocks");
    }
    private void randomFixes() {
        ArrayList<Coordinate> roofBlocks = new ArrayList<>();
        for (XYZBlock block : blocks) {
            if ((block.block().classification() == Classification.UNKNOWN || block.block().classification().isPlant())
                    && blocks.numberOfNeighboringBlocksWithClassification(block, block.block().classification()) == 0) {
                blocks.remove(block);
            }
            if (block.block().classification() == Classification.BUILDING
            && blocks.numberOfNeighboringBlocksWithClassification(block, block.block().classification()) <= 1) {
                blocks.remove(block);
            }
            else if (block.block().classification() == Classification.BUILDING) {
                Block upper = blocks.get(block.x(), block.y(), block.z() + 1);
                if (upper == null || upper.classification() != Classification.BUILDING) {
                    blocks.set(block, this.roofBlock);
                    roofBlocks.add(new Coordinate(block));
                }
            }
        }
        for (Coordinate roof : roofBlocks) {
            List<XYZBlock> neighbors = blocks.neighborsList(roof.x, roof.y, roof.z);
            boolean hasRoofNeighbor = neighbors.stream().anyMatch(n -> n.block().equals(this.roofBlock));
            if (!hasRoofNeighbor) blocks.set(roof.x, roof.y, roof.z, this.buildingBlock);
        }
    }
    private void addIfNull(List<XYZBlock> list, int x, int y, int z, XYZBlock[][] ground) {
        if (x < 0 || y < 0 || x >= ground.length || y >= ground[0].length) {
            return;
        }
        if (ground[x][y] == null) {
            list.add(new XYZBlock(x,y,z, null));
        }
    }
    private List<XYZBlock> nullNeighbors(int x, int y, int z, XYZBlock[][] ground) {
        ArrayList<XYZBlock> ret = new ArrayList<>(4);
        addIfNull(ret, x-1, y,z, ground);
        addIfNull(ret, x+1, y,z, ground);
        addIfNull(ret, x, y-1,z, ground);
        addIfNull(ret, x, y+1,z, ground);
        return ret;
    }
    //Making sure that every (x,y) column has at least one ground block.
    //This fills any holes in the landscape
    private void fillInGround() {
        LOGGER.debug("Filling in the ground");
        XYZBlock[][] ground = blocks.getGroundLayerIncomplete();
        PriorityQueue<XYZBlock> edges = new PriorityQueue<>(Comparator.comparingInt(XYZBlock::z));
        for (int x = 0; x < blocks.getWidth(); x++) {
            for (int y = 0; y < blocks.getLength(); y++) {
                List<XYZBlock> nullNeighbors = nullNeighbors(x, y, 0, ground);
                if (!nullNeighbors.isEmpty() && ground[x][y] != null) edges.add(ground[x][y]);
            }
        }
        while (!edges.isEmpty()) {
            XYZBlock edge = edges.poll();
            List<XYZBlock> nullNeighbors = nullNeighbors(edge.x(), edge.y(), edge.z(), ground);
            for (XYZBlock nullNeighbor : nullNeighbors) {
                XYZBlock newXYZBlock = new XYZBlock(nullNeighbor.x(), nullNeighbor.y(), nullNeighbor.z(), edge.block());
                ground[nullNeighbor.x()][nullNeighbor.y()] = newXYZBlock;
                blocks.set(newXYZBlock, edge.block());
                edges.add(newXYZBlock);
            }
        }
        //throws error if the ground still contains holes
        blocks.getGroundLayer();
    }

    private void fillBottom() {
        LOGGER.debug("Filling the bottom blocks");
        int amount = 0;
        for (int x = 0; x < blocks.getWidth(); x++) {
            for (int y = 0; y < blocks.getLength(); y++) {
                for (int z = 0; z < blocks.getHeight(); z++) {
                    //finding the lowest ground block
                    Block block = this.blocks.get(x, y, z);
                    if (block == null || block.classification() != Classification.GROUND) continue;
                    //filling down to the bottom
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


    private static class ClassifiedCoordinate {
        int x, y, z;
        Classification classification;

        public ClassifiedCoordinate(int x, int y, int z, Classification classification) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.classification = classification;
        }

        public ClassifiedCoordinate(XYZBlock XYZBlock) {
            this.x = XYZBlock.x();
            this.y = XYZBlock.y();
            this.z = XYZBlock.z();
            this.classification = XYZBlock.block().classification();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClassifiedCoordinate that = (ClassifiedCoordinate) o;
            return x == that.x && y == that.y && z == that.z && classification == that.classification;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z, classification);
        }
    }

    private void addIfValid(int x, int y, int z, Queue<ClassifiedCoordinate> q) {
        if (!blocks.inRange(x,y,z)) return;
        Block block = blocks.get(x, y, z);
        if (block != null && block.classification() == Classification.WATER) return;
        if (block != null) q.add(new ClassifiedCoordinate(x, y, z, block.classification()));
        else q.add(new ClassifiedCoordinate(x, y, z, null));
    }

    //the lidar laser is bad at detecting water so oceans may have holes in them
    //Flood fill algorithm: https://en.wikipedia.org/wiki/Flood_fill
    private void floodWater() {
        LOGGER.debug("Starting to fill water");
        int amount = 0;
        HashSet<ClassifiedCoordinate> visited = new HashSet<>();
        Queue<ClassifiedCoordinate> q = new LinkedList<>();
        XYZBlock[][] ground = blocks.getGroundLayerIncomplete();

        for (XYZBlock block : blocks) {
            if (block.block().classification() != Classification.WATER) continue;
            q.add(new ClassifiedCoordinate(block));
            while (!q.isEmpty()) {
                ClassifiedCoordinate n = q.poll();
                if (visited.contains(n)) continue;
                visited.add(n);
                if (n.classification != null && !n.classification.isPlant()) continue;
                if (ground[n.x][n.y] != null) continue;
                Block below = blocks.get(n.x, n.y, n.z - 1);
                if (below != null && below.classification() == Classification.WATER) continue;

                blocks.set(n.x, n.y, n.z, this.waterBlock);
                if (n.z == 0) {
                    blocks.set(n.x, n.y, 0, this.waterBlock);
                } else {
                    blocks.set(n.x, n.y, 0, this.seaBottomBlock);
                }
                amount++;
                addIfValid(n.x - 1, n.y, n.z, q);
                addIfValid(n.x + 1, n.y, n.z, q);
                addIfValid(n.x, n.y - 1, n.z, q);
                addIfValid(n.x, n.y + 1, n.z, q);
            }

        }
        LOGGER.debug("Changed " + amount + " blocks to water");
    }


}
