package org.patonki.blocks.nodes;

import org.patonki.blocks.XYZBlock;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class LeafNode extends Node{
    private final byte[] blocks;
    public LeafNode(int minX, int minY, int minZ, int width, int length, int height) {
        super(minX, minY, minZ, width, length, height);
        this.blocks = new byte[width*length*height];
    }
    private int pos(int x, int y, int z) {
        x -= minX;
        y -= minY;
        z -= minZ;
        return localPos(x,y,z);
    }
    private int localPos(int x, int y, int z) {
        return z * (this.width * this.length) + x*(this.length) + y;
    }

    @Override
    public byte get(int x, int y, int z) {
        if (outOfBounds(x, y, z)) return 0;
        return blocks[pos(x,y,z)];
    }

    @Override
    public boolean set(int x, int y, int z, byte block) {
        if (outOfBounds(x, y, z)) return false;
        blocks[pos(x,y,z)] = block;
        return true;
    }

    @Override
    public int size() {
        return width * length * height;
    }

    @Override
    public boolean groundLayer(XYZBlock[][] groundModel, BlockConverter converter) {
        int sum = 0;
        for (int x = 0; x < this.width; x++) {
            for (int y = 0; y < this.length; y++) {
                for (int z = this.height-1; z >= 0; z--) {
                    byte block = this.blocks[localPos(x,y,z)];
                    if (block != 0) {
                        groundModel[x+minX][y+minY] = new XYZBlock(x+minX, y+minX, z+minZ, converter.convert(block));
                        break;
                    }
                }
            }
        }
        return sum == this.width * this.length;
    }

    @Override
    public void forEachSet(SetByteAction action) {
        for (int x = 0; x < this.width; x++) {
            for (int y = 0; y < this.length; y++) {
                for (int z = 0; z < this.height; z++) {
                    int pos = localPos(x,y,z);
                    if (this.blocks[pos] != 0)
                        blocks[pos] = action.get(minX + x, minY + y,minZ + z);
                }
            }
        }
    }

    @Override
    public void forEach(ByteAction action) {
        for (int x = 0; x < this.width; x++) {
            for (int y = 0; y < this.length; y++) {
                for (int z = 0; z < this.height; z++) {
                    int pos = localPos(x,y,z);
                    if (this.blocks[pos] != 0)
                        action.run(minX + x, minY + y,minZ + z,this.blocks[pos]);
                }
            }
        }
    }

    @Override
    public void forEach(ByteAction action, BBox box) {
        int endX = Math.min(this.minX + width - 1, box.maxX());
        int endY = Math.min(this.minY + length -1, box.maxY());
        int endZ = Math.min(this.minZ + height -1, box.maxZ());

        int x = Math.max(this.minX, box.minX());
        int y = Math.max(this.minY, box.minY());
        int z = Math.max(this.minZ, box.minZ());

        for (;x <= endX; x++) {
            for (;y <= endY; y++) {
                for (;z <= endZ; z++) {
                    byte block = this.blocks[pos(x,y,z)];
                    if (block != 0) action.run(x,y,z,block);
                }
            }
        }
    }

    private class BlocksIterator implements Iterator<ByteItem> {
        private int x,y,z;
        private final ByteItem item = new ByteItem(0,0,0, (byte) 0);

        private void reset() {
            this.x = -1;
            this.y = 0;
            this.z = 0;
            advance();
        }
        private void advanceCoordinates() {
            x++;
            if (x == width) {
                x = 0;y++;
                if (y == length) {
                    y = 0;z++;
                }
            }
        }
        private int pos(int x, int y, int z) {
            return z * (width * length) + x*(length) + y;
        }
        private void advance() {
            advanceCoordinates();
            while (z < height && blocks[pos(x,y,z)] == 0) {
                advanceCoordinates();
            }
        }
        @Override
        public boolean hasNext() {
            return z < height;
        }

        @Override
        public ByteItem next() {
            if (z >= height)
                throw new NoSuchElementException();
            this.item.x = x + minX;
            this.item.y = y + minY;
            this.item.z = z + minZ;
            this.item.block = blocks[pos(x,y,z)];
            advance();
            return this.item;
        }
    }
    private final BlocksIterator iterator = new BlocksIterator();
    @Override
    public Iterator<ByteItem> iterator() {
        iterator.reset();
        return iterator;
    }
}
