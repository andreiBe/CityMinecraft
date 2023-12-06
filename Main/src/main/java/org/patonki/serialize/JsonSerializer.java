package org.patonki.serialize;

import com.google.gson.Gson;
import org.patonki.citygml.citygml.GmlOptions;
import org.patonki.color.ColorToBlockConverterOptions;
import org.patonki.data.Block;
import org.patonki.data.BoundingBox;
import org.patonki.data.Classification;
import org.patonki.data.IntBoundingBox;
import org.patonki.downloader.Downloader;
import org.patonki.groundcolor.GroundColorSettings;
import org.patonki.openstreetmap.FeatureFilterer;
import org.patonki.openstreetmap.settings.LandUseInfo;
import org.patonki.openstreetmap.settings.OpenStreetMapSettings;
import org.patonki.openstreetmap.settings.RoadInfo;
import org.patonki.openstreetmap.settings.WaterwayInfo;
import org.patonki.las.settings.LasReaderSettings;
import org.patonki.types.LandUseType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.patonki.settings.Settings;
import org.patonki.types.RoadType;
import org.patonki.types.WaterWayType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import static org.patonki.color.IColorToBlockConverter.BlockEntry;
import static org.patonki.color.IColorToBlockConverter.Group;

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

    private static void generateCode(String path) {
        StringBuilder res = new StringBuilder();
        InputStream stream = JsonSerializer.class.getResourceAsStream(path);

        record Block(String group, String name, int id, int data, String textureName) {
        }
        ArrayList<Block> blocks = new ArrayList<>();

        try (Scanner scanner = new Scanner(stream)) {
            String group = null;
            double groupWeight = 0;
            while (scanner.hasNextLine()) {
                String lineStr = scanner.nextLine();
                if (lineStr.isBlank()) continue;
                String[] line = lineStr.split(" ");
                if (line[0].equalsIgnoreCase("group")) {
                    group = line[1];
                    groupWeight = Double.parseDouble(line[2]);
                    res.append("Group ").append(group).append("= new Group(\"").append(group).append("\",").append(groupWeight).append(");");
                    continue;
                }
                String name = line[0];
                if (name.startsWith("#")) continue;
                String[] ids = line[1].split(":"); //the format is: id:data or just: id
                int id = Integer.parseInt(ids[0]);
                int data = ids.length > 1 ? Integer.parseInt(ids[1]) : 0;
                //if the texture name differs from the name used by minecraft, the block has a
                //third attribute that defines the texture-file name
                String textureName = line.length > 2 ? line[2] : null;
                textureName = textureName == null ? name : textureName;

                blocks.add(new Block(group, name, id, data, textureName));
            }
        }
        res.append("BlockEntry[] entries = new BlockEntry[] {");
        for (Block block : blocks) {
            res.append("new BlockEntry(").append(block.group).append(",\"")
                    .append(block.name).append("\",").append(block.id).append(",")
                    .append(block.data).append(",\"").append(block.textureName).append("\"),\n");
        }
        res.append("};");
        System.out.println(res);
    }

    public static void main(String[] args) {
        generateCode("/black_and_white.txt");
        generateCode("/ground_blocks.txt");
        generateCode("/minecraft_blocks.txt");
    }

    private static void runOptions(String templateFolder) {
        HashMap<Integer, Classification> iMap = new HashMap<>();
        iMap.put(1, Classification.UNKNOWN);
        iMap.put(2, Classification.GROUND);
        iMap.put(3, Classification.LOW_VEGETATION);
        iMap.put(4, Classification.MEDIUM_VEGETATION);
        iMap.put(5, Classification.HIGH_VEGETATION);
        iMap.put(6, Classification.BUILDING);
        iMap.put(7, Classification.LOW_POINT);
        iMap.put(8, Classification.KEY_POINT);
        iMap.put(9, Classification.WATER);
        iMap.put(10, Classification.BRIDGE);
        iMap.put(12, Classification.OVERLAP);

        HashMap<Classification, Block> blockMap = new HashMap<>();
        blockMap.put(Classification.UNKNOWN, new Block(1, 0, Classification.UNKNOWN));
        blockMap.put(Classification.GROUND, new Block(1, 5, Classification.GROUND));
        blockMap.put(Classification.LOW_VEGETATION, new Block(18, 4, Classification.LOW_VEGETATION));
        blockMap.put(Classification.MEDIUM_VEGETATION, new Block(18, 4, Classification.MEDIUM_VEGETATION));
        blockMap.put(Classification.HIGH_VEGETATION, new Block(18, 4, Classification.HIGH_VEGETATION));
        blockMap.put(Classification.BUILDING, new Block(42, 0, Classification.BUILDING));
        blockMap.put(Classification.LOW_POINT, new Block(133, 0, Classification.LOW_POINT));
        blockMap.put(Classification.KEY_POINT, new Block(133, 0, Classification.KEY_POINT));
        blockMap.put(Classification.WATER, new Block(22, 0, Classification.WATER));
        blockMap.put(Classification.BRIDGE, new Block(98, 0, Classification.BRIDGE));
        blockMap.put(Classification.OVERLAP, new Block(133, 0, Classification.OVERLAP));
        Block roofBlock = new Block(159, 9, Classification.BUILDING);

        Classification[] ignored = {Classification.OVERLAP, Classification.KEY_POINT, Classification.LOW_POINT};

        HashMap<RoadType, RoadInfo> roads = new HashMap<>();
        roads.put(RoadType.RESIDENTIAL, new RoadInfo(new Block(173, 0, Classification.GROUND), 5));
        roads.put(RoadType.SERVICE, new RoadInfo(new Block(173, 0, Classification.GROUND), 4));
        roads.put(RoadType.UNCLASSIFIED, new RoadInfo(new Block(173, 0, Classification.GROUND), 5));
        roads.put(RoadType.FOOTWAY, new RoadInfo(new Block(1, 1, Classification.GROUND), 2));
        roads.put(RoadType.PATH, new RoadInfo(new Block(3, 1, Classification.GROUND), 1));
        roads.put(RoadType.STEPS, new RoadInfo(new Block(1, 1, Classification.GROUND), 2));
        roads.put(RoadType.PEDESTRIAN, new RoadInfo(new Block(1, 1, Classification.GROUND), 2));
        roads.put(RoadType.LIVING_STREET, new RoadInfo(new Block(173, 0, Classification.GROUND), 5));
        roads.put(RoadType.CYCLEWAY, new RoadInfo(new Block(1, 1, Classification.GROUND), 2));
        roads.put(RoadType.BRIDLEWAY, new RoadInfo(new Block(1, 1, Classification.GROUND), 2));
        roads.put(RoadType.PRIMARY, new RoadInfo(new Block(173, 0, Classification.GROUND), 6));
        roads.put(RoadType.SECONDARY, new RoadInfo(new Block(173, 0, Classification.GROUND), 6));
        roads.put(RoadType.TERTIARY, new RoadInfo(new Block(173, 0, Classification.GROUND), 6));
        roads.put(RoadType.MOTORWAY, new RoadInfo(new Block(173, 0, Classification.GROUND), 10));
        roads.put(RoadType.TRUNK, new RoadInfo(new Block(173, 0, Classification.GROUND), 7));
        roads.put(RoadType.PRIMARY_LINK, new RoadInfo(new Block(159, 7, Classification.GROUND), 5));
        roads.put(RoadType.SECONDARY_LINK, new RoadInfo(new Block(159, 7, Classification.GROUND), 5));
        roads.put(RoadType.TERTIARY_LINK, new RoadInfo(new Block(159, 7, Classification.GROUND), 5));
        roads.put(RoadType.MOTORWAY_LINK, new RoadInfo(new Block(159, 7, Classification.GROUND), 5));
        roads.put(RoadType.TRUNK_LINK, new RoadInfo(new Block(159, 7, Classification.GROUND), 5));
        roads.put(RoadType.UNKNOWN, new RoadInfo(new Block(1, 1, Classification.GROUND), 1));
        roads.put(RoadType.TRACK, new RoadInfo(new Block(1, 1, Classification.GROUND), 2));
        roads.put(RoadType.TRACK_GRADE1, new RoadInfo(new Block(1, 2, Classification.GROUND), 2));
        roads.put(RoadType.TRACK_GRADE2, new RoadInfo(new Block(1, 1, Classification.GROUND), 2));
        roads.put(RoadType.TRACK_GRADE3, new RoadInfo(new Block(1, 1, Classification.GROUND), 2));
        roads.put(RoadType.TRACK_GRADE4, new RoadInfo(new Block(3, 1, Classification.GROUND), 2));
        roads.put(RoadType.TRACK_GRADE5, new RoadInfo(new Block(3, 1, Classification.GROUND), 2));

        HashMap<LandUseType, LandUseInfo> landUse = new HashMap<>();
        landUse.put(LandUseType.PARK, new LandUseInfo(new Block(2, 0, Classification.GROUND)));
        landUse.put(LandUseType.FOREST, new LandUseInfo(new Block(2, 0, Classification.GROUND)));
        landUse.put(LandUseType.MEADOW, new LandUseInfo(new Block(2, 0, Classification.GROUND)));
        landUse.put(LandUseType.GRASS, new LandUseInfo(new Block(2, 0, Classification.GROUND)));
        landUse.put(LandUseType.SCRUB, new LandUseInfo(new Block(2, 0, Classification.GROUND)));
        landUse.put(LandUseType.FARMLAND, new LandUseInfo(new Block(5, 2, Classification.GROUND)));
        landUse.put(LandUseType.RESIDENTIAL, new LandUseInfo(new Block(1, 5, Classification.GROUND)));
        landUse.put(LandUseType.INDUSTRIAL, new LandUseInfo(new Block(1, 6, Classification.GROUND)));
        landUse.put(LandUseType.FARMYARD, new LandUseInfo(new Block(3, 2, Classification.GROUND)));
        landUse.put(LandUseType.CEMETERY, new LandUseInfo(new Block(4, 0, Classification.GROUND)));
        landUse.put(LandUseType.NATURE_RESERVE, new LandUseInfo(new Block(2, 0, Classification.GROUND)));
        landUse.put(LandUseType.MILITARY, new LandUseInfo(new Block(1, 6, Classification.GROUND)));
        landUse.put(LandUseType.ALLOTMENTS, new LandUseInfo(new Block(2, 0, Classification.GROUND)));
        landUse.put(LandUseType.RETAIL, new LandUseInfo(new Block(1, 3, Classification.GROUND)));
        landUse.put(LandUseType.COMMERCIAL, new LandUseInfo(new Block(1, 4, Classification.GROUND)));
        landUse.put(LandUseType.RECREATION_GROUND, new LandUseInfo(new Block(5, 4, Classification.GROUND)));
        landUse.put(LandUseType.QUARRY, new LandUseInfo(new Block(1, 0, Classification.GROUND)));
        landUse.put(LandUseType.HEATH, new LandUseInfo(new Block(2, 0, Classification.GROUND)));
        landUse.put(LandUseType.ORCHARD, new LandUseInfo(new Block(2, 0, Classification.GROUND)));
        landUse.put(LandUseType.VINEYARD, new LandUseInfo(new Block(2, 0, Classification.GROUND)));


        HashMap<WaterWayType, WaterwayInfo> waterways = new HashMap<>();
        for (WaterWayType value : WaterWayType.values()) {
            waterways.put(value, new WaterwayInfo());
        }
        boolean useOctTree = false;

        ColorToBlockConverterOptions colorOptions = colorOptions();
        ColorToBlockConverterOptions blackAndWhiteColorOptions = blackAndWhiteColorOptions();
        ColorToBlockConverterOptions groundColorColorOptions = groundColorOptions();

        int sideLength = 500;

        LasReaderSettings lasReaderSettings = new LasReaderSettings(iMap, blockMap, roofBlock, ignored, useOctTree,sideLength);
        OpenStreetMapSettings openStreetMapSettings = new OpenStreetMapSettings(
                "EPSG:3877",
                "EPSG:4326",
                true, true, true,
                roads, landUse, waterways);

        GmlOptions gmlOptions = new GmlOptions(
                GmlOptions.TexturingType.USE_MINECRAFT_TEXTURES,
                GmlOptions.ColoringType.ALL_DIFFERENT,
                new Block[0], colorOptions, blackAndWhiteColorOptions, false);

        GroundColorSettings groundColorSettings = new GroundColorSettings(groundColorColorOptions, new Block(2,0,Classification.GROUND), new Block(1,5,Classification.GROUND));
        Settings settings = new Settings(
                lasReaderSettings,
                openStreetMapSettings,
                gmlOptions,
                groundColorSettings,3);
        saveTemplate(settings, templateFolder + "/run-options-template.json");
    }

    private static ColorToBlockConverterOptions groundColorOptions() {
        Group vegetation = new Group("vegetation", 1.0);
        Group built = new Group("built", 1.0);
        BlockEntry[] entries = new BlockEntry[]{new BlockEntry(vegetation, "grass_block", 2, 0, "grass_carried"),
                new BlockEntry(vegetation, "coarse_dirt", 2, 0, "coarse_dirt"),
                new BlockEntry(vegetation, "podzol", 2, 0, "dirt_podzol_top"),
                new BlockEntry(vegetation, "sandstone", 2, 0, "sandstone_normal"),
                new BlockEntry(vegetation, "lime_wool", 2, 0, "wool_colored_lime"),
                new BlockEntry(vegetation, "green_wool", 2, 0, "wool_colored_green"),
                new BlockEntry(vegetation, "lime_terracotta", 2, 0, "hardened_clay_stained_lime"),
                new BlockEntry(vegetation, "green_terracotta", 2, 0, "hardened_clay_stained_green"),
                new BlockEntry(vegetation, "lime_concrete", 2, 0, "concrete_lime"),
                new BlockEntry(vegetation, "green_concrete", 2, 0, "concrete_green"),
                new BlockEntry(vegetation, "minecraft:birch_planks", 2, 0, "planks_birch"),
                new BlockEntry(vegetation, "minecraft:oak_planks", 2, 0, "planks_oak"),
                new BlockEntry(built, "diorite", 1, 5, "stone_diorite"),
                new BlockEntry(built, "andesite", 1, 5, "stone_andesite"),
                new BlockEntry(built, "cobblestone", 1, 5, "cobblestone"),
                new BlockEntry(built, "smooth_stone", 1, 5, "stone_slab_top"),
        };
        return new ColorToBlockConverterOptions(entries);
    }

    private static ColorToBlockConverterOptions blackAndWhiteColorOptions() {
        Group white = new Group("white", 1.0);
        Group gray = new Group("gray", 1.0);
        Group black = new Group("black", 0.7);
        BlockEntry[] blackAndWhiteEntries = new BlockEntry[]{new BlockEntry(white, "white_wool", 35, 0, "wool_colored_white"),
                new BlockEntry(white, "smooth_quartz", 43, 7, "quartz_block_bottom"),
                new BlockEntry(white, "snow_block", 80, 0, "snow"),
                new BlockEntry(white, "chiseled_quartz_block", 155, 1, "quartz_block_chiseled"),
                new BlockEntry(white, "quartz_block", 155, 0, "quartz_block_side"),
                new BlockEntry(gray, "gray_wool", 35, 7, "wool_colored_gray"),
                new BlockEntry(gray, "light_gray_wool", 35, 8, "wool_colored_silver"),
                new BlockEntry(gray, "smooth_stone", 43, 8, "stone_slab_top"),
                new BlockEntry(gray, "stone_bricks", 98, 0, "stonebrick"),
                new BlockEntry(gray, "cyan_terracotta", 159, 9, "hardened_clay_stained_cyan"),
                new BlockEntry(gray, "gray_concrete", 251, 7, "concrete_gray"),
                new BlockEntry(gray, "light_gray_concrete", 251, 8, "concrete_silver"),
                new BlockEntry(black, "black_wool", 35, 15, "wool_colored_black"),
                new BlockEntry(black, "coal_block", 173, 0, "coal_block"),
                new BlockEntry(black, "black_concrete", 251, 15, "concrete_black"),
        };
        return new ColorToBlockConverterOptions(blackAndWhiteEntries);
    }

    private static ColorToBlockConverterOptions colorOptions() {
        Group brown = new Group("brown", 0.4);
        Group white = new Group("white", 1.0);
        Group orange = new Group("orange", 1.2);
        Group black = new Group("black", 0.6);
        Group blue = new Group("blue", 1.2);
        Group yellow = new Group("yellow", 1.0);
        Group pink = new Group("pink", 1.0);
        Group green = new Group("green", 1.0);
        Group gray = new Group("gray", 1.0);
        Group red = new Group("red", 1.0);
        BlockEntry[] entries = new BlockEntry[]{new BlockEntry(brown, "brown_wool", 35, 12, "wool_colored_brown"),
                new BlockEntry(brown, "brown_terracotta", 159, 12, "hardened_clay_stained_brown"),
                new BlockEntry(brown, "brown_concrete", 251, 12, "concrete_brown"),
                new BlockEntry(brown, "gray_terracotta", 159, 7, "hardened_clay_stained_gray"),
                new BlockEntry(brown, "black_terracotta", 159, 15, "hardened_clay_stained_black"),
                new BlockEntry(white, "birch_planks", 5, 2, "planks_birch"),
                new BlockEntry(white, "white_wool", 35, 0, "wool_colored_white"),
                new BlockEntry(white, "smooth_quartz", 43, 7, "quartz_block_bottom"),
                new BlockEntry(white, "snow_block", 80, 0, "snow"),
                new BlockEntry(white, "chiseled_quartz_block", 155, 1, "quartz_block_chiseled"),
                new BlockEntry(white, "quartz_block", 155, 0, "quartz_block_side"),
                new BlockEntry(white, "white_terracotta", 159, 0, "hardened_clay_stained_white"),
                new BlockEntry(orange, "acacia_planks", 5, 4, "planks_acacia"),
                new BlockEntry(orange, "orange_wool", 35, 1, "wool_colored_orange"),
                new BlockEntry(orange, "orange_terracotta", 159, 1, "hardened_clay_stained_orange"),
                new BlockEntry(orange, "red_sandstone", 179, 0, "red_sandstone_normal"),
                new BlockEntry(orange, "chiseled_red_sandstone", 179, 1, "red_sandstone_carved"),
                new BlockEntry(orange, "cut_red_sandstone", 179, 2, "red_sandstone_smooth"),
                new BlockEntry(orange, "white_concrete", 251, 0, "concrete_white"),
                new BlockEntry(orange, "orange_concrete", 251, 1, "concrete_orange"),
                new BlockEntry(black, "black_wool", 35, 15, "wool_colored_black"),
                new BlockEntry(black, "bedrock", 7, 0, "bedrock"),
                new BlockEntry(black, "black_terracotta", 159, 15, "hardened_clay_stained_black"),
                new BlockEntry(black, "coal_block", 173, 0, "coal_block"),
                new BlockEntry(black, "black_concrete", 251, 15, "concrete_black"),
                new BlockEntry(blue, "lapis_block", 22, 0, "lapis_block"),
                new BlockEntry(blue, "light_blue_wool", 35, 3, "wool_colored_light_blue"),
                new BlockEntry(blue, "blue_wool", 35, 11, "wool_colored_blue"),
                new BlockEntry(blue, "light_blue_terracotta", 159, 3, "hardened_clay_stained_light_blue"),
                new BlockEntry(blue, "blue_terracotta", 159, 11, "hardened_clay_stained_blue"),
                new BlockEntry(blue, "light_blue_concrete", 251, 3, "concrete_light_blue"),
                new BlockEntry(blue, "blue_concrete", 251, 11, "concrete_blue"),
                new BlockEntry(blue, "cyan_concrete", 251, 9, "concrete_cyan"),
                new BlockEntry(blue, "cyan_wool", 35, 9, "wool_colored_cyan"),
                new BlockEntry(yellow, "sandstone", 24, 0, "sandstone_normal"),
                new BlockEntry(yellow, "yellow_wool", 35, 4, "wool_colored_yellow"),
                new BlockEntry(yellow, "gold_block", 41, 0, "gold_block"),
                new BlockEntry(yellow, "yellow_terracotta", 159, 4, "hardened_clay_stained_yellow"),
                new BlockEntry(yellow, "yellow_concrete", 251, 4, "concrete_yellow"),
                new BlockEntry(pink, "magenta_wool", 35, 2, "wool_colored_magenta"),
                new BlockEntry(pink, "purple_wool", 35, 10, "wool_colored_purple"),
                new BlockEntry(pink, "magenta_terracotta", 159, 2, "hardened_clay_stained_magenta"),
                new BlockEntry(pink, "pink_terracotta", 159, 6, "hardened_clay_stained_pink"),
                new BlockEntry(pink, "purple_terracotta", 159, 10, "hardened_clay_stained_purple"),
                new BlockEntry(pink, "magenta_concrete", 251, 2, "concrete_magenta"),
                new BlockEntry(pink, "pink_concrete", 251, 6, "concrete_pink"),
                new BlockEntry(pink, "purple_concrete", 251, 10, "concrete_purple"),
                new BlockEntry(green, "lime_wool", 35, 5, "wool_colored_lime"),
                new BlockEntry(green, "green_wool", 35, 13, "wool_colored_green"),
                new BlockEntry(green, "mossy_cobblestone", 48, 0, "cobblestone_mossy"),
                new BlockEntry(green, "lime_terracotta", 159, 5, "hardened_clay_stained_lime"),
                new BlockEntry(green, "green_terracotta", 159, 13, "hardened_clay_stained_green"),
                new BlockEntry(green, "lime_concrete", 251, 5, "concrete_lime"),
                new BlockEntry(green, "green_concrete", 251, 13, "concrete_green"),
                new BlockEntry(gray, "gray_wool", 35, 7, "wool_colored_gray"),
                new BlockEntry(gray, "light_gray_wool", 35, 8, "wool_colored_silver"),
                new BlockEntry(gray, "smooth_stone", 43, 8, "stone_slab_top"),
                new BlockEntry(gray, "clay", 82, 0, "clay"),
                new BlockEntry(gray, "stone_bricks", 98, 0, "stonebrick"),
                new BlockEntry(gray, "gray_terracotta", 159, 7, "hardened_clay_stained_gray"),
                new BlockEntry(gray, "light_gray_terracotta", 159, 8, "hardened_clay_stained_silver"),
                new BlockEntry(gray, "cyan_terracotta", 159, 9, "hardened_clay_stained_cyan"),
                new BlockEntry(gray, "gray_concrete", 251, 7, "concrete_gray"),
                new BlockEntry(gray, "light_gray_concrete", 251, 8, "concrete_silver"),
                new BlockEntry(red, "red_wool", 35, 14, "wool_colored_red"),
                new BlockEntry(red, "bricks", 45, 0, "brick"),
                new BlockEntry(red, "red_terracotta", 159, 14, "hardened_clay_stained_red"),
                new BlockEntry(red, "terracotta", 172, 0, "hardened_clay"),
                new BlockEntry(red, "red_concrete", 251, 14, "concrete_red"),
        };
        return new ColorToBlockConverterOptions(entries);
    }

    private static void downloadOptions(String templateFolder) {
        Downloader.LasDownloadSettings las = new Downloader.LasDownloadSettings(
                "https://opaskartta.turku.fi/3d/pistepilvi/",
                500, new IntBoundingBox[] {
                        new IntBoundingBox(23449500, 6692500, 23466000, 6713000),
                        new IntBoundingBox(23460000,6713500, 	23469000,6723000),
                        new IntBoundingBox(	23465500,6723500, 	23470000,6736500)
                },
                "$X_$Y.laz"
        );
        FeatureFilterer.FeatureFiltererOptions filterOptions = new FeatureFilterer.FeatureFiltererOptions(
                "EPSG:4326", "EPSG:3877", new BoundingBox(
                23440000, 6690000, 23472000, 6739500)
        );
        Downloader.OsmDownloadSettings osm = new Downloader.OsmDownloadSettings(
                "http://download.geofabrik.de/europe/finland-latest-free.shp.zip", filterOptions
        );
        String gmlUrl = "https://opaskartta.turku.fi/TeklaOGCWeb/WFS.ashx" +
                "?service=WFS&" +
                "request=GetFeature&" +
                "typename=bldg:Building_LOD2&" +
                "bbox=$XMIN,$YMIN,$XMAX,$YMAX&" +
                "version=1.1.0&";


        Downloader.GmlDownloadSettings gml = new Downloader.GmlDownloadSettings(gmlUrl, "3.1.1", 500,
                new IntBoundingBox[] {
                        new IntBoundingBox(23449500, 6692000, 23466000, 6713000),
                        new IntBoundingBox(23460000,6713500, 	23469000,6723000),
                        new IntBoundingBox(	23465500,6723500, 	23470000,6736500)
                });
        //https://docs.geoserver.org/2.23.x/en/user/services/wms/reference.html
        String aerialUrl = "https://opaskartta.turku.fi/TeklaOGCWeb/WMS.ashx" +
                "?service=WMS&" +
                "request=GetMap&" +
                "layers=Ilmakuva%202022%20True%20ortho&" +
                "srs=EPSG:3877&" +
                "bbox=$XMIN,$YMIN,$XMAX,$YMAX&" +
                "version=1.1.1&" +
                "format=image%2Fpng&" +
                "width=$WIDTH&" +
                "height=$HEIGHT&";
        Downloader.AerialDownloadSettings aerial = new Downloader.AerialDownloadSettings(
            aerialUrl, 500,
                new IntBoundingBox[] {
                        new IntBoundingBox(23449500, 6692000, 23466000, 6713000),
                        new IntBoundingBox(23460000,6713500, 	23469000,6723000),
                        new IntBoundingBox(	23465500,6723500, 	23470000,6736500)
                }
        );
        Downloader.DownloadSettings options = new Downloader.DownloadSettings(
                las, aerial,osm, gml
        );
        saveTemplate(options, templateFolder + "/download-template.json");
    }

    public static void createTemplates(String templateFolder) {
        new File(templateFolder).mkdirs();
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
