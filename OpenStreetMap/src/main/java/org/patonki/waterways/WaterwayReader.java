package org.patonki.waterways;

import org.patonki.FeatureReader;
import org.patonki.data.Block;
import org.patonki.data.Classification;
import org.patonki.openstreetmap.settings.WaterwayInfo;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.patonki.types.WaterWayType;

import java.awt.*;
import java.util.Arrays;

public class WaterwayReader extends FeatureReader<Waterway, WaterWayType, WaterwayInfo> {
    public WaterwayReader(InfoSupplier<WaterWayType, WaterwayInfo> supplier) {
        super(supplier);
    }

    private Polygon pointsToPolygon(Geometry geometry, int width) {
        Geometry buffered = geometry.buffer(width);
        Coordinate[] coordinates = buffered.getCoordinates();
        int[] x = Arrays.stream(coordinates).mapToInt(c -> (int) c.x).toArray();
        int[] y = Arrays.stream(coordinates).mapToInt(c -> (int) c.y).toArray();
        return new Polygon(x, y, coordinates.length);
    }
    @Override
    protected Waterway process(Feature feature) {
        String type_string = (String) feature.getAttribute("fclass");
        int width = (int) feature.getAttribute("width");
        String name = (String) feature.getAttribute("name");
        width = Math.max(width, 1);
        Polygon pol = pointsToPolygon(feature.geometry(), width);
        return new Waterway( width, WaterWayType.matchingType(type_string), name, pol, new Block(22, 0, Classification.WATER));
    }

}
