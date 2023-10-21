package blocks;


import data.Block;
import data.Classification;

import java.util.ArrayList;
import java.util.List;

public abstract class Blocks implements Iterable<Item>{
    protected final int width, length, height, minX, minY, minZ;

    public Blocks(int width, int length, int height, int minX, int minY, int minZ) {
        this.width = width;
        this.length = length;
        this.height = height;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
    }
    public boolean inRange(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < this.width && y < this.length && z < this.height;
    }

    public abstract boolean set(int x, int y, int z, Block block);
    public boolean set(Item item, Block block) {
        return this.set(item.x, item.y, item.z, block);
    }

    public boolean isAir(int x, int y, int z) {
        return get(x, y, z) == null;
    }
    public boolean isAir(Item item) {
        return isAir(item.x, item.y, item.z);
    }

    public abstract boolean remove(int x, int y, int z);
    public boolean remove(Item item) {
        return this.remove(item.x, item.y, item.z);
    }

    public abstract Block get(int x, int y, int z);
    public Block get(Item item) {
        return this.get(item.x, item.y, item.z);
    }

    public int neighborsCount(Item item) {return neighborsCount(item.x, item.y, item.z);}
    public int neighborsCount(int x, int y, int z) {
        int sum = 0;
        if (get(x-1, y, z)!=null) sum++;
        if (get(x, y-1, z)!=null) sum++;
        if (get(x, y, z-1)!=null) sum++;
        if (get(x+1, y, z)!=null) sum++;
        if (get(x, y+1, z)!=null) sum++;
        if (get(x, y, z+1)!=null) sum++;
        return sum;
    }

    public int numberOfNeighboringBlocksWithClassification(int x, int y, int z, Classification classification) {
        int sum = 0;
        if (classification == (get(x-1, y, z)).classification()) sum++;
        if (classification == (get(x, y-1, z)).classification()) sum++;
        if (classification == (get(x, y, z-1)).classification()) sum++;
        if (classification == (get(x+1, y, z)).classification()) sum++;
        if (classification == (get(x, y+1, z)).classification()) sum++;
        if (classification == (get(x, y, z+1)).classification()) sum++;
        return sum;
    }
    public int numberOfNeighboringBlocksWithClassification(Item item, Classification classification) {
        return numberOfNeighboringBlocksWithClassification(item.x, item.y, item.z, classification);
    }

    public int numberOfBlocksWithClassificationInRadius(int x, int y, int z, Classification classification, int radius) {
        int sum = 0;
        for (int x1 = x-radius; x1 < x+radius; x1++) {
            for (int y1 = y-radius; y1 < y+radius; y1++) {
                for (int z1 = z-radius; z1 < z+radius; z1++) {
                    Block b = get(x1,y1,z1);
                    if (classification == b.classification()) sum++;
                }
            }
        }
        return sum;
    }
    public int numberOfBlocksWithClassificationInRadius(Item item, Classification classification, int radius) {
        return numberOfBlocksWithClassificationInRadius(item.x, item.y, item.z, classification, radius);
    }
    protected void addIfNotNull(List<Item> list, int x, int y, int z) {
        Block block = get(x, y, z);
        if (block != null) {
            list.add(new Item(x,y,z, block));
        }
    }
    public List<Item> neighborsList(Item item) {
        return this.neighborsList(item.x, item.y, item.z);
    }
    public List<Item> neighborsList(int x, int y, int z) {
        ArrayList<Item> items = new ArrayList<>();
        addIfNotNull(items, x-1, y, z);
        addIfNotNull(items, x+1, y, z);
        addIfNotNull(items, x, y-1, z);
        addIfNotNull(items, x, y+1, z);
        addIfNotNull(items, x, y, z-1);
        addIfNotNull(items, x, y, z+1);
        return items;
    }
    public List<Item> xyNeighborsList(Item item) {
        return this.xyNeighborsList(item.x, item.y, item.z);
    }

    public List<Item> xyNeighborsList(int x, int y, int z) {
        ArrayList<Item> items = new ArrayList<>();
        addIfNotNull(items, x-1, y, z);
        addIfNotNull(items, x+1, y, z);
        addIfNotNull(items, x, y-1, z);
        addIfNotNull(items, x, y+1, z);
        return items;
    }


    public GroundLayer getGroundLayer() {
        return new GroundLayer(getGroundLayerIncomplete());
    }
    public Item[][] getGroundLayerIncomplete() {
        Item[][] ar = new Item[getWidth()][getLength()];
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getLength(); y++) {
                for (int z = getHeight()-1; z >= 0; z--) {
                    Block block = get(x,y,z);
                    if (block != null && block.classification() == Classification.GROUND) {
                        ar[x][y] = new Item(x,y,z, block);
                        break;
                    }
                }
            }
        }
        return ar;
    }
    public interface BlockAction {
        void run(int x, int y, int z, Block block);
    }
    public void forEach(BlockAction action) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < length; y++) {
                for (int z = 0; z < height; z++) {
                    action.run(x,y,z, get(x,y,z));
                }
            }
        }
    }


    public record BlockData(byte[] blockIds, byte[] blockData,
                            short width, short length, short height,
                            int minX, int minY, int minZ) {}

    public BlockData getBlockData() {
        byte[] blocks = new byte[this.width*this.length*this.height];
        byte[] data = new byte[blocks.length];
        this.forEach((x, y, z, b) -> {
            int i = z * (this.width * this.length) + x*(this.length) + y;
            if (b == null) return;
            blocks[i] = b.id();
            data[i] = b.data();
        });
        return new Blocks.BlockData(blocks, data, (short) this.length, (short) this.width, (short) this.height, minX, minY, minZ);
    }


    public int getWidth() {
        return width;
    }

    public int getLength() {
        return length;
    }

    public int getHeight() {
        return height;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public abstract int size();
}
