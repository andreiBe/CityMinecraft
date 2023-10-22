package org.patonki.data;

import java.util.HashMap;

/**
 * Classification defines the type of the block. <br>
 * These are the same classifications that the Lidar technology uses <br>
 * Here are some important classifications
 * <ul>
 *     <li>{@link Classification#UNKNOWN} -> Classification that cannot be classified</li>
 *     <li>{@link Classification#GROUND} -> asphalt, grass, stone etc.</li>
 *     <li>{@link Classification#LOW_VEGETATION} and other vegetation classifications -> tree leaves, bushes, high grass etc.</li>
 *     <li>{@link Classification#BUILDING} -> the walls and roofs off buildings</li>
 *     <li>{@link Classification#LOW_POINT} -> basically mistakes in the Lidar data (points that are way too low)</li>
 *     <li>{@link Classification#KEY_POINT} -> Points that the lidar uses for some weird purpose that I don't understand</li>
 *     <li>{@link Classification#WATER} -> water</li>
 *     <li>{@link Classification#BRIDGE} -> bridge</li>
 *     <li>{@link Classification#OVERLAP} -> Points that are caused by the Lidar laser scanning the same areas multiple times. Can be ignored</li>
 * </ul>
 */
public enum Classification {
    UNKNOWN, GROUND, LOW_VEGETATION, MEDIUM_VEGETATION, HIGH_VEGETATION, BUILDING, LOW_POINT, KEY_POINT, WATER, BRIDGE, OVERLAP;
    private static final HashMap<Classification, Integer> map = new HashMap<>();
    static {
        Classification[] ar = Classification.values();
        for (int i = 0; i < ar.length; i++) {
            map.put(ar[i], i);
        }
    }
    public static int index(Classification classification) {
        return map.get(classification);
    }

    public boolean isPlant() {
        return this == LOW_VEGETATION || this == MEDIUM_VEGETATION || this == HIGH_VEGETATION;
    }

    public static int importance(Classification classification) {
        return switch (classification) {
            case BRIDGE -> 60;
            case BUILDING -> 30;
            case WATER -> 50;
            case KEY_POINT, UNKNOWN -> 1;
            default -> 20;
        };
    }
}
