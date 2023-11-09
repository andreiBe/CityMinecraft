package org.patonki.blocks.nodes;


import org.patonki.blocks.XYZBlock;
import org.patonki.data.Block;

import java.util.Iterator;

public abstract class Node implements Iterable<Node.ByteItem>{


    public abstract Iterator<ByteItem> getIterator(boolean bottomToUp);

    public interface ByteAction {
        void run(int x, int y, int z, byte b);
    }
    public interface SetByteAction {
        byte get(int x, int y, int z);
    }

    public interface BlockConverter {
        Block convert(byte b);
    }
    public static class ByteItem {
        public int x,y,z;
        public byte block;

        public ByteItem(int x, int y, int z, byte block) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
        }

    }
    protected final int minX, minY, minZ, width, length, height;

    public Node(int minX, int minY, int minZ, int width, int length, int height) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.width = width;
        this.length = length;
        this.height = height;
    }
    protected boolean outOfBounds(int x, int y, int z) {
        x-= minX;
        y -= minY;
        z -= minZ;
        return x < 0 || y < 0 || z < 0 || x >= this.width || y >= this.length || z >= this.height;
    }
    public abstract byte get(int x, int y, int z);
    public abstract boolean set(int x, int y, int z, byte block);
    public abstract int size();
    public abstract boolean groundLayer(XYZBlock[][] model, BlockConverter converter);

    public abstract void forEach(ByteAction action);
    public record BBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {}
    public abstract void forEach(ByteAction action, BBox box);

    public abstract void forEachSet(SetByteAction action);

    public boolean inside(BBox box) {
        return box.maxX() >= minX && box.maxY() >= minY && box.maxZ() >= minZ
            && box.minX() < minX+width && box.minY() < minY+length && box.minZ() < minZ + height;
    }
}