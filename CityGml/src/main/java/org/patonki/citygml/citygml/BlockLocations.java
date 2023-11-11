package org.patonki.citygml.citygml;

import org.patonki.data.BoundingBox3D;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BlockLocations implements Iterable<BlockLocation>{
    private final ArrayList<BlockLocation> locations;
    private final BoundingBox3D boundingBox;

    public BlockLocations(ArrayList<BlockLocation> locations, BoundingBox3D boundingBox) {
        this.locations = locations;
        this.boundingBox = boundingBox;
    }

    public List<BlockLocation> getLocations() {
        return locations;
    }

    public BoundingBox3D getBoundingBox() {
        return boundingBox;
    }

    @Override
    public Iterator<BlockLocation> iterator() {
        return this.locations.iterator();
    }
}
