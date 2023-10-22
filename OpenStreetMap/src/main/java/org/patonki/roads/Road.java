package org.patonki.roads;

import org.patonki.data.Block;
import org.patonki.Feature;
import org.patonki.types.RoadType;

import java.awt.*;

public record Road(Polygon polygon, String name, RoadType type, Block block,
                   int width) implements Comparable<Road>, Feature {
    @Override
    public int compareTo(Road r) {
        return Integer.compare(r.width, this.width);
    }
}