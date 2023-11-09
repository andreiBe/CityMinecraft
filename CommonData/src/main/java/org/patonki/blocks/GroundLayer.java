package org.patonki.blocks;

import org.patonki.data.Block;

/**
 * Represents the ground surface of an area. <br>
 * The highest ground point can be retrieved from a (x,y)-coordinate <br>
 * The ground surface is guaranteed to be filled (no holes).
 */
public class GroundLayer {
    private final XYZBlock[][] ground;
    private final int width;
    private final int length;

    public GroundLayer(XYZBlock[][] ground) {
        this.ground = ground;
        this.width = ground.length;
        this.length = ground[0].length;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < length; y++) {
                XYZBlock XYZBlock = ground[x][y];
                if (XYZBlock == null) {
                    throw new IllegalArgumentException("Null value found at coordinates: " + x + " " + y);
                }
                if (XYZBlock.block == null) {
                    throw new IllegalArgumentException("Null block found at: " + x + " " + y);
                }
            }
        }
    }
    public boolean inRange(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < length;
    }

    public int getWidth() {
        return width;
    }

    public int getLength() {
        return length;
    }
    public Block getBlock(int x, int y) {
        return ground[x][y].block;
    }
    public XYZBlock getXYZBlock(int x, int y) {
        return ground[x][y];
    }

    public int getHeightAt(int x, int y) {
        return ground[x][y].z;
    }
}
