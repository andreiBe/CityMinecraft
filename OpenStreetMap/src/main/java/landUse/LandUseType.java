package landUse;

import java.util.Arrays;

public enum LandUseType {
    PARK, FOREST, SCRUB, MEADOW, GRASS, FARMLAND, RESIDENTIAL, INDUSTRIAL, FARMYARD,
    CEMETERY, NATURE_RESERVE, MILITARY, ALLOTMENTS, RETAIL, COMMERCIAL, RECREATION_GROUND,
    QUARRY, HEATH, ORCHARD, VINEYARD;
    public static LandUseType matchingType(String s) {
        s = s.toUpperCase().replace('-','_');
        for (LandUseType value : LandUseType.values()) {
            if (value.name().equals(s)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Not supported land-type: " + s);
    }
    private static final LandUseType[] citytypes = new LandUseType[] {RESIDENTIAL, INDUSTRIAL, MILITARY, RETAIL, COMMERCIAL, QUARRY};
    public static boolean isCity(LandUseType type) {
        return Arrays.stream(citytypes).anyMatch(l -> l == type);
    }
}
