package org.patonki.openstreetmap.settings;

import org.patonki.types.LandUseType;
import org.patonki.types.RoadType;
import org.patonki.types.WaterWayType;

import java.util.HashMap;

/**
 * Defines how the openStreetMap features are added to the blocks
 */
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

    /**
     * @param lasDataCoordinateSystem Format: authority:code, for example, EPSG:3877
     * @param osmDataCoordinateSystem Format: authority:code, for example, EPSG:4326 (WGS 84)
     * @param addRoads if the roads should be added
     * @param addLandUse if the land use areas should be added
     * @param addWaterWays if the waterways should be added
     * @param roadInfo maps each road type into its definitions
     * @param landUseInfo maps each land use type into its definitions
     * @param waterWayInfo map each waterway type into its definitions
     */
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