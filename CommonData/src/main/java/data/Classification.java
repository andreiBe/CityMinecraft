package data;

import java.util.HashMap;

public enum Classification {
    UNKNOWN, GROUND, LOW_VEGETATION, MEDIUM_VEGETATION, HIGH_VEGETATION, BUILDING, LOW_POINT, KEY_POINT, WATER, BRIDGE, OVERLAP, NULL;
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
