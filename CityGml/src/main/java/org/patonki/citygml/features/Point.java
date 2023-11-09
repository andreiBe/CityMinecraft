package org.patonki.citygml.features;


import org.patonki.data.BoundingBox3D;

import java.util.Objects;

public class Point extends Feature{
    private final double x,y,z;

    public Point(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z) || Double.isInfinite(x) || Double.isInfinite(y) || Double.isInfinite(z)) {
            throw new IllegalArgumentException("X,Y and Z cannot be NaN or infinity " + x + " " + y + " " + z);
        }
    }
    public double distance(Point point) {
        double x = x() - point.x();
        double y = y() - point.y();
        double z = z() - point.z();
        return Math.sqrt(x*x + y*y + z*z);
    }

    @Override
    public String toString() {
        return "(" +
                Math.round(x * 100)/100.0 +
                "," + Math.round(y*100)/100.0 +
                "," + Math.round(z*100)/100.0 +
                ')';
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    @Override
    public BoundingBox3D getBBox() {
        return new BoundingBox3D(x,y,z,0,0,0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return Double.compare(point.x, x) == 0 && Double.compare(point.y, y) == 0 && Double.compare(point.z, z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }


}

