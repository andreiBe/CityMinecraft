package org.patonki.las.settings;

import org.patonki.data.Block;
import org.patonki.data.Classification;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Class used to customize the way lidar data is converted to minecraft blocks.
 */
public class LasReaderSettings {
    //classification number in the laz file mapped to the actual classification
    //for example, 2 = WATER and 3 = GROUND
    private final HashMap<Integer, Classification> classificationMapping;
    //classification mapped to the desired minecraft block
    //for example, all buildings could be iron blocks
    private final HashMap<Classification, Block> blockMapping;

    private final Block roofBlock;

    private final Classification[] ignoredClassifications;

    private final boolean useOctTree;

    private final int sideLength;

    private<K,V> HashMap<K,V> checkNotNull(HashMap<K,V> map, String name) {
        if (map == null) throw new NullPointerException(name + " cannot be null!");
        for (K key : map.keySet()) {
            if (map.get(key) == null) {
                throw new NullPointerException("Entries inside " + name +" cannot be null. Found null with key: " + key);
            }
        }
        return new HashMap<>(map);
    }
    private<T,K> HashMap<T, K> checkContainsAllEntries(HashMap<T,K> map, T[] values, String name) {
        if (map == null) throw new NullPointerException(name +" cannot be null!");
        for (T value : values) {
            K k = map.get(value);
            if (k == null) {
                throw new NullPointerException("No info for type " + value);
            }
        }
        return new HashMap<>(map);
    }

    /**
     * @param classificationMapping  Classification integer mapped to the correct classification
     * @param blockMapping           Classification mapped to the minecraft block representing it. (VEGETATION -> leaves)
     * @param roofBlock              The block that the building roofs will have
     * @param ignoredClassifications Lidar points with these classifications will be ignored
     * @param useOctTree             Whether to use the more memory efficient but slower algorithm
     * @param sideLength The length of the area in meters, for example 500m
     */
    public LasReaderSettings(HashMap<Integer, Classification> classificationMapping, HashMap<Classification, Block> blockMapping, Block roofBlock, Classification[] ignoredClassifications, boolean useOctTree, int sideLength) {
        if (ignoredClassifications == null) ignoredClassifications = new Classification[0];
        if (roofBlock == null) throw new NullPointerException("Roof block cannot be null!");
        if (roofBlock.classification() != Classification.BUILDING) throw new NullPointerException("Roof block should be classified as building!");
        this.roofBlock = roofBlock;
        this.useOctTree = useOctTree;
        this.ignoredClassifications = Arrays.copyOf(ignoredClassifications,ignoredClassifications.length);
        this.classificationMapping = checkNotNull(classificationMapping, "Classification map");
        this.blockMapping = checkContainsAllEntries(blockMapping, classificationMapping.values().toArray(Classification[]::new), "Block map");
        this.sideLength = sideLength;
    }

    /**
     * @param classificationNumber Classification as number in the las file
     * @return Matching classification
     */
    public Classification mapToClassification(int classificationNumber) {
        Classification classification = classificationMapping.get(classificationNumber);
        if (classification == null) {
            throw new IllegalArgumentException("No valid classification for number: " + classificationNumber);
        }
        return classification;
    }

    /**
     * For example, {@link Classification#WATER} -> Lapis lazuli block
     * @param classification classification
     * @return the block that matches that classification
     */
    public Block mapToBlock(Classification classification) {
        return blockMapping.get(classification);
    }

    public boolean classificationShouldBeIgnored(Classification classification) {
        return Arrays.asList(ignoredClassifications).contains(classification);
    }

    public boolean useOctTree() {
        return useOctTree;
    }

    public Block getRoofBlock() {
        return roofBlock;
    }

    public int getSideLength() {
        return sideLength;
    }
}
