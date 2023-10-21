package roads;

import data.Block;
import endpoint.FeatureReader;
import endpoint.settings.RoadInfo;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import types.RoadType;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class RoadReader extends FeatureReader<Road, RoadType, RoadInfo> {
    public RoadReader(InfoSupplier<RoadType, RoadInfo> supplier) {
        super(supplier);
    }

    @Override
    protected boolean isValid(Feature feature) {
        if (!feature.getAttribute("bridge").equals("F")) {
            return false;
        }
        String type_string = (String) feature.getAttribute("fclass");
        try {
            RoadType.matchingType(type_string);
        }  catch (IllegalArgumentException e) {
            System.out.println("Ignoring road: " + e.getMessage());
            return false;
        }
        return feature.getAttribute("tunnel").equals("F");
    }
    private Polygon pointsToPolygon(Geometry geometry, int width) {
        Geometry buffered = geometry.buffer(width);
        Coordinate[] coordinates = buffered.getCoordinates();
        int[] x = Arrays.stream(coordinates).mapToInt(c -> (int) c.x).toArray();
        int[] y = Arrays.stream(coordinates).mapToInt(c -> (int) c.y).toArray();
        return new Polygon(x, y, coordinates.length);
    }
    @Override
    protected Road process(Feature feature) {
        String type_string = (String) feature.getAttribute("fclass");
        RoadType type = RoadType.matchingType(type_string);

        String roadname = (String) feature.getAttribute("name");

        RoadInfo roadInfo = this.supplier.get(type);
        int width = roadInfo.width();
        Block block = roadInfo.block();
        Polygon pol = pointsToPolygon(feature.geometry(), width);

        return new Road(pol, roadname, type, block, width);
    }

    @Override
    public ArrayList<Road> read(String path) throws FactoryException, IOException, TransformException {
        ArrayList<Road> roads = super.read(path);
        Collections.sort(roads);
        return roads;
    }
}
