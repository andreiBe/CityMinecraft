package org.patonki.citygml.math;

public class Point2D {
    private final double x,y;

    public Point2D(double x, double y) {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            throw new IllegalArgumentException("Y and X cannot be NaN");
        }
        this.x = x;
        this.y = y;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    @Override
    public String toString() {
        return "(" + Math.round(x * 100.0) / 100.0 + ", " + Math.round(y * 100.0) / 100.0 + ")";
    }

    public double distance(double x, double y) {
        double xDif = x - this.x;
        double yDif = y - this.y;
        return Math.sqrt(xDif * xDif + yDif * yDif);
    }
}
