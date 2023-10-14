package waterways;


import endpoint.Feature;

import java.awt.*;

public record Waterway(int width, WaterWayType type, String name,
                       Polygon polygon) implements Feature {
}
