package blocks;

import data.Block;

public class GroundLayer {
    private Item[][] ground;
    private final int width;
    private final int length;

    public GroundLayer(Item[][] ground) {
        this.ground = ground;
        this.width = ground.length;
        this.length = ground[0].length;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < length; y++) {
                Item item = ground[x][y];
                if (item == null) {
                    throw new NullPointerException("Null value found at coordinates: " + x + " " + y);
                }
                if (item.block == null) {
                    throw new NullPointerException("Null block found at: " + x + " " + y);
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
    public Item getItem(int x, int y) {
        return ground[x][y];
    }

    public int getHeightAt(int x, int y) {
        return ground[x][y].z;
    }
}
