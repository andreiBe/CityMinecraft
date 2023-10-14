package interfaces.settings;

import data.Block;
import data.Classification;

import java.util.Arrays;
import java.util.HashMap;

public class LasReaderSettings {
    //classification number in the laz file mapped to the actual classification
    //for example, 2 = WATER and 3 = GROUND
    private final HashMap<Integer, Classification> classificationMapping;
    //classification mapped to the desired minecraft block
    //for example, all buildings could be iron blocks
    private final HashMap<Classification, Block> blockMapping;

    private final Classification[] ignoredClassifications;

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
    public LasReaderSettings(HashMap<Integer, Classification> classificationMapping, HashMap<Classification, Block> blockMapping, Classification[] ignoredClassifications) {
        if (ignoredClassifications == null) ignoredClassifications = new Classification[0];
        this.ignoredClassifications = Arrays.copyOf(ignoredClassifications,ignoredClassifications.length);
        this.classificationMapping = checkNotNull(classificationMapping, "Classification map");
        this.blockMapping = checkContainsAllEntries(blockMapping, classificationMapping.values().toArray(Classification[]::new), "Block map");
    }

    public Classification mapToClassification(int classificationNumber) {
        return classificationMapping.get(classificationNumber);
    }
    public Block mapToBlock(Classification classification) {
        return blockMapping.get(classification);
    }
    public boolean classificationShouldBeIgnored(Classification classification) {
        return Arrays.asList(ignoredClassifications).contains(classification);
    }
}
