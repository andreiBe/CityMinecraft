package org.patonki.util;

import org.patonki.data.IntBoundingBox;

public class BoundsCalculator2D {
    private int minX = Integer.MAX_VALUE;
    private int minY = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int maxY = Integer.MIN_VALUE;
    public void add(int x, int y) {
        this.minX = Math.min(this.minX, x);
        this.minY = Math.min(this.minY, y);
        this.maxX = Math.max(this.maxX, x);
        this.maxY = Math.max(this.maxY, y);
    }
    public IntBoundingBox get() {
        return new IntBoundingBox(minX, minY, maxX, maxY);
    }
}
