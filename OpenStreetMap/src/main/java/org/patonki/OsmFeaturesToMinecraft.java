package org.patonki;

import org.patonki.blocks.Blocks;
import org.patonki.blocks.GroundLayer;
import org.patonki.blocks.XYZBlock;
import org.patonki.data.Block;
import org.patonki.data.Classification;
import org.patonki.landUse.LandUse;
import org.patonki.types.LandUseType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.patonki.roads.Road;
import org.patonki.waterways.Waterway;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OsmFeaturesToMinecraft {
    private Blocks blocks;
    private final ArrayList<LandUse> lands;
    private final ArrayList<Road> roads;
    private final ArrayList<Waterway> waterways;


    private static final Logger LOGGER = LogManager.getLogger(OsmFeaturesToMinecraft.class);

    public OsmFeaturesToMinecraft(ArrayList<LandUse> lands, ArrayList<Road> roads, ArrayList<Waterway> waterways) {
        this.lands = lands;
        this.roads = roads;
        this.waterways = waterways;
    }

    public void applyFeatures(Blocks blocks, boolean applyLandUse, boolean applyRoads, boolean applyWaterWays) {
        this.blocks = blocks;
        if (applyLandUse) {
            this.applyLandUse();
        }
        if (applyRoads) {
            this.applyRoads();
        }
        if (applyWaterWays) {
            this.applyWaterWays();
        }
    }

    private boolean shouldReplace(Block block) {
        return block.classification() == Classification.GROUND || block.classification() == Classification.LOW_VEGETATION;
    }
    private void replaceGroundBlocks(int x, int y, Block block) {
        for (int z = 0; z < blocks.getHeight(); z++) {
            Block b = blocks.get(x,y,z);
            if (b == null) continue;
            if (shouldReplace(b)) {
                blocks.set(x,y,z, block);
            }
        }
    }
    private <T extends Feature> T findFeatureAtCoordinate(int x, int y, List<T> features) {
        int realX = this.blocks.getMinX() + x;
        int realY = this.blocks.getMinY() + y;
        Optional<T> firstMatch = features.stream().filter(l -> l.polygon().contains(realX, realY)).findFirst();
        return firstMatch.orElse(null);
    }
    private void applyLandUse() {
        LOGGER.debug("Adding land-use information");
        ArrayList<LandUse> valid_lands = this.lands.stream()
                .filter(land -> land.polygon().intersects(this.blocks.getMinX(), this.blocks.getMinY(), this.blocks.getWidth(), this.blocks.getLength()))
                .filter(land -> !List.of(LandUseType.COMMERCIAL, LandUseType.RESIDENTIAL).contains(land.type()))
                .collect(Collectors.toCollection(ArrayList::new));
        LOGGER.debug("Found " + valid_lands.size() + " land-use areas in the area");
        for (int x = 0; x < blocks.getWidth(); x++) {
            for (int y = 0; y < blocks.getLength(); y++) {
                LandUse land = findFeatureAtCoordinate(x,y,valid_lands);
                if (land == null) continue;
                replaceGroundBlocks(x,y, land.block());
            }
        }
    }
    private void applyWaterWays() {
        LOGGER.debug("Adding waterways");
        ArrayList<Waterway> valid_waterways = this.waterways.stream()
                .filter(waterway -> waterway.polygon().intersects(blocks.getMinX(),blocks.getMinY(), blocks.getWidth(), blocks.getLength()))
                .collect(Collectors.toCollection(ArrayList::new));

        GroundLayer groundLayer = blocks.getGroundLayer();
        for (int x = 0; x < blocks.getWidth(); x++) {
            for (int y = 0; y < blocks.getLength(); y++) {
                Waterway waterway = findFeatureAtCoordinate(x, y, valid_waterways);
                if (waterway == null) continue;
                fillWater(x, y, groundLayer, waterway.block());
            }
        }
    }
    private void fillWater(int xo, int yo, GroundLayer groundLayer, Block waterBlock) {
        int radius = 5;
        int minZ = Integer.MAX_VALUE;
        ArrayList<XYZBlock> bottomLayer = new ArrayList<>();

        for (int x = xo-radius; x <= xo+radius; x++) {
            for (int y = yo-radius; y <= yo+radius; y++) {
                if (!groundLayer.inRange(x,y)) continue;
                XYZBlock i = groundLayer.getXYZBlock(x,y);
                if (i.z() < minZ) {
                    minZ = i.z();
                    if (i.block().classification() == Classification.WATER) minZ--;
                    bottomLayer.clear();
                }
                if (i.z() == minZ) {
                    bottomLayer.add(i);
                }
            }
        }

        for (XYZBlock XYZBlock : bottomLayer) {
            blocks.set(XYZBlock.x(), XYZBlock.y(), minZ+1, waterBlock);
        }
    }
    private void applyRoads() {
        LOGGER.debug("Adding roads");
        ArrayList<Road> valid_roads = this.roads.stream()
                .filter(road -> road.polygon().intersects(blocks.getMinX(),blocks.getMinY(), blocks.getWidth(), blocks.getLength()))
                .collect(Collectors.toCollection(ArrayList::new));
        LOGGER.debug("Found " + valid_roads.size() + " roads in the area");
        Road[][] roadsAtCoordinate = new Road[501][501];
        int[][] roadHeight = new int[501][501];
        for (int x = 0; x < blocks.getWidth(); x++) {
            for (int y = 0; y < blocks.getLength(); y++) {
                Road road = findFeatureAtCoordinate(x,y,valid_roads);
                if (road == null) continue;
                roadsAtCoordinate[x][y] = road;
                for (int z = 0; z < blocks.getHeight(); z++) {
                    Block b = blocks.get(x,y,z);

                    if (b == null) continue;
                    if (shouldReplace(b)) {
                        blocks.set(x,y,z, road.block());

                        roadHeight[x][y] = z;
                    }
                }
            }
        }
        GroundLayer groundLayer = blocks.getGroundLayer();
        for (int x = 0; x < blocks.getWidth(); x++) {
            for (int y = 0; y < blocks.getLength(); y++) {
                Road road = roadsAtCoordinate[x][y];
                if (road == null) continue;

                removeBuiltAreaAroundRoad(x,y, groundLayer, roadsAtCoordinate);
                int averageHeight = roadHeight[x][y];
                if (averageHeight == 0) averageHeight = averageGroundHeight( x-10,y-10,x+10,y+10);
                //for (int z = 0; z <= averageHeight; z++) blocks.setBlock(x,y,z,road.getBlock());
                for (int z = averageHeight; z < averageHeight+6; z++) {
                    Block block = blocks.get(x,y,z);
                    if (block == null) continue;
                    if (block.classification() == Classification.UNKNOWN || block.classification().isPlant()) {
                        blocks.remove(x,y,z);
                    }
                }
            }
        }
    }
    private void removeBuiltAreaAroundRoad(int xo, int yo, GroundLayer groundLayer, Road[][] roads) {
        ArrayList<XYZBlock> built = new ArrayList<>();
        int total = 0;
        int nature = 0;
        int radius = 5;
        for (int x = xo-radius; x <= xo+radius; x++) {
            for (int y = yo-radius; y <= yo+radius; y++) {
                if (x < 0 || y < 0 || x >= groundLayer.getWidth() || y >= groundLayer.getLength()) continue;
                XYZBlock i = groundLayer.getXYZBlock(x,y);
                Block block = i.block();
                if (roads[x][y] != null) continue;
                if (block.id() == 1) built.add(i);
                if (block.id() == 2) nature++;
                total++;
            }
        }
        if (nature < total/1.5) return;
        for (XYZBlock XYZBlock : built) {
            blocks.set(XYZBlock, new Block((byte) 2, (byte) 0, Classification.GROUND));
        }
    }
    private int averageGroundHeight( int x1, int y1, int x2, int y2) {
        int sum = 0;
        int amount = 0;
        for (int x = x1; x < x2; x++) {
            int highest = -1;
            for (int y = y1; y < y2; y++) {
                for (int z = 0; z < blocks.getHeight(); z++) {
                    Block block = blocks.get(x,y,z);
                    if (block != null && shouldReplace(block))
                        highest = z;
                }
            }
            if (highest == -1) continue;
            sum += highest;
            amount++;
        }

        if (amount == 0) {
            return 0;
        }
        return sum/amount;
    }


}
