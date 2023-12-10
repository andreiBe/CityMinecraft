package org.patonki.citygml;

import org.patonki.citygml.citygml.BlockLocation;
import org.patonki.citygml.citygml.BlockLocations;
import org.patonki.citygml.features.Building;
import org.patonki.citygml.features.BuildingStructure;
import org.patonki.citygml.features.Point;
import org.patonki.citygml.features.Polygon3D;
import org.patonki.citygml.math.Point2D;
import org.patonki.color.IColorToBlockConverter;
import org.patonki.data.*;
import org.patonki.util.Counter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts the building into a set of cubes that each have an id and data
 * The ids and data are the ids and data properties used by minecraft
 * <a href="https://minecraftitemids.com/">...</a>
 */
public class BuildingToCubes {
    private record Id(String id) {}

    //trying to make the textures of buildings more smooth
    private void fixRandomSportOfDifferentColor(HashMap<Id, List<BlockLocation>> structures) {
        for (Map.Entry<Id, List<BlockLocation>> entry : structures.entrySet()) {
            List<BlockLocation> smallList = entry.getValue();
            int numberOfBlocks = smallList.size();
            if (numberOfBlocks > 5 || smallList.isEmpty()) continue;
            BlockLocation b = smallList.get(0);
            int x = b.x();
            int y = b.y();
            Point2D p = new Point2D(x,y);
            //finding all blocks in a radius of 5
            Counter<Block> counter = new Counter<>();
            for (List<BlockLocation> list : structures.values()) {
                if (list == smallList) continue; // "==" comparison to compare for same instance
                for (BlockLocation blockLocation : list) {
                    if (p.distance(blockLocation.x(), blockLocation.y()) <= 5) {
                        counter.add(new Block(blockLocation.id(), blockLocation.data(), Classification.BUILDING));
                    }
                }
            }
            Block mostCommon = counter.isEmpty() ? new Block(b.id(), b.data(), Classification.BUILDING) : counter.getMostCommon();
            List<BlockLocation> newBlockLocations = smallList.stream()
                    .map(loc -> new BlockLocation(loc.x(), loc.y(), loc.z(), mostCommon.id(), mostCommon.data()))
                    .toList();
            structures.put(entry.getKey(), newBlockLocations);
        }
    }
    private List<BlockLocation> processStructure(BuildingStructure structure,
                                                 IColorToBlockConverter colorConverter,
                                                 TextureReader textureReader,
                                                 boolean oneColor,
                                                 BoundingBox3D building_box,
                                                 IntBoundingBox3D vbox) {
        Polygon3D polygon = structure.getPolygon();
        //getting a color array of the polygon's texture
        //the texture can contain texture coordinates, which are handled by the textureReader
        Block[][] colors = textureReader.textureOfPolygon(polygon, colorConverter, oneColor);

        ArrayList<BlockLocation> locations = new ArrayList<>();

        //the polygon is in 3d-space, but it's projected to 2d-space
        BoundingBox bBox2D = polygon.getBBox2D();

        //my suboptimal algorithm for turning a 3d shape into cubes (voxelization)
        //The algorithm begins from the lowest corner of the shape
        //and checks for every point, if it's contained by the polygon
        double step = 0.5;
        double xLimit = bBox2D.minX() + bBox2D.w();
        double yLimit = bBox2D.minY() + bBox2D.h();
        for (double x = bBox2D.minX(); x < xLimit; x += step) {
            for (double y = bBox2D.minY(); y < yLimit; y += step) {
                Point2D point2D = new Point2D(x, y);
                if (!polygon.contains(point2D)) {
                    continue;
                }
                int blockCoordinateX = (int) (x - bBox2D.minX());
                int blockCoordinateY = (int) (y - bBox2D.minY());
                Block block = colors[blockCoordinateX][blockCoordinateY];
                Point p = polygon.pointTo3D(point2D);
                int xs = (int) (p.x() - building_box.x());
                int ys = (int) (p.y() - building_box.y());
                int zs = (int) (p.z() - building_box.z());
                xs = Math.min(xs, vbox.w() - 1);
                ys = Math.min(ys, vbox.l() - 1);
                zs = Math.min(zs, vbox.h() - 1);
                locations.add(new BlockLocation(xs, ys, zs, block.id(), block.data()));
            }
        }
        return locations;
    }
    /**
     * @param building The building that should be represented in cubes
     * @param oneColor If true, the individual walls and roofs are colored similarly.
     *                 If false, the walls and roofs can contain multiple colors
     * @param colorConverter The class that is used to translate colors into Minecraft blocks
     * @return List of blocks with their locations relative to the lowest corner of the building
     * Meaning that the block at (minX, minY, minZ) has the coordinate (0,0,0)
     * The return value will also contain the {@link BoundingBox} of the building
     */
    public BlockLocations convert(Building building, boolean oneColor, IColorToBlockConverter colorConverter) {
        BoundingBox3D building_box = building.getBBox();
        //Bounding box that is guaranteed to be larger than
        //the floating point bounding box
        IntBoundingBox3D vbox = building.getVBox();

        TextureReader textureReader = new TextureReader();

        HashMap<Id, List<BlockLocation>> structures = new HashMap<>();


        for (BuildingStructure structure : building.getStructures()) {
            List<BlockLocation> locations = processStructure(structure, colorConverter, textureReader,
                    oneColor, building_box, vbox);
            structures.put(new Id(structure.getId()), locations);
        }
        fixRandomSportOfDifferentColor(structures);

        ArrayList<BlockLocation> locations = new ArrayList<>();
        structures.values().forEach(locations::addAll);

        return new BlockLocations(locations, building_box);
    }

}
