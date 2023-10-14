package waterways;

public enum WaterWayType {
    RIVER,STREAM,CANAL,DRAIN;

    public static WaterWayType matchingType(String s) {
        s = s.toUpperCase().replace('-','_');
        for (WaterWayType value : WaterWayType.values()) {
            if (value.name().equals(s)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Not supported road-type: " + s);
    }
}
