package org.patonki.citygml.features;


import org.patonki.data.BoundingBox3D;

public sealed class BuildingStructure extends Feature permits Wall, Roof{
    private final Polygon3D polygon;
    private final String id;

    public BuildingStructure( Polygon3D polygon, String id) {
        this.polygon = polygon;
        this.id = id;
    }

    public Polygon3D getPolygon() {
        return polygon;
    }

    @Override
    public BoundingBox3D getBBox() {
        return polygon.getBBox();
    }

    public String getId() {
        return id;
    }
}
