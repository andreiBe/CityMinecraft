package org.patonki.main;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.patonki.downloader.Downloader;
import org.patonki.serialize.JsonSerializer;
import org.patonki.settings.Settings;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
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

    private static final String AERIAL_FILE_DOWNLOAD_LOCATION = INPUT_DATA_FOLDER +"/aerial";

    private static final String GML_DOWNLOAD_FOLDER = INPUT_DATA_FOLDER + "/citygml";

    private static final String TEXTURE_PACK_DOWNLOAD_FOLDER = INPUT_DATA_FOLDER;

    private static final String RESULTING_MINECRAFT_WORLD_PATH = new File("output/minecraft_world").getAbsolutePath();

    private static final String TEXTURE_PACK_FOLDER = TEXTURE_PACK_DOWNLOAD_FOLDER +"/texturePack/resource_pack/textures/blocks";
    private static final String TEMPLATE_MINECRAFT_WORLD = "inputData/templateWorld/minecraft_world";
    private static final String OUTPUT_FOLDER = "output";
    private static final String CACHE_FILE_LOCATION = OUTPUT_FOLDER + "/cache";
    private static final String MINECRAFT_SCHEMATIC_OUTPUT_PATH = OUTPUT_FOLDER +  "/schematics";



    private static void init() {
        File inputFolder = new File(INPUT_DATA_FOLDER);
        if (!inputFolder.exists()) inputFolder.mkdirs();
    }

    private static void setLogLevel(Level level) {
        Configurator.setAllLevels(LogManager.getRootLogger().getName(), level);
    }
    private static void runAll(String[] args) throws IOException{

        if (args.length == 0) {
            System.out.println("Please provide the path to the configuration json file");
            return;
        }
        String options = args[0];
        args = Arrays.copyOfRange(args, 1,args.length);

        Settings settings = JsonSerializer.deserializeFromFile(options, Settings.class);
        setLogLevel(Level.DEBUG);
        String lazFileFolder = args[0];

        args = Arrays.copyOfRange(args, 1, args.length);
        CommandLineArgs commandLineArgs = readCommandLineArguments(args);
        try {
            WorldBuilder.runAll(settings,
                    commandLineArgs.start, commandLineArgs.end, commandLineArgs.skipped,commandLineArgs.cached,
                    commandLineArgs.lasFiles,
                    lazFileFolder,
                    CACHE_FILE_LOCATION,
                    AERIAL_FILE_DOWNLOAD_LOCATION,
                    FILTERED_LAND_USE_LOCATION,
                    FILTERED_ROADS_LOCATION,
                    FILTERED_WATERWAYS_LOCATION,
                    MINECRAFT_SCHEMATIC_OUTPUT_PATH,
                    TEMPLATE_MINECRAFT_WORLD,
                    GML_DOWNLOAD_FOLDER, TEXTURE_PACK_FOLDER,
                    RESULTING_MINECRAFT_WORLD_PATH, commandLineArgs.copyToMinecraft());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private record CommandLineArgs(ExecutionStep start, ExecutionStep end, ExecutionStep[] skipped, ExecutionStep[] cached, boolean copyToMinecraft, String[] lasFiles){}
    private static CommandLineArgs readCommandLineArguments(String[] args) {
        ExecutionStep start = ExecutionStep.BEGINNING;
        ExecutionStep end = ExecutionStep.END;
        ExecutionStep[] skipped = new ExecutionStep[0];
        ExecutionStep[] cached = new ExecutionStep[0];
        String[] lasFiles = null;
        boolean copyToMinecraftWorld = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--start" -> {
                    String nextArg = args[i + 1];
                    start = ExecutionStep.valueOf(nextArg);
                    i++;
                }
                case "--end" -> {
                    String nextArg = args[i + 1];
                    end = ExecutionStep.valueOf(nextArg);
                    i++;
                }
                case "--skip" -> {
                    String nextArg = args[i + 1];
                    String[] skippedSteps = nextArg.split(",");
                    skipped = Arrays.stream(skippedSteps).map(ExecutionStep::valueOf).toArray(ExecutionStep[]::new);
                    i++;
                }
                case "--cache" -> {
                    String nextArg = args[i + 1];
                    String[] cachedSteps = nextArg.split(",");
                    cached = Arrays.stream(cachedSteps).map(ExecutionStep::valueOf).toArray(ExecutionStep[]::new);
                    i++;
                }
                case "--copy" -> copyToMinecraftWorld = true;
                case "--files" -> {
                    String nextArg = args[i+1];
                    lasFiles = nextArg.split(",");
                    i++;
                }
                default -> LOGGER.warn("Unknown cmd argument: " + arg);
            }
        }
        return new CommandLineArgs(start, end, skipped,cached, copyToMinecraftWorld, lasFiles);
    }
    private static void runTest(String[] args) throws IOException {
        LOGGER.debug("Running test");
        if (args.length == 0) {
            System.out.println("Please provide the path to the configuration json file");
            return;
        }
        String options = args[0];
        setLogLevel(Level.DEBUG);

        Settings settings = JsonSerializer.deserializeFromFile(options, Settings.class);

        String lazFile = args[0];

        args = Arrays.copyOfRange(args, 1, args.length);
        CommandLineArgs commandLineArgs = readCommandLineArguments(args);
        try {
            WorldBuilder.runSingle(settings,
                    commandLineArgs.start, commandLineArgs.end, commandLineArgs.skipped, commandLineArgs.cached,
                    lazFile,
                    CACHE_FILE_LOCATION,
                    MINECRAFT_SCHEMATIC_OUTPUT_PATH,
                    TEMPLATE_MINECRAFT_WORLD,
                    GML_DOWNLOAD_FOLDER,
                    AERIAL_FILE_DOWNLOAD_LOCATION,
                    FILTERED_LAND_USE_LOCATION,
                    FILTERED_ROADS_LOCATION,
                    FILTERED_WATERWAYS_LOCATION, TEXTURE_PACK_FOLDER,
                    RESULTING_MINECRAFT_WORLD_PATH, commandLineArgs.copyToMinecraft());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void download(String[] args) throws IOException {
        LOGGER.debug("Downloading");
        if (args.length == 0) {
            System.out.println("Please provide the path to the configuration json file");
            return;
        }
        String options = args[0];
        boolean downloadLaz = false;
        boolean downloadOsm = false;
        boolean downloadGml = false;
        boolean downloadAerial = false;
        for (String arg : args) {
            switch (arg) {
                case "-las" -> downloadLaz = true;
                case "-osm" -> downloadOsm = true;
                case "-gml" -> downloadGml = true;
                case "-aerial" -> downloadAerial = true;
            }
        }
        Downloader.DownloadSettings settings = JsonSerializer.deserializeFromFile(options, Downloader.DownloadSettings.class);
        Downloader downloader = new Downloader(settings);
        downloader.download(downloadLaz,downloadOsm,downloadGml,downloadAerial,
                LAS_FILE_DOWNLOAD_LOCATION, AERIAL_FILE_DOWNLOAD_LOCATION, SHAPE_FILE_DOWNLOAD_LOCATION,GML_DOWNLOAD_FOLDER, TEXTURE_PACK_DOWNLOAD_FOLDER, TEMPLATE_MINECRAFT_WORLD,
                UNFILTERED_SHAPEFILE_LOCATIONS, FILTERED_INPUT_DATA_FOLDER);
    }
    private static void createTemplates() {
        String folder = INPUT_DATA_FOLDER + "/templates";
        LOGGER.info("Creating templates into folder: " + folder);
        JsonSerializer.createTemplates(folder);
        LOGGER.info("Exiting...");
    }

    public static void main(String[] args) throws IOException {
        LOGGER.debug("Starting application");
        init();
        if (args.length == 0) {
            System.out.println("""
                    Please see github repo for instructions on command line arguments
                    """);
            return;
        }
        String command = args[0];
        String[] remainingArgs = Arrays.copyOfRange(args, 1,args.length);
        switch (command) {
            case "download" -> download(remainingArgs);
            case "test" -> runTest(remainingArgs);
            case "runAll" -> runAll(remainingArgs);
            case "createTemplates" -> createTemplates();
        }
    }
}
