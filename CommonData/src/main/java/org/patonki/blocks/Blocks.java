package org.patonki.blocks;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patonki.data.Block;
import org.patonki.data.Classification;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class is used to store minecraft blocks in an area defined by the width, length, height
 * minX, minY and minZ values. The class contains a collection of helper function for common queries
 * about the blocks, for example, the number of blocks with a specific classification in a subarea.<br>
 * This abstract class does NOT define how the blocks are stored.
 *<br><br>
 * Most methods have two versions. One that uses the {@link XYZBlock} class and the other with just coordinates.
 * {@link XYZBlock} is just a wrapper class for the coordinates + the block at those coordinates
 *<br><br>
 * The coordinates start from zero. One corner is (0,0,0) and another (width-1, length-1, height-1)
 */

@SuppressWarnings({"unused", "UnusedReturnValue", "BooleanMethodIsAlwaysInverted"})
public abstract class Blocks implements Iterable<XYZBlock>{
    protected final int width, length, height, minX, minY, minZ;
    private final int sideLength;

    /**
     * @param width Width of the area
     * @param length Length of the area
     * @param height Height of the area
     * @param minX The smallest x-coordinate in the area.
     * @param minY The smallest y-coordinate in the area.
     * @param minZ The smallest z-coordinate in the area.
     */
    public Blocks(int width, int length, int height, int minX, int minY, int minZ, int sideLength) {
        this.width = width;
        this.length = length;
        this.height = height;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.sideLength = sideLength;
    }

    /**
     * Returns true if the coordinate is inside the area.
     * @param x value in the range of 0-width is inside the area
     * @param y value in the range of 0-length is inside the area
     * @param z value in the range of 0-height is inside the area
     * @return if the coordinate is inside the area
     */
    public boolean inRange(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < this.width && y < this.length && z < this.height;
    }


    /**
     * Sets a block at a coordinate. The blocks should not be null! See {@link #remove(int, int, int)}
     * @param x x
     * @param y y
     * @param z z
     * @param block the block
     * @return if setting the block was successful. Returns false when out of bounds
     */
    public abstract boolean set(int x, int y, int z, @NotNull Block block);
    public boolean set(XYZBlock XYZBlock, Block block) {
        return this.set(XYZBlock.x, XYZBlock.y, XYZBlock.z, block);
    }

    /**
     * Checks if the block in the coordinates is an air block (no block present).
     * @param x x
     * @param y y
     * @param z z
     * @return whether the block is air
     */
    public boolean isAir(int x, int y, int z) {
        return get(x, y, z) == null;
    }
    public boolean isAir(XYZBlock XYZBlock) {
        return isAir(XYZBlock.x, XYZBlock.y, XYZBlock.z);
    }

    /**
     * Removes a block (sets it to air).
     * @param x x
     * @param y y
     * @param z z
     * @return whether the removal was successful. Returns false when out of bounds
     */
    public abstract boolean remove(int x, int y, int z);
    public boolean remove(XYZBlock XYZBlock) {
        return this.remove(XYZBlock.x, XYZBlock.y, XYZBlock.z);
    }

    /**
     * Gets a block in the coordinates. Returns null if there is no block (air)
     * @param x x
     * @param y y
     * @param z z
     * @return The block
     */
    @Nullable
    public abstract Block get(int x, int y, int z);
    public Block get(XYZBlock XYZBlock) {
        return this.get(XYZBlock.x, XYZBlock.y, XYZBlock.z);
    }

    /**
     * Counts the number of blocks touching the block at x,y,z.
     * Maximum value is 6 (cube has 6 sides).
     * Does not count air blocks (null blocks).
     * @param x x
     * @param y y
     * @param z z
     * @return the number of blocks
     */
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
    public int neighborsCount(XYZBlock XYZBlock) {return neighborsCount(XYZBlock.x, XYZBlock.y, XYZBlock.z);}

    /**
     * Returns the number of blocks touching the block at the coordinate x,y,z that
     * have the given classification. Maximum value is 6.
     * @param x x
     * @param y y
     * @param z z
     * @param classification classification to match
     * @return the number of blocks
     */
    public int numberOfNeighboringBlocksWithClassification(int x, int y, int z, Classification classification) {
        int sum = 0;
        if (hasClassification(x-1, y, z,classification)) sum++;
        if (hasClassification(x, y-1, z,classification)) sum++;
        if (hasClassification(x, y, z-1,classification)) sum++;
        if (hasClassification(x+1, y, z,classification)) sum++;
        if (hasClassification(x, y+1, z,classification)) sum++;
        if (hasClassification(x, y, z+1,classification)) sum++;
        return sum;
    }
    public boolean hasClassification(int x, int y, int z, Classification classification) {
        Block block = get(x,y,z);
        return block != null && block.classification() == classification;
    }
    public int numberOfNeighboringBlocksWithClassification(XYZBlock XYZBlock, Classification classification) {
        return numberOfNeighboringBlocksWithClassification(XYZBlock.x, XYZBlock.y, XYZBlock.z, classification);
    }

    /**
     * Returns the number of blocks that have the given classification in a radius.
     * For example, a radius of three checks all blocks in a 7*7*7 cube with the specified coordinate
     * as the middle.
     * @param x x
     * @param y y
     * @param z z
     * @param classification classification to match
     * @param radius the radius
     * @return number of blocks
     */
    public int numberOfBlocksWithClassificationInRadius(int x, int y, int z, Classification classification, int radius) {
        int sum = 0;
        for (int x1 = x-radius; x1 < x+radius; x1++) {
            for (int y1 = y-radius; y1 < y+radius; y1++) {
                for (int z1 = z-radius; z1 < z+radius; z1++) {
                    Block b = get(x1,y1,z1);
                    if (b != null && classification == b.classification()) sum++;
                }
            }
        }
        return sum;
    }
    public int numberOfBlocksWithClassificationInRadius(XYZBlock XYZBlock, Classification classification, int radius) {
        return numberOfBlocksWithClassificationInRadius(XYZBlock.x, XYZBlock.y, XYZBlock.z, classification, radius);
    }

    protected void addIfNotNull(List<XYZBlock> list, int x, int y, int z) {
        Block block = get(x, y, z);
        if (block != null) {
            list.add(new XYZBlock(x,y,z, block));
        }
    }

    /**
     * Returns the blocks that touch the block at x,y,z as a list
     * of {@link XYZBlock}. The list has a maximum of 6 elements.
     * @param x x
     * @param y y
     * @param z z
     * @return list containing the items
     */
    public List<XYZBlock> neighborsList(int x, int y, int z) {
        ArrayList<XYZBlock> XYZBlocks = new ArrayList<>();
        addIfNotNull(XYZBlocks, x-1, y, z);
        addIfNotNull(XYZBlocks, x+1, y, z);
        addIfNotNull(XYZBlocks, x, y-1, z);
        addIfNotNull(XYZBlocks, x, y+1, z);
        addIfNotNull(XYZBlocks, x, y, z-1);
        addIfNotNull(XYZBlocks, x, y, z+1);
        return XYZBlocks;
    }
    public List<XYZBlock> neighborsList(XYZBlock XYZBlock) {
        return this.neighborsList(XYZBlock.x, XYZBlock.y, XYZBlock.z);
    }

    /**
     * Returns the blocks that touch the block at x,y,z in the x and y directions as a list
     * of {@link XYZBlock}. The list has a maximum of 4 elements.
     * @param x x
     * @param y y
     * @param z z
     * @return list containing the items
     */
    public List<XYZBlock> xyNeighborsList(int x, int y, int z) {
        ArrayList<XYZBlock> XYZBlocks = new ArrayList<>();
        addIfNotNull(XYZBlocks, x-1, y, z);
        addIfNotNull(XYZBlocks, x+1, y, z);
        addIfNotNull(XYZBlocks, x, y-1, z);
        addIfNotNull(XYZBlocks, x, y+1, z);
        return XYZBlocks;
    }
    public List<XYZBlock> xyNeighborsList(XYZBlock XYZBlock) {
        return this.xyNeighborsList(XYZBlock.x, XYZBlock.y, XYZBlock.z);
    }


    /**
     * Returns a {@link GroundLayer} built from the data.
     * The ground layer object requires that at every (x,y) column there must
     * be a ground block (no null values). So one must make sure that this object
     * satisfies that requirement
     * @throws IllegalArgumentException If the ground layer cannot be built
     * @return GroundLayer
     */
    public GroundLayer getGroundLayer() {
        return new GroundLayer(getGroundLayerIncomplete());
    }

    private boolean isGroundBlock(Block block) {
        if (block == null) return false;
        return block.classification() == Classification.GROUND || block.classification() == Classification.WATER;
    }
    /**
     * Finds the highest ground block {@link Classification#GROUND} in each (x,y) column.
     * However, the 2d array may contain null values if the column does not have any ground blocks.
     * Use {@link #getGroundLayer()} to get a {@link GroundLayer} object that is guaranteed to
     * have a ground block in every column.
     * @return The x,y array that contains the {@link XYZBlock} representing the highest ground block
     */
    public XYZBlock[][] getGroundLayerIncomplete() {
        XYZBlock[][] ar = new XYZBlock[getWidth()][getLength()];
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getLength(); y++) {
                for (int z = getHeight()-1; z >= 0; z--) {
                    Block block = get(x,y,z);
                    if (isGroundBlock(block)) {
                        ar[x][y] = new XYZBlock(x,y,z, block);
                        break; //the highest block has been found
                    }
                }
            }
        }
        return ar;
    }

    /**
     * Run an action on a block
     */
    public interface BlockAction {
        void run(int x, int y, int z, Block block);
    }

    /**
     * Runs the action on every block in the area.
     * The blocks will not be processed in any specific order
     * @param action action
     */
    public void forEach(BlockAction action) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < length; y++) {
                for (int z = 0; z < height; z++) {
                    action.run(x,y,z, get(x,y,z));
                }
            }
        }
    }

    /**
     * Runs an action on a block and returns a new block or null if the block should be removed
     */
    public interface BlockSetAction {
        @Nullable Block run(int x, int y, int z, @Nullable Block block);
    }
    /**
     * Runs the action on every block in the area and replaces the block with whatever the action returns.
     * The blocks will not be processed in any specific order
     * @param action action
     */
    public void forEachSet(BlockSetAction action) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < length; y++) {
                for (int z = 0; z < height; z++) {
                    Block block = action.run(x,y,z, get(x,y,z));
                    if (block != null) {
                        set(x,y,z, block);
                    } else {
                        remove(x,y,z);
                    }
                }
            }
        }
    }

    /**
     * Data format for storing blocks. Same for all implementations of {@link Blocks}<br>
     * mimics the format of .schematic files.
     * @see <a href="https://minecraft.fandom.com/wiki/Schematic_file_format">Schematic format</a>
     */
    public record BlockData(byte[] blockIds, byte[] blockData,
                            short width, short length, short height,
                            int minX, int minY, int minZ) {}

    public abstract Iterator<XYZBlock> getIterator(boolean bottomToUp);
    /**
     * Returns an {@link BlockData} object that represents the data.
     * The format of the {@link BlockData} mimics the format of .schematic files
     * that some minecraft editors use.
     * @return the data
     * @see <a href="https://minecraft.fandom.com/wiki/Schematic_file_format">Schematic format</a>
     */
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

    public int getSideLength() {
        return sideLength;
    }

    /**
     * @return the number of blocks stored. Can be used to compare the memory usage of different implementations.
     */
    public abstract int size();
}
