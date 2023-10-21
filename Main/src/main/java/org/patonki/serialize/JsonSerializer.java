package org.patonki.serialize;

import com.google.gson.Gson;
import data.Block;
import data.BoundingBox;
import data.Classification;
import data.IntBoundingBox;
import org.patonki.downloader.Downloader;
import endpoint.FeatureFilterer;
import endpoint.settings.LandUseInfo;
import endpoint.settings.OpenStreetMapSettings;
import endpoint.settings.RoadInfo;
import endpoint.settings.WaterwayInfo;
import interfaces.settings.LasReaderSettings;
import types.LandUseType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.patonki.settings.Settings;
import types.RoadType;
import types.WaterWayType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

public class JsonSerializer {

    private static final Gson gson = new Gson();
    private static final Logger LOGGER = LogManager.getLogger(JsonSerializer.class);

    private static void saveTemplate(Object object, String filename) {
        String serialized = JsonSerializer.serialize(object);
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(serialized);
        } catch (IOException e) {
            LOGGER.error("Can't create template file: " + filename);
        }
    }
    private static void runOptions(String templateFolder) {
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
        boolean useOctTree = true;
        Settings options = new Settings(
                new LasReaderSettings(iMap, blockMap, ignored, useOctTree),
                new OpenStreetMapSettings(
                        "EPSG:3877",
                        "EPSG:4326",
                        true, true, true,
                        roads, landUse, waterways)
        );
        saveTemplate(options, templateFolder +"/run-options-template.json");
    }
    private static void featureFiltererOptions(String templateFolder) {
        FeatureFilterer.FeatureFiltererOptions options = new FeatureFilterer.FeatureFiltererOptions(
                "EPSG:4326", "EPSG:3877", new BoundingBox(
                23450000,6693000, 23469000,6730500)
        );

        saveTemplate(options, templateFolder+"/feature-filter-template.json");
    }
    private static void downloadOptions(String templateFolder) {
        Downloader.LasDownloadSettings las = new Downloader.LasDownloadSettings(
                "https://opaskartta.turku.fi/3d/pistepilvi/",
                500, new IntBoundingBox(23449500,6692500, 23465000, 6710000),
                "$X_$Y.laz"
        );
        Downloader.OsmDownloadSettings osm = new Downloader.OsmDownloadSettings(
                "http://download.geofabrik.de/europe/finland-latest-free.shp.zip"
        );
        Downloader.DownloadSettings options = new Downloader.DownloadSettings(
                las, osm
        );
        saveTemplate(options, templateFolder+"/download-template.json");
    }

    public static void createTemplates(String templateFolder) {
        new File(templateFolder).mkdirs();
        featureFiltererOptions(templateFolder);
        runOptions(templateFolder);
        downloadOptions(templateFolder);
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
