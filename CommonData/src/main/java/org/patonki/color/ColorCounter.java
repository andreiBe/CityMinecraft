package org.patonki.color;


import org.patonki.util.Pair;

import java.util.HashMap;
import java.util.Map;

public class ColorCounter<T> {
    private final HashMap<IColorToBlockConverter.Group, HashMap<T, Integer>> count = new HashMap<>();

    public void add(T val, IColorToBlockConverter.Group group) {
        if (val == null) throw new NullPointerException("Cannot be null!");
        var groupMap = count.computeIfAbsent(group, k -> new HashMap<>());

        Integer number = groupMap.get(val);
        groupMap.put(val, number == null ? 1 : number + 1);
    }
    private IColorToBlockConverter.Group getMostCommonGroup() {
        Pair<IColorToBlockConverter.Group, Integer> mostCommon = null;
        for (Map.Entry<IColorToBlockConverter.Group, HashMap<T, Integer>> group : count.entrySet()) {
            int total = group.getValue().values().stream().mapToInt(Integer::intValue).sum();
            total = (int) (total * group.getKey().weight());

            if (mostCommon == null || total > mostCommon.second()) {
                mostCommon = new Pair<>(group.getKey(), total);
            }
        }
        if (mostCommon == null) throw new NullPointerException("No groups!");

        return mostCommon.first();
    }
    public boolean isEmpty() {
        return count.isEmpty();
    }
    public T getMostCommon() {
        IColorToBlockConverter.Group mostCommonGroup = getMostCommonGroup();
        var groupMap = count.get(mostCommonGroup);
        Map.Entry<T, Integer> mostCommon = null;
        for (Map.Entry<T, Integer> entry : groupMap.entrySet()) {
            if (mostCommon == null || entry.getValue() > mostCommon.getValue()) {
                mostCommon = entry;
            }
        }
        if (mostCommon == null) throw new NullPointerException("No entries! ");
        return mostCommon.getKey();
    }
}
