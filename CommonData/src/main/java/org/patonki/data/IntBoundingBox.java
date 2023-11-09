package org.patonki.data;

/**
 * A 2D area defined by int coordinates
 */
public record IntBoundingBox(int minX, int minY, int maxX, int maxY) {
    public int w() {
        return maxX - minX + 2;
    }
    public int h() {
        return maxY - minY + 2;
    }
}
