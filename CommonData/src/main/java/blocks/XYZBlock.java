package blocks;

import data.Block;

/**
 * Class that represents a block and it's location
 */
public class XYZBlock {
    int x,y,z; //package private so the values cannot be changed outside this package
    Block block;

    public XYZBlock(int x, int y, int z, Block block) {
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