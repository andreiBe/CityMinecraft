package org.patonki.util;

import java.util.HashMap;
import java.util.Map;

public class Counter<T> {
    private final HashMap<T, Integer> count = new HashMap<>();

    public void add(T val) {
        if (val == null) throw new NullPointerException("Cannot be null!");
        Integer number = count.get(val);
        count.put(val, number == null ? 1 : number + 1);
    }
    public T getMostCommon() {
        Map.Entry<T, Integer> mostCommon = null;
        for (Map.Entry<T, Integer> entry : count.entrySet()) {
            if (mostCommon == null || entry.getValue() > mostCommon.getValue()) {
                mostCommon = entry;
            }
        }
        if (mostCommon == null) throw new NullPointerException("No entries! ");
        return mostCommon.getKey();
    }
    public boolean isEmpty() {
        return count.isEmpty();
    }
}
