package org.patonki.data;

/**
 * A 2D-area defined by double coordinates
 */
public record BoundingBox(double minX, double minY, double maxX, double maxY) {
    public double w() {
        return maxX - minX +1;
    }

    public double h() {
        return maxY - minY + 1;
    }
}
