package org.patonki.waterways;


import org.patonki.Feature;
import org.patonki.data.Block;
import org.patonki.types.WaterWayType;

import java.awt.*;

public record Waterway(int width, WaterWayType type, String name,
                       Polygon polygon, Block block) implements Feature {
}
