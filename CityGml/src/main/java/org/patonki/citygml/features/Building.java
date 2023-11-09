package org.patonki.citygml.features;

import org.patonki.data.BoundingBox3D;

public class Building extends Feature{
    private final Wall[] walls;
    private final Roof[] roofs;

    public Building(Wall[] walls, Roof[] roofs) {
        this.walls = walls;
        this.roofs = roofs;
    }

    public Wall[] getWalls() {
        return walls;
    }
    @SuppressWarnings("ManualArrayCopy")
    public BuildingStructure[] getStructures() {
        BuildingStructure[] structures = new BuildingStructure[walls.length + roofs.length];
        for (int i = 0; i < walls.length; i++) {
            structures[i] = walls[i];
        }
        for (int i = 0; i < roofs.length; i++) {
            structures[walls.length+i] = roofs[i];
        }
        return structures;
    }

    public Roof[] getRoofs() {
        return roofs;
    }

    public BoundingBox3D getBBox() {
        BuildingStructure[] structures = getStructures();
        return getBoundingBoxFromArray(structures);
    }
}
