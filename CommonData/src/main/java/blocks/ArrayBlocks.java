package blocks;

import data.Block;
import data.BlockSerializer;
import data.Classification;
import data.Serializer;

import java.util.*;

public class ArrayBlocks extends Blocks {
    private final byte[] blocks; //minecraft blocks
    private final HashMap<Block, Byte> ids = new HashMap<>();
    private final Block[] pallet = new Block[256];

    @Override
    public Iterator<Item> iterator() {
        return new XYZIterator(this);
    }

    public ArrayBlocks(int width, int length, int height, int minX, int minY, int minZ) {
        super(width, length, height, minX, minY, minZ);
        this.blocks = new byte[width*length*height];
    }
    private int pos(int x, int y, int z) {
        return z * (this.width * this.length) + x*(this.length) + y;
    }
    public boolean set(int x, int y, int z, Block block) {
        Byte index = ids.get(block);
        if (index == null) {
            byte size = (byte) (ids.size() + 1);
            ids.put(block, size);
            this.pallet[size] = block;
            this.blocks[pos(x,y,z)] = size;
        } else {
            this.blocks[pos(x,y,z)] = index;
        }
        return true;
    }

    @Override
    public boolean remove(int x, int y, int z) {
        this.blocks[pos(x,y,z)] = 0;
        return true;
    }

    @Override
    public int size() {
        return width * length * height;
    }

    public Block get(int x, int y, int z) {
        if (!inRange(x,y,z)) {
            return null;
        }
        return this.pallet[this.blocks[pos(x,y,z)]];
    }

    @Override
    public void forEach(BlockAction action) {
        for (int x = 0; x < this.width; x++) {
            for (int y = 0; y < this.length; y++) {
                for (int z = 0; z < this.height; z++) {
                    action.run(x,y,z, this.pallet[pos(x,y,z)]);
                }
            }
        }
    }

    public static class XYZIterator implements Iterator<Item> {
        private final ArrayBlocks blocks;
        private int x,y,z;

        private final Item item;
        public XYZIterator(ArrayBlocks blocks) {
            this.blocks = blocks;
            this.x = -1;
            this.item = new Item(0,0,0, null);
            advance();
        }
        @Override
        public boolean hasNext() {
            return z < this.blocks.height;
        }
        private void advanceCoordinates() {
            x++;
            if (x == this.blocks.width) {
                x = 0;y++;
                if (y == this.blocks.length) {
                    y = 0;z++;
                }
            }
        }
        private void advance() {
            advanceCoordinates();
            while (z < this.blocks.height && this.blocks.blocks[blocks.pos(x,y,z)] == 0) {
                advanceCoordinates();
            }
        }
        @Override
        public Item next() {
            if (z >= this.blocks.height)
                throw new NoSuchElementException();
            //Item item = this.blocks.blocks[x][y][z];
            this.item.x = x;
            this.item.y = y;
            this.item.z = z;
            this.item.block = this.blocks.pallet[this.blocks.blocks[this.blocks.pos(x,y,z)]];
            advance();
            return this.item;
        }
    }

    public static class ArrayBlockSerializer extends BlockSerializer {
        @Override
        public ArrayBlocks deserialize(byte[] ar) {
            int[] ints = readInts(ar, 0, 6);
            int width, length, height, minX, minY, minZ;
            width = ints[0];
            length = ints[1];
            height = ints[2];
            minX = ints[3];
            minY = ints[4];
            minZ = ints[5];
            ArrayBlocks blocks = new ArrayBlocks(width, length, height, minX, minY, minZ);
            int i = 6 * 4;
            for (int palletIndex = 0; palletIndex < blocks.pallet.length; palletIndex++) {
                byte id = ar[i];
                byte data = ar[i+1];
                Classification classification = Classification.values()[ar[i+2]];
                blocks.pallet[palletIndex] = new Block(id, data, classification);
                i+=3;
            }
            for (int x = 0; x < blocks.width; x++) {
                for (int y = 0; y < blocks.length; y++) {
                    for (int z = 0; z < blocks.height; z++) {
                        int index = (z * blocks.length + y) * blocks.width + x;
                        index += i;
                        byte block = ar[index];
                        blocks.blocks[blocks.pos(x,y,z)] = block;
                    }
                }
            }
            return blocks;
        }
        @Override
        public byte[] serialize(Blocks blocksUnknown) {
            ArrayBlocks blocks = (ArrayBlocks) blocksUnknown;
            // 6 * 4 = width, length, height, minX, minY, minZ as bytes
            //the palette takes 256*3 bytes
            //it takes 1 byte to represent a block

            byte[] serialized = new byte[6*4 + blocks.pallet.length * 3 + blocks.width * blocks.length * blocks.height];
            writeInts(serialized, 0, blocks.width, blocks.length, blocks.height, blocks.minX, blocks.minY, blocks.minZ);
            int i = 6*4;
            for (int palletIndex = 0; palletIndex < blocks.pallet.length; palletIndex++) {
                Block block = blocks.pallet[palletIndex];
                if (block == null) continue;
                serialized[i] = block.id();
                serialized[i+1] = block.data();
                serialized[i+2] = (byte) Classification.index(block.classification());
                i+=3;
            }
            for (int x = 0; x < blocks.width; x++) {
                for (int y = 0; y < blocks.length; y++) {
                    for (int z = 0; z < blocks.height; z++) {
                        int index = (z * blocks.length + y) * blocks.width + x;
                        index += i;
                        byte block = blocks.blocks[blocks.pos(x,y,z)];
                        serialized[index] = block;
                    }
                }
            }
            return serialized;
        }
    }

    @Override
    public Blocks.BlockData getBlockData() {
        byte[] blocks = new byte[this.width*this.length*this.height];
        byte[] data = new byte[blocks.length];
        for (int i = 0; i < blocks.length; i++) {
            Block block = pallet[blocks[i]];
            if (block == null) continue;
            blocks[i] = block.id();
            data[i] = block.data();
        }
        return new Blocks.BlockData(blocks, data, (short) this.length, (short) this.width, (short) this.height, minX, minY, minZ);
    }
}
