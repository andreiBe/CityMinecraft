package roads;

import data.Block;
import endpoint.Feature;

import java.awt.*;

public record Road(Polygon polygon, String name, RoadType type, Block block,
                   int width) implements Comparable<Road>, Feature {
    @Override
    public int compareTo(Road r) {
        return Integer.compare(r.width, this.width);
    }
}