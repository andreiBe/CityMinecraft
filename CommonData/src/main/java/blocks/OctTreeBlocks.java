package blocks;

import blocks.nodes.Node;
import blocks.nodes.ParentNode;
import data.Block;
import data.BlockSerializer;
import data.Classification;

import java.util.*;
/**
 * An implementation of {@link Blocks}
 * that uses an Oct tree. Which is an algorithm used to reduce memory consumption of storing
 * voxels (blocks).<br>
 * Because of memory limitations, this class only supports up to 256 (byte max value) different
 * types of blocks.
 */
public class OctTreeBlocks extends Blocks {
    private final Node blocks;
    private final HashMap<Block, Byte> ids = new HashMap<>();
    private final Block[] pallet = new Block[256];
    private final int maxSize;

    /**
     * @param maxSize The maximum number of blocks inside one Node in the oct tree.
     * @see Blocks#Blocks(int, int, int, int, int, int)
     */
    public OctTreeBlocks(int width, int length, int height, int minX, int minY, int minZ, int maxSize) {
        super(width, length, height, minX, minY, minZ);
        this.maxSize = maxSize;
        this.blocks = new ParentNode(0,0,0, width, length, height, maxSize);
    }

    public boolean set(int x, int y, int z, Block block) {
        Byte index = ids.get(block);
        if (index == null) {
            byte size = (byte) (ids.size() + 1);
            ids.put(block, size);
            this.pallet[size] = block;
            this.blocks.set(x,y,z, size);
        } else {
            this.blocks.set(x,y,z, index);
        }
        return true;
    }

    @Override
    public boolean remove(int x, int y, int z) {
        return this.blocks.set(x,y,z, (byte)0);
    }

    @Override
    public int size() {
        return this.blocks.size();
    }

    public Block get(int x, int y, int z) {
        return this.pallet[this.blocks.get(x,y,z)];
    }

    private final Node.BlockConverter converter = b -> this.pallet[b];

    @Override
    public XYZBlock[][] getGroundLayerIncomplete() {
        XYZBlock[][] ar = new XYZBlock[getWidth()][getLength()];
        this.blocks.groundLayer(ar, converter);
        return ar;
    }
    public Object getMaxSize() {
        return this.maxSize;
    }


    @Override
    public void forEach(BlockAction action) {
        this.blocks.forEach((x, y, z, b) -> action.run(x,y,z, pallet[b]));
    }

    @Override
    public int numberOfBlocksWithClassificationInRadius(int x, int y, int z, Classification classification, int radius) {
        final int[] num = {0};
        this.blocks.forEach((x1, y1, z1, b) -> {
            if (classification == this.pallet[b].classification()) num[0]++;
        }, new Node.BBox(x - radius, y - radius, z - radius, x + radius, y + radius, z +radius));
        return num[0];
    }
    @Override
    public Iterator<XYZBlock> iterator() {
        return new XYZIterator();
    }

    /**
     * Iterates over blocks
     */
    public class XYZIterator implements Iterator<XYZBlock> {
        private final XYZBlock XYZBlock;
        private final Iterator<Node.ByteItem> iterator;

        public XYZIterator() {
            this.iterator = blocks.iterator();
            this.XYZBlock = new XYZBlock(0,0,0, null);
        }
        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public XYZBlock next() {
            if (!iterator.hasNext())
                throw new NoSuchElementException();
            Node.ByteItem next = iterator.next();
            this.XYZBlock.x = next.x;
            this.XYZBlock.y = next.y;
            this.XYZBlock.z = next.z;
            this.XYZBlock.block = pallet[next.block];
            return this.XYZBlock;
        }
    }
    /**
     * Enables serialization to and from byte arrays.
     * Used for caching to a file for later use
     */
    public static class OctTreeBlocksSerializer extends BlockSerializer {
        @Override
        public byte[] serialize(Blocks blocksUnknown) {
            OctTreeBlocks blocks = (OctTreeBlocks) blocksUnknown;
            // 7 * 4 = width, length, height, minX, minY, minZ, maxSize as bytes
            //the palette takes 256*3 bytes
            //it takes 1 byte to represent a block

            //storing the metadata
            byte[] serialized = new byte[7*4 + blocks.pallet.length * 3 + blocks.width * blocks.length * blocks.height];
            writeInts(serialized, 0, blocks.width, blocks.length, blocks.height, blocks.minX, blocks.minY, blocks.minZ, blocks.maxSize);
            int i = 7*4;
            //storing the pallet
            for (int palletIndex = 0; palletIndex < blocks.pallet.length; palletIndex++) {
                Block block = blocks.pallet[palletIndex];
                if (block == null) continue;
                serialized[i] = block.id();
                serialized[i+1] = block.data();
                serialized[i+2] = (byte) Classification.index(block.classification());
                i+=3;
            }
            //storing the blocks
            int finalI = i;
            blocks.blocks.forEach((x, y, z, b) -> {
                int index = (z * blocks.length + y) * blocks.width + x;
                index += finalI;
                serialized[index] = b;
            });
            return serialized;
        }

        @Override
        public OctTreeBlocks deserialize(byte[] ar) {
            // 7 * 4 = width, length, height, minX, minY, minZ, maxSize as bytes
            //the palette takes 256*3 bytes
            //it takes 1 byte to represent a block
            int[] ints = readInts(ar, 0, 7);
            int width, length, height, minX, minY, minZ, maxSize;

            //the metadata
            width = ints[0];
            length = ints[1];
            height = ints[2];
            minX = ints[3];
            minY = ints[4];
            minZ = ints[5];
            maxSize = ints[6];
            OctTreeBlocks blocks = new OctTreeBlocks(width, length, height, minX, minY, minZ, maxSize);
            int i = 7 * 4;
            //the pallet
            for (int palletIndex = 0; palletIndex < blocks.pallet.length; palletIndex++) {
                byte id = ar[i];
                byte data = ar[i+1];
                Classification classification = Classification.values()[ar[i+2]];
                blocks.pallet[palletIndex] = new Block(id, data, classification);
                i+=3;
            }
            //the blocks
            int finalI = i;
            blocks.blocks.forEachSet((x, y, z) -> {
                int index = (z * blocks.length + y) * blocks.width + x;
                index += finalI;
                return ar[index];
            });
            return blocks;
        }
    }
}
