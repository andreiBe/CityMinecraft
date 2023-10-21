package org.patonki.main;

import org.patonki.downloader.Downloader;
import endpoint.FeatureFilterer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.patonki.serialize.JsonSerializer;
import org.patonki.settings.Settings;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Main {
    private static final Logger LOGGER = LogManager.getLogger(Main.class);
    private static final String INPUT_DATA_FOLDER = "inputData";
    private static final String FILTERED_INPUT_DATA_FOLDER = INPUT_DATA_FOLDER+"/filtered";
    private static final String SHAPE_FILE_DOWNLOAD_LOCATION = INPUT_DATA_FOLDER + "/osmDataUnfiltered";

    private static final String ROADS_FILE_NAME = "gis_osm_roads_free_1.shp";
    private static final String LAND_USE_FILE_NAME = "gis_osm_landuse_a_free_1.shp";
    private static final String WATERWAYS_FILE_NAME = "gis_osm_waterways_free_1.shp";
    private static final String[] UNFILTERED_SHAPEFILE_LOCATIONS = new String[] {
            SHAPE_FILE_DOWNLOAD_LOCATION+"/" + ROADS_FILE_NAME,
            SHAPE_FILE_DOWNLOAD_LOCATION+"/" + LAND_USE_FILE_NAME,
            SHAPE_FILE_DOWNLOAD_LOCATION+"/" + WATERWAYS_FILE_NAME
    };
    private static final String FILTERED_ROADS_LOCATION = FILTERED_INPUT_DATA_FOLDER+"/" + ROADS_FILE_NAME;
    private static final String FILTERED_LAND_USE_LOCATION = FILTERED_INPUT_DATA_FOLDER+"/" + LAND_USE_FILE_NAME;
    private static final String FILTERED_WATERWAYS_LOCATION = FILTERED_INPUT_DATA_FOLDER+"/" + WATERWAYS_FILE_NAME;
    private static final String LAS_FILE_DOWNLOAD_LOCATION = INPUT_DATA_FOLDER+"/lasFiles";

    private static final String TEMPLATE_MINECRAFT_WORLD = "inputData/templateWorld/minecraft_world";
    private static final String OUTPUT_FOLDER = "output";
    private static final String CACHE_FILE_LOCATION = OUTPUT_FOLDER + "/cache";
    private static final String MINECRAFT_SCHEMATIC_OUTPUT_PATH = OUTPUT_FOLDER +  "/schematics";


    private static void init() {
        File inputFolder = new File(INPUT_DATA_FOLDER);
        if (!inputFolder.exists()) inputFolder.mkdirs();
    }
    private static void filter(String options) throws IOException {
        LOGGER.debug("Starting to filter");
        FeatureFilterer.FeatureFiltererOptions osmOptions =
                JsonSerializer.deserializeFromFile(options, FeatureFilterer.FeatureFiltererOptions.class);

        FilterManager manager = new FilterManager(FILTERED_INPUT_DATA_FOLDER, osmOptions);
        try {
            manager.prepareData(UNFILTERED_SHAPEFILE_LOCATIONS);
        } catch (FeatureFilterer.FilterException | IOException e) {
            e.printStackTrace();
        }
    }
    private static void runAll(String options, String[] args) throws IOException{
        LOGGER.debug("Running all");

        Settings settings = JsonSerializer.deserializeFromFile(options, Settings.class);

        String lazFileFolder = args[0];

        CacheEdges cacheEdges = readCommandLineArguments(args);
        try {
            WorldBuilder.runAll(settings,
                    cacheEdges.start, cacheEdges.end,
                    lazFileFolder,
                    CACHE_FILE_LOCATION,
                    FILTERED_LAND_USE_LOCATION,
                    FILTERED_ROADS_LOCATION,
                    FILTERED_WATERWAYS_LOCATION,
                    MINECRAFT_SCHEMATIC_OUTPUT_PATH,
                    TEMPLATE_MINECRAFT_WORLD);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private record CacheEdges(ExecutionStep start, ExecutionStep end){}
    private static CacheEdges readCommandLineArguments(String[] args) {
        ExecutionStep start = ExecutionStep.BEGINNING;
        ExecutionStep end = ExecutionStep.END;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--start")) {
                String nextArg = args[i+1];
                start = ExecutionStep.valueOf(nextArg);
            }
            if (arg.equals("--end")) {
                String nextArg = args[i+1];
                end = ExecutionStep.valueOf(nextArg);
            }
        }
        return new CacheEdges(start, end);
    }
    private static void runTest(String options, String[] args) throws IOException {
        LOGGER.debug("Running test");

        Settings settings = JsonSerializer.deserializeFromFile(options, Settings.class);

        String lazFile = args[0];

        CacheEdges cacheEdges = readCommandLineArguments(args);
        try {
            WorldBuilder.runOne(settings,
                    cacheEdges.start, cacheEdges.end,
                    lazFile,
                    CACHE_FILE_LOCATION,
                    FILTERED_LAND_USE_LOCATION,
                    FILTERED_ROADS_LOCATION,
                    FILTERED_WATERWAYS_LOCATION,
                    MINECRAFT_SCHEMATIC_OUTPUT_PATH,
                    TEMPLATE_MINECRAFT_WORLD);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void download(String options, String[] args) throws IOException {
        LOGGER.debug("Downloading");
        boolean downloadLaz = false;
        boolean downloadOsm = false;
        boolean downloadGml = false;
        for (String arg : args) {
            switch (arg) {
                case "-las" -> downloadLaz = true;
                case "-osm" -> downloadOsm = true;
                case "-gml" -> downloadGml = true;
            }
        }
        Downloader.DownloadSettings settings = JsonSerializer.deserializeFromFile(options, Downloader.DownloadSettings.class);
        Downloader downloader = new Downloader(settings);
        downloader.download(downloadLaz,downloadOsm,downloadGml,
                LAS_FILE_DOWNLOAD_LOCATION, SHAPE_FILE_DOWNLOAD_LOCATION);
    }
    private static void createTemplates() {
        JsonSerializer.createTemplates(INPUT_DATA_FOLDER+"/templates");
    }

    public static void main(String[] args) throws IOException {
        LOGGER.debug("Starting application");

        init();
        if (args.length < 2) {
            System.out.println("""
                    HELP SCREEN TODO
                    """);
            return;
        }
        String command = args[0];
        String options = args[1];
        String[] remainingArgs = Arrays.copyOfRange(args, 2,args.length);
        switch (command) {
            case "download" -> download(options, remainingArgs);
            case "filter" -> filter(options);
            case "test" -> runTest(options, remainingArgs);
            case "runAll" -> runAll(options, remainingArgs);
            case "createTemplates" -> createTemplates();
        }
    }
}