package blocks;


import data.Block;

public interface Blocks extends Iterable<Item>{
    boolean set(int x, int y, int z, Block block);
    default boolean set(Item item, Block block) {
        return this.set(item.x, item.y, item.z, block);
    }
    boolean remove(int x, int y, int z);
    default boolean remove(Item item) {
        return this.remove(item.x, item.y, item.z);
    }

    Block get(int x, int y, int z);
    default Block get(Item item) {
        return this.get(item.x, item.y, item.z);
    }

    GroundLayer getGroundLayer();

    int getWidth();
    int getLength();
    int getHeight();
    int getMinX();
    int getMinY();
    int getMinZ();
    int size();
}
