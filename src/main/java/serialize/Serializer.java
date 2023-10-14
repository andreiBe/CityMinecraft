package serialize;

import com.google.gson.Gson;
import data.Block;
import data.Classification;
import endpoint.settings.LandUseInfo;
import endpoint.settings.OpenStreetMapSettings;
import endpoint.settings.RoadInfo;
import endpoint.settings.WaterwayInfo;
import interfaces.settings.LasReaderSettings;
import landUse.LandUseType;
import roads.RoadType;
import settings.Settings;
import waterways.WaterWayType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

public class Serializer {
    private static final Settings testSettings;
    static {
        HashMap<Integer, Classification> iMap = new HashMap<>();
        iMap.put(1, Classification.GROUND);

        Block nullB = new Block((byte) 0, (byte) 0,Classification.UNKNOWN);
        HashMap<Classification, Block> blockMap = new HashMap<>();
        for (Classification value : Classification.values()) {
            blockMap.put(value, nullB);
        }
        Classification[] ignored = {Classification.OVERLAP};

        HashMap<RoadType, RoadInfo> roads = new HashMap<>();
        for (RoadType value : RoadType.values()) {
            roads.put(value, new RoadInfo(nullB, 0));
        }
        HashMap<LandUseType, LandUseInfo> landUse = new HashMap<>();
        for (LandUseType value : LandUseType.values()) {
            landUse.put(value, new LandUseInfo(nullB));
        }
        HashMap<WaterWayType, WaterwayInfo> waterways = new HashMap<>();
        for (WaterWayType value : WaterWayType.values()) {
            waterways.put(value, new WaterwayInfo());
        }
        testSettings = new Settings(
                new LasReaderSettings(iMap, blockMap, ignored),
                new OpenStreetMapSettings(
                        "EPSG:3877",
                        "EPSG:4326",
                        true, true, true,
                        roads, landUse, waterways)
        );
    }

    private static final Gson gson = new Gson();
    public static void main(String[] args) {
        String serialized = Serializer.serialize(testSettings);
        System.out.println(serialized);

        Settings deserialized = Serializer.deserialize(serialized, Settings.class);

        String serialized2 = Serializer.serialize(deserialized);
        System.out.println(serialized2);

    }
    public static String serialize(Object settings) {
        return gson.toJson(settings);
    }

    public static <T> T deserialize(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    public static <T> T deserializeFromFile(String path, Class<T> clazz) throws IOException {
        String json = Files.readString(Paths.get(path));
        return deserialize(json, clazz);
    }
}
