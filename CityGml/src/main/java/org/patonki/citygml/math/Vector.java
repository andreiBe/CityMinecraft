package org.patonki.citygml.math;


import org.patonki.citygml.features.Point;

public class Vector extends Point {
    public Vector(double x, double y, double z) {
        super(x, y, z);
    }

    public Vector(Point p0) {
        super(p0.x(),p0.y(),p0.z());
    }

    public static Vector fromAtoB(Point p0, Point p1) {
        return new Vector(p1.x()-p0.x(), p1.y()-p0.y(), p1.z()-p0.z());
    }

    public Vector normalize() {
        double len = Math.sqrt(x() * x() + y() *y() + z() * z());
        if (len == 0) throw new IllegalArgumentException("Length can't be zero");
        try {
            return new Vector(x()/len, y()/len, z()/len);
        } catch (IllegalArgumentException e) {
            System.out.println(x() + " " + y() + " " + z() + " "+ len);
            throw e;
        }
    }

    public double dot(Vector v) {
        return x() * v.x() + y() * v.y() + z() * v.z();
    }

    public Vector cross(Vector v) {
        return new Vector(
                y()*v.z()-z()*v.y(),
                -(x()*v.z()-z()*v.x()),
                x()*v.y()-y()*v.x()
        );
    }

    public Vector subtract(Vector o) {
        return new Vector(x()-o.x(), y()-o.y(), z()- o.z());
    }
    public Vector add(Vector o) {
        return new Vector(x()+o.x(), y()+o.y(), z()+o.z());
    }
    public Vector multiply(double val) {
        return new Vector(x()*val, y()*val, z()*val);
    }

}
