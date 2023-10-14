package roads;

public enum RoadType {
    RESIDENTIAL,SERVICE, UNCLASSIFIED, FOOTWAY, PATH,
    STEPS, PEDESTRIAN, LIVING_STREET, CYCLEWAY, BRIDLEWAY
    , PRIMARY, SECONDARY, TERTIARY, MOTORWAY, TRUNK
    , PRIMARY_LINK, SECONDARY_LINK, TERTIARY_LINK, MOTORWAY_LINK, TRUNK_LINK
    , UNKNOWN
    , TRACK, TRACK_GRADE1, TRACK_GRADE2, TRACK_GRADE3, TRACK_GRADE4, TRACK_GRADE5
    ;


    public static RoadType matchingType(String s) {
        s = s.toUpperCase().replace('-','_');
        for (RoadType value : RoadType.values()) {
            if (value.name().equals(s)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Not supported road-type: " + s);
    }
}
