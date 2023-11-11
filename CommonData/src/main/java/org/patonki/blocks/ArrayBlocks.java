package org.patonki.blocks;

import org.jetbrains.annotations.NotNull;
import org.patonki.data.Block;
import org.patonki.data.BlockSerializer;
import org.patonki.data.Classification;

import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An implementation of {@link Blocks}.
 * that uses a simple array of blocks. <br>
 * Because of memory limitations, this class only supports up to 256 (byte max value) different
 * types of blocks.
 */
public class ArrayBlocks extends Blocks {
    private final Block[] pallet = new Block[256];
    private final byte[] blocks; //indexes of the block pallet
    //each block mapped to it's index in the pallet
    private final HashMap<Block, Byte> ids = new HashMap<>();

    @Override
    public Iterator<XYZBlock> iterator() {
        return new XYZIterator(this, true);
    }
    @Override
    public Iterator<XYZBlock> getIterator(boolean bottomToUp) {
        return new XYZIterator(this, bottomToUp);
    }

    /**
     * @see Blocks#Blocks(int, int, int, int, int, int)
     */
    public ArrayBlocks(int width, int length, int height, int minX, int minY, int minZ) {
        super(width, length, height, minX, minY, minZ);
        this.blocks = new byte[width*length*height];
    }
    private int pos(int x, int y, int z) {
        return z * (this.width * this.length) + x*(this.length) + y;
    }

    public boolean set(int x, int y, int z, @NotNull Block block) {
        if (!inRange(x,y,z)) return false;

        Byte index = ids.get(block);
        if (index == null) { //new block
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
                    action.run(x,y,z, this.pallet[this.blocks[pos(x,y,z)]]);
                }
            }
        }
    }

    /**
     * Iterates over blocks.
     */
    public static class XYZIterator implements Iterator<XYZBlock> {
        private final ArrayBlocks blocks;
        private int x,y,z;

        private final XYZBlock XYZBlock;

        private final int zStep;
        public XYZIterator(ArrayBlocks blocks, boolean bottomToUp) {
            this.blocks = blocks;
            this.x = -1;
            this.XYZBlock = new XYZBlock(0,0,0, null);
            this.zStep = bottomToUp ? 1 : -1;
            this.z = bottomToUp ? 0 : blocks.height-1;
            advance();
        }
        @Override
        public boolean hasNext() {
            return z >= 0 && z < this.blocks.height;
        }
        private void advanceCoordinates() {
            x++;
            if (x == this.blocks.width) {
                x = 0;y++;
                if (y == this.blocks.length) {
                    y = 0; z+=zStep;
                }
            }
        }
        private void advance() {
            advanceCoordinates();
            while (z >= 0 && z < this.blocks.height && this.blocks.blocks[blocks.pos(x,y,z)] == 0) {
                advanceCoordinates();
            }
        }
        @Override
        public XYZBlock next() {
            if (z >= this.blocks.height)
                throw new NoSuchElementException();
            //Item item = this.blocks.blocks[x][y][z];
            this.XYZBlock.x = x;
            this.XYZBlock.y = y;
            this.XYZBlock.z = z;
            this.XYZBlock.block = this.blocks.pallet[this.blocks.blocks[this.blocks.pos(x,y,z)]];
            advance();
            return this.XYZBlock;
        }
    }

    /**
     * Enables serialization to and from byte arrays.
     * Used for caching to a file for later use.
     */
    public static class ArrayBlockSerializer extends BlockSerializer {
        @Override
        public ArrayBlocks deserialize(byte[] ar) {
            // 6 * 4 = width, length, height, minX, minY, minZ as bytes
            //the palette takes 256*3 bytes
            //it takes 1 byte to represent a block

            //the metadata
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
            //the block pallet
            for (int palletIndex = 0; palletIndex < blocks.pallet.length; palletIndex++) {
                byte id = ar[i];
                if (id == 0) {
                    blocks.pallet[palletIndex] = null;
                } else {
                    byte data = ar[i+1];
                    Classification classification = Classification.values()[ar[i+2]];
                    blocks.pallet[palletIndex] = new Block(id, data, classification);
                }
                i+=3;
            }
            //the actual blocks
            for (int x = 0; x < blocks.width; x++) {
                for (int y = 0; y < blocks.length; y++) {
                    for (int z = 0; z < blocks.height; z++) {
                        int index = blocks.pos(x,y,z);
                        index += i;
                        byte block = ar[index];
                        blocks.blocks[blocks.pos(x,y,z)] = block;

                        Block b = blocks.pallet[block];
                        if (b != null)
                            blocks.ids.put(b, block);
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

            //storing the metadata
            byte[] serialized = new byte[6*4 + blocks.pallet.length * 3 + blocks.width * blocks.length * blocks.height];
            writeInts(serialized, 0, blocks.width, blocks.length, blocks.height, blocks.minX, blocks.minY, blocks.minZ);
            int i = 6*4;
            //storing the pallet
            for (int palletIndex = 0; palletIndex < blocks.pallet.length; palletIndex++) {
                Block block = blocks.pallet[palletIndex];
                if (block != null) {
                    serialized[i] = block.id();
                    serialized[i+1] = block.data();
                    serialized[i+2] = (byte) Classification.index(block.classification());
                }
                i+=3;
            }
            //storing the blocks
            for (int x = 0; x < blocks.width; x++) {
                for (int y = 0; y < blocks.length; y++) {
                    for (int z = 0; z < blocks.height; z++) {
                        int index = blocks.pos(x,y,z);
                        index += i;
                        byte block = blocks.blocks[index];
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
        for (int i = 0; i < this.blocks.length; i++) {
            Block block = pallet[this.blocks[i]];
            if (block == null) continue;
            blocks[i] = block.id();
            data[i] = block.data();
        }
        return new Blocks.BlockData(blocks, data, (short) this.length, (short) this.width, (short) this.height, minX, minY, minZ);
    }
}
