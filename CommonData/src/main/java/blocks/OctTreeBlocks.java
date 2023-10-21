package blocks;

import blocks.nodes.Node;
import blocks.nodes.ParentNode;
import data.Block;
import data.BlockSerializer;
import data.Classification;

import java.util.*;

public class OctTreeBlocks extends Blocks {
    private final Node blocks;
    private final HashMap<Block, Byte> ids = new HashMap<>();
    private final Block[] pallet = new Block[256];

    public OctTreeBlocks(int width, int length, int height, int minX, int minY, int minZ, int maxSize) {
        super(width, length, height, minX, minY, minZ);
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
    public Item[][] getGroundLayerIncomplete() {
        Item[][] ar = new Item[getWidth()][getLength()];
        this.blocks.groundLayer(ar, converter);
        return ar;
    }


    @Override
    public void forEach(BlockAction action) {
        this.blocks.forEach((x, y, z, b) -> {
            action.run(x,y,z, pallet[b]);
        });
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
    public Iterator<Item> iterator() {
        return new XYZIterator();
    }

    public class XYZIterator implements Iterator<Item> {
        private final Item item;
        private final Iterator<Node.ByteItem> iterator;

        public XYZIterator() {
            this.iterator = blocks.iterator();
            this.item = new Item(0,0,0, null);
        }
        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Item next() {
            if (!iterator.hasNext())
                throw new NoSuchElementException();
            Node.ByteItem next = iterator.next();
            this.item.x = next.x;
            this.item.y = next.y;
            this.item.z = next.z;
            this.item.block = pallet[next.block];
            return this.item;
        }
    }

    public static class OctTreeBlocksSerializer extends BlockSerializer {

        @Override
        public byte[] serialize(Blocks blocksUnknown) {
            OctTreeBlocks blocks = (OctTreeBlocks) blocksUnknown;
            return new byte[0];
        }

        @Override
        public Blocks deserialize(byte[] ar) {
            return null;
        }
    }
}
