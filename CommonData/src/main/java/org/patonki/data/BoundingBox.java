package org.patonki.data;

/**
 * A 2D-area defined by double coordinates
 */
public record BoundingBox(double minX, double minY, double maxX, double maxY) {
}
