package landUse;

import data.Block;
import endpoint.FeatureReader;
import endpoint.settings.LandUseInfo;
import org.locationtech.jts.geom.Coordinate;
import types.LandUseType;

import java.awt.*;
import java.util.Arrays;

public class LandUseReader extends FeatureReader<LandUse, LandUseType, LandUseInfo> {
    public LandUseReader(InfoSupplier<LandUseType, LandUseInfo> supplier) {
        super(supplier);
    }

    @Override
    protected boolean isValid(Feature feature) {
        String type_string = (String) feature.getAttribute("fclass");
        try {
            LandUseType.matchingType(type_string);
        }  catch (IllegalArgumentException e) {
            System.out.println("Ignoring land: " + e.getMessage());
            return false;
        }
        return true;
    }

    private Polygon toPolygon(Coordinate[] coordinates) {
        int[] xs = Arrays.stream(coordinates).mapToInt(c -> (int) c.x).toArray();
        int[] ys = Arrays.stream(coordinates).mapToInt(c -> (int) c.y).toArray();
        return new Polygon(xs,ys, coordinates.length);
    }
    @Override
    protected LandUse process(Feature feature) {
        String type_string = (String) feature.getAttribute("fclass");
        LandUseType type = LandUseType.matchingType(type_string);
        Block block = this.supplier.get(type).block();
        Polygon pol = toPolygon(feature.coordinates());
        return new LandUse(block, pol, type);
    }
}
