package blocks;

import data.Block;

public class Item {
    int x,y,z;
    Block block;

    public Item(int x, int y, int z, Block block) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.block = block;
    }
    public int x() {
        return x;
    }
    public int y() {
        return y;
    }
    public int z() {
        return z;
    }
    public Block block() {
        return block;
    }
}