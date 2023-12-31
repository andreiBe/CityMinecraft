package org.patonki.decorator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.patonki.blocks.Blocks;
import org.patonki.blocks.GroundLayer;
import org.patonki.blocks.XYZBlock;
import org.patonki.data.Block;
import org.patonki.data.Classification;

import java.util.*;

/**
 * Adds tree trunks and grass cover to grass blocks
 */
public class WorldDecorator {
    private static final Logger LOGGER = LogManager.getLogger(WorldDecorator.class);

    private static final int MIN_TREE_HEIGHT = 6;
    /**
     * Adds finishing touches to the blocks
     * @param blocks blocks
     */
    public void decorate(Blocks blocks) {
        LOGGER.debug("Starting decorating");
        Block logBlock = new Block((byte) 17, (byte) 0, Classification.MEDIUM_VEGETATION);
        treeTrunks(blocks, logBlock);
        addGrassCover(blocks);
        removeStackedGrassBlock(blocks);
        LOGGER.debug("Decoration finished");
    }

    private void removeStackedGrassBlock(Blocks blocks) {
        Block dirt = new Block(3,0,Classification.GROUND);
        for (XYZBlock block : blocks) {
            Block above = blocks.get(block.x(), block.y(), block.z()+1);
            if (block.block().id() == 2 && (above != null && above.id() == 2)) {
                blocks.set(block, dirt);
            }
        }
    }

    private static class TreePoint {
        private final int x,y,z;
        private boolean partOfTree;

        public TreePoint(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public double XYDistance(TreePoint tree) {
            return Math.sqrt((x-tree.x) * (x-tree.x) + (y - tree.y) * (y - tree.y));
        }
    }

    private void treeTrunks(Blocks blocks, Block logBlock) {
        GroundLayer groundLayer = blocks.getGroundLayer();
        ArrayList<TreePoint> treeTops = treeTops(groundLayer, blocks);
        for (TreePoint treeTop : treeTops) {
            int x = treeTop.x;
            int y = treeTop.y;
            XYZBlock groundBlock = groundLayer.getXYZBlock(x,y);
            if (groundBlock.block().classification() == Classification.WATER) continue;
            int startZ = groundBlock.z()+1;
            int endZ = treeTop.z - 1;
            boolean insideTree = false;

            for (int z = startZ; z <= endZ; z++) {
                long plantNeighborCount = blocks.xyNeighborsList(x, y, z).stream().filter(i -> i.block().classification().isPlant()).count();
                //the trunk of the tree must end before the top of the tree while it's surrounded by leaves
                if (insideTree && plantNeighborCount < 3) {
                    break;
                }
                blocks.set(x,y,z, logBlock);
                if (plantNeighborCount >= 3) insideTree = true;
            }
        }
    }
    private void addIfValid(Queue<TreePoint> rec, TreePoint p, int x, int y, TreePoint[][] treePointTop, int[][] visited, int maxDown) {
        if (x < 0 || y < 0 || x >= treePointTop.length || y>= treePointTop[0].length)
            return;
        TreePoint p2 = treePointTop[x][y];
        if (p2 == null) return;
        if (visited[x][y] == Integer.MAX_VALUE) return;
        if (p2.z > p.z) return;
        if (p.z - p2.z > maxDown) return;
        rec.add(p2);
    }
    private record TreeExtend(int minX, int minY, int maxX, int maxY, List<TreePoint> list) {
        public TreePoint mostCenterPoint() {
            int centerX = minX + (maxX - minX) / 2;
            int centerY = minY + (maxY - minY) / 2;
            TreePoint center = new TreePoint(centerX, centerY, 0);

            return list.stream().min((o1, o2) -> (int) (o1.XYDistance(center) - o2.XYDistance(center))).orElse(null);
        }
    }
    private TreeExtend extentOfTree(TreePoint centerOfTree, int[][] visited, int i, TreePoint[][] treePointTop, int maxDown) {
        ArrayList<TreePoint> ret = new ArrayList<>();
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        PriorityQueue<TreePoint> rec = new PriorityQueue<>((o1, o2) -> o2.z - o1.z);
        rec.add(centerOfTree);
        while (!rec.isEmpty()) {
            TreePoint point = rec.poll();
            if (point == null) continue;
            int x = point.x;
            int y = point.y;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);

            if (visited[x][y] == i) continue;
            visited[x][y] = i;
            ret.add(point);
            addIfValid(rec, point, x-1, y, treePointTop, visited, maxDown);
            addIfValid(rec, point, x+1, y, treePointTop,  visited, maxDown);
            addIfValid(rec, point, x, y-1, treePointTop,  visited, maxDown);
            addIfValid(rec, point, x, y+1, treePointTop,  visited, maxDown);
        }
        return new TreeExtend(minX,minY,maxX,maxY,ret);
    }
    private ArrayList<TreePoint> treeTops(GroundLayer groundLayer, Blocks blocks) {
        ArrayList<TreePoint> treePoints = new ArrayList<>();
        TreePoint[][] treePointTop = new TreePoint[blocks.getWidth()][blocks.getLength()];
        int[][] visited = new int[blocks.getWidth()][blocks.getLength()];

        for (XYZBlock block : blocks) {
            if (!block.block().classification().isPlant()) {
                continue;
            }
            TreePoint treePoint = new TreePoint(block.x(), block.y(), block.z());

            TreePoint currentHighestPoint = treePointTop[block.x()][block.y()];
            treePointTop[block.x()][block.y()] = (currentHighestPoint == null ? treePoint : ( currentHighestPoint.z > treePoint.z ? currentHighestPoint : treePoint));
            treePoints.add(treePoint);
        }
        if (treePoints.size() == 0) return new ArrayList<>();
        //processing the points of the trees from highest to lowest
        //because the tree trunk is probably where the highest point of the tree is
        treePoints.sort(Comparator.comparingInt(o -> o.z));

        ArrayList<TreePoint> treeCenters = new ArrayList<>();

        TreePoint highestPoint = treePoints.get(treePoints.size() - 1);
        treeCenters.add(highestPoint);

        LOGGER.debug("Tree points: " + treePoints.size());
        for (int i = treePoints.size()-1; i >= 0; i--) {
            TreePoint p = treePoints.get(i);

            int groundHeight = groundLayer.getHeightAt(p.x,p.y);
            //ignore the point, if it's a part of tree already
            if (p.partOfTree || p.z - groundHeight < MIN_TREE_HEIGHT) {
                continue;
            }
            if (visited[p.x][p.y] != 0) {
                continue;
            }

            TreeExtend treeTopExtend = extentOfTree(p, visited, i, treePointTop, 0);
            TreePoint centerOfTree = treeTopExtend.mostCenterPoint();
            if (centerOfTree == null) {
                throw new NullPointerException("Should not happen!");
            }

            TreeExtend extentOfTree = extentOfTree(centerOfTree, visited, -i, treePointTop, 3);
            double minDistance = extentOfTree.maxX - centerOfTree.x;
            minDistance = Math.min(minDistance, extentOfTree.maxY-centerOfTree.y);
            minDistance = Math.min(minDistance, centerOfTree.x - extentOfTree.minX);
            minDistance = Math.min(minDistance, centerOfTree.y - extentOfTree.minY);
            minDistance+=1;
            for (int x = extentOfTree.minX; x <= extentOfTree.maxX; x++) {
                for (int y = extentOfTree.minY; y <= extentOfTree.maxY; y++) {
                    if (visited[x][y] == -i) continue;
                    minDistance = Math.min(minDistance, new TreePoint(x,y,0).XYDistance(centerOfTree));
                }
            }
            if (minDistance <= 1.01) continue;

            minDistance++;
            int mDistance = (int) Math.round(minDistance);

            ArrayList<TreePoint> valid = new ArrayList<>();
            for (int x = centerOfTree.x - mDistance; x <= centerOfTree.x + mDistance; x++) {
                for (int y = centerOfTree.y - mDistance; y <= centerOfTree.y + mDistance; y++) {
                    if (x < 0 || y < 0 || x >= visited.length || y >= visited[0].length) continue;

                    TreePoint possiblePoint = treePointTop[x][y];
                    if (possiblePoint == null) continue;
                    if (possiblePoint.XYDistance(centerOfTree) <= mDistance && !possiblePoint.partOfTree) {
                        valid.add(possiblePoint);
                    }
                }
            }
            if (valid.size() <= 3) continue;
            for (TreePoint treePoint : valid) {
                treePoint.partOfTree = true;
                //blocks.setBlock(treePoint.x, treePoint.y, treePoint.z, new Block((byte) 35, (byte) randomData, Classification.MEDIUM_VEGETATION));
                visited[treePoint.x][treePoint.y] = Integer.MAX_VALUE;
            }

            treeCenters.add(centerOfTree);
        }
        LOGGER.info("Tree tops: " + treeCenters.size());
        return treeCenters;
    }
    private void addGrassCover(Blocks blocks) {
        Random rng = new Random(blocks.getWidth());
        GroundLayer groundLayer = blocks.getGroundLayer();
        for (int x = 0; x < groundLayer.getWidth(); x++) {
            for (int y = 0; y < groundLayer.getLength(); y++) {
                XYZBlock XYZBlock = groundLayer.getXYZBlock(x,y);
                //grass block
                if (XYZBlock.block().id() != 2
                        || rng.nextInt(100) > 40
                        || !blocks.isAir(XYZBlock.x(), XYZBlock.y(), XYZBlock.z()+1))
                    continue;
                blocks.set(XYZBlock.x(), XYZBlock.y(), XYZBlock.z() + 1, getGrassDecoration(rng));
            }
        }
    }
    private final Block[] flowers = new Block[] {
            new Block((byte) 37, (byte) 0, Classification.LOW_VEGETATION),
            new Block((byte) 38, (byte) 0, Classification.LOW_VEGETATION),
            new Block((byte) 38, (byte) 1, Classification.LOW_VEGETATION),
            new Block((byte) 38, (byte) 2, Classification.LOW_VEGETATION),
            new Block((byte) 38, (byte) 3, Classification.LOW_VEGETATION),
            new Block((byte) 38, (byte) 4, Classification.LOW_VEGETATION),
            new Block((byte) 38, (byte) 5, Classification.LOW_VEGETATION),
            new Block((byte) 38, (byte) 6, Classification.LOW_VEGETATION),
            new Block((byte) 38, (byte) 7, Classification.LOW_VEGETATION),
            new Block((byte) 38, (byte) 8, Classification.LOW_VEGETATION),
    };
    private Block getGrassDecoration(Random rng) {
        int number = rng.nextInt(100);
        if (number < 70) { //grass
            return new Block((byte) 31, (byte) 1, Classification.LOW_VEGETATION);
        }
        //else flowers
        return flowers[rng.nextInt(flowers.length)];
    }
}
