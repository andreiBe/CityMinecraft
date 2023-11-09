package org.patonki.main;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.spi.LoggerContext;
import org.patonki.downloader.Downloader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
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
    private static void runAll(String options, String[] args) throws IOException{
        Settings settings = JsonSerializer.deserializeFromFile(options, Settings.class);
        setLogLevel(Level.DEBUG);
        String lazFileFolder = args[0];

        CommandLineArgs commandLineArgs = readCommandLineArguments(args);
        try {
            WorldBuilder.runAll(settings,
                    commandLineArgs.start, commandLineArgs.end, commandLineArgs.skipped,
                    lazFileFolder,
                    CACHE_FILE_LOCATION,
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
    private record CommandLineArgs(ExecutionStep start, ExecutionStep end, ExecutionStep[] skipped, boolean copyToMinecraft){}
    private static CommandLineArgs readCommandLineArguments(String[] args) {
        ExecutionStep start = ExecutionStep.BEGINNING;
        ExecutionStep end = ExecutionStep.END;
        ExecutionStep[] skipped = new ExecutionStep[0];
        boolean copyToMinecraftWorld = false;
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
            if (arg.equals("--skip")) {
                String nextArg = args[i+1];
                String[] skippedSteps = nextArg.split(",");
                skipped = Arrays.stream(skippedSteps).map(ExecutionStep::valueOf).toArray(ExecutionStep[]::new);
            }
            if (arg.equals("--copy")) {
                copyToMinecraftWorld = true;
            }
        }
        return new CommandLineArgs(start, end, skipped, copyToMinecraftWorld);
    }
    private static void runTest(String options, String[] args) throws IOException {
        LOGGER.debug("Running test");
        setLogLevel(Level.DEBUG);

        Settings settings = JsonSerializer.deserializeFromFile(options, Settings.class);

        String lazFile = args[0];

        CommandLineArgs commandLineArgs = readCommandLineArguments(args);
        try {
            WorldBuilder.runSingle(settings,
                    commandLineArgs.start, commandLineArgs.end, commandLineArgs.skipped,
                    lazFile,
                    CACHE_FILE_LOCATION,
                    MINECRAFT_SCHEMATIC_OUTPUT_PATH,
                    TEMPLATE_MINECRAFT_WORLD,
                    GML_DOWNLOAD_FOLDER,
                    FILTERED_LAND_USE_LOCATION,
                    FILTERED_ROADS_LOCATION,
                    FILTERED_WATERWAYS_LOCATION, TEXTURE_PACK_FOLDER,
                    RESULTING_MINECRAFT_WORLD_PATH, commandLineArgs.copyToMinecraft());
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
                LAS_FILE_DOWNLOAD_LOCATION, SHAPE_FILE_DOWNLOAD_LOCATION,GML_DOWNLOAD_FOLDER, TEXTURE_PACK_DOWNLOAD_FOLDER, TEMPLATE_MINECRAFT_WORLD,
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
            case "test" -> runTest(options, remainingArgs);
            case "runAll" -> runAll(options, remainingArgs);
            case "createTemplates" -> createTemplates();
        }
    }
}
