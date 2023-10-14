package endpoint.settings;

import landUse.LandUseType;
import roads.RoadType;
import waterways.WaterWayType;

import java.util.HashMap;

public class OpenStreetMapSettings {
    //authority:code for example EPSG:3877
    private final String lazDataCoordinateSystem;
    private final String osmDataCoordinateSystem;
    private final boolean addRoads, addLandUse, addWaterWays;
    private final HashMap<RoadType, RoadInfo> roadInfo;
    private final HashMap<LandUseType, LandUseInfo> landUseInfo;
    private final HashMap<WaterWayType, WaterwayInfo> waterWayInfo;

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
    public OpenStreetMapSettings(String lasDataCoordinateSystem,
                                 String osmDataCoordinateSystem,
                                 boolean addRoads,
                                 boolean addLandUse,
                                 boolean addWaterWays,
                                 HashMap<RoadType, RoadInfo> roadInfo,
                                 HashMap<LandUseType, LandUseInfo> landUseInfo,
                                 HashMap<WaterWayType, WaterwayInfo> waterWayInfo
    ) {
        if (lasDataCoordinateSystem == null)
            throw new NullPointerException("Coordinate system for las Data cannot be null!");
        if (osmDataCoordinateSystem == null) {
            throw new NullPointerException("Coordinate system for osm Data cannot be null!");
        }
        this.lazDataCoordinateSystem = lasDataCoordinateSystem;
        this.osmDataCoordinateSystem = osmDataCoordinateSystem;
        this.addRoads = addRoads;
        this.addLandUse = addLandUse;
        this.addWaterWays = addWaterWays;
        this.roadInfo = this.checkContainsAllEntries(roadInfo, RoadType.values(), "Road info");
        this.landUseInfo = this.checkContainsAllEntries(landUseInfo, LandUseType.values(), "LandUse info");
        this.waterWayInfo = this.checkContainsAllEntries(waterWayInfo, WaterWayType.values(), "WaterWay info");
    }

    public boolean isAddRoads() {
        return addRoads;
    }

    public boolean isAddLandUse() {
        return addLandUse;
    }

    public boolean isAddWaterWays() {
        return addWaterWays;
    }
    public RoadInfo getRoadInfo(RoadType type) {
        return roadInfo.get(type);
    }
    public LandUseInfo getLandUseInfo(LandUseType type) {
        return landUseInfo.get(type);
    }
    public WaterwayInfo getWaterwayInfo(WaterWayType type) {
        return waterWayInfo.get(type);
    }
    public String getOsmDataCoordinateSystem() {
        return osmDataCoordinateSystem;
    }

    public String getLazDataCoordinateSystem() {
        return lazDataCoordinateSystem;
    }
}