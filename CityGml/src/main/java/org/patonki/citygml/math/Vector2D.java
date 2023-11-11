package org.patonki.citygml.math;

public class Vector2D extends Point2D{
    public static final Vector2D ZERO = new Vector2D(0, 0);

    public Vector2D(double x, double y) {
        super(x, y);
    }

    public Vector2D(Point2D point) {
        super(point.x(), point.y());
    }

    public double len() {
        return Math.sqrt(x() * x() + y() *y());
    }
    public Vector2D normalize() {
        double len = len();
        if (len == 0) {
            throw new IllegalArgumentException("Length can't be zero");
        }
        return new Vector2D(x()/len, y()/len);
    }
    public double angle() {
        Vector2D v = normalize();
        if (v.y() > 1 || Double.isNaN(v.y())) {
            throw new IllegalArgumentException("Can't be over 1 or NaN, was: " + v.y());
        }
        if (v.x() < 0) {
            return v.y() < 0 ? -Math.PI - Math.asin(v.y()) : Math.PI - Math.asin(v.y());
        }
        return Math.asin(v.y());
    }

    public static Vector2D fromAtoB(Point2D p0, Point2D p1) {
        return new Vector2D(p1.x()-p0.x(), p1.y()-p0.y());
    }
    public static Vector2D fromAngle(double angle) {
        double x = Math.cos(angle);
        double y = Math.sin(angle);
        return new Vector2D(x,y).normalize();
    }
    public Vector2D add(Vector2D v) {
        return new Vector2D(x() + v.x(), y() + v.y());
    }
    public Vector2D multiply(double val) {
        return new Vector2D(x() * val, y() * val);
    }
}
