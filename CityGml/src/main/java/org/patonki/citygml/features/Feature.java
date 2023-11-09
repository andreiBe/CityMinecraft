package org.patonki.citygml.features;


import org.patonki.data.BoundingBox3D;
import org.patonki.data.IntBoundingBox;
import org.patonki.data.IntBoundingBox3D;

public abstract class Feature {
    public abstract BoundingBox3D getBBox();

    public IntBoundingBox3D getVBox() {
        return bboxToVoxelBox(getBBox());
    }
    public IntBoundingBox3D bboxToVoxelBox(BoundingBox3D bbox) {
        int x = (int) (bbox.x());
        int y = (int) (bbox.y());
        int z = (int) (bbox.z());
        int w = (int) (bbox.w())+2;
        int l = (int) (bbox.l())+2;
        int h = (int) (bbox.h())+2;
        return new IntBoundingBox3D(x,y,z,w,l,h);
    }
    protected BoundingBox3D getBoundingBoxFromArray(Feature[] features) {
        double minX, minY, minZ;
        minX = minY = minZ = 999999999999999999f;
        double maxX, maxY, maxZ;
        maxX = maxY = maxZ = 0;
        for (Feature feature : features) {
            BoundingBox3D box = feature.getBBox();
            minX = Math.min(minX, box.x());
            minY = Math.min(minY, box.y());
            minZ = Math.min(minZ, box.z());
            maxX = Math.max(maxX, box.x() + box.w());
            maxY = Math.max(maxY, box.y() + box.l());
            maxZ = Math.max(maxZ, box.z() + box.h());
        }
        double w = maxX-minX;
        double l = maxY-minY;
        double h = maxZ-minZ;
        return new BoundingBox3D(minX,minY,minZ, w,l,h);
    }
}
