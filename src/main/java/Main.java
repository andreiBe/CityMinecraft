import endpoint.FeatureFilterer;
import serialize.Serializer;
import settings.Settings;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Main {
    private static final String INPUT_DATA_FOLDER = "inputData";
    private static final String FILTERED_INPUT_DATA_FOLDER = INPUT_DATA_FOLDER+"/filtered";
    private static final String SHAPE_FILE_DOWNLOAD_LOCATION = INPUT_DATA_FOLDER + "/osmDataUnfiltered";

    private static final String[] UNFILTERED_SHAPEFILE_LOCATIONS = new String[] {
            SHAPE_FILE_DOWNLOAD_LOCATION+"/roads",
            SHAPE_FILE_DOWNLOAD_LOCATION+"/landuse",
            SHAPE_FILE_DOWNLOAD_LOCATION+"/waterways"
    };
    private static final String FILTERED_ROADS_LOCATION = FILTERED_INPUT_DATA_FOLDER+"/roads";
    private static final String FILTERED_LAND_USE_LOCATION = FILTERED_INPUT_DATA_FOLDER+"/landuse";
    private static final String FILTERED_WATERWAYS_LOCATION = FILTERED_INPUT_DATA_FOLDER+"/waterways";


    private static void init() {
        File inputFolder = new File(INPUT_DATA_FOLDER);
        if (!inputFolder.exists()) inputFolder.mkdir();
    }
    private static void filter(String options) throws IOException {
        FeatureFilterer.FeatureFiltererOptions osmOptions =
                Serializer.deserializeFromFile(options, FeatureFilterer.FeatureFiltererOptions.class);

        FilterManager manager = new FilterManager(FILTERED_INPUT_DATA_FOLDER, osmOptions);
        try {
            manager.prepareData(UNFILTERED_SHAPEFILE_LOCATIONS);
        } catch (FeatureFilterer.FilterException | IOException e) {
            e.printStackTrace();
        }
    }
    private static void runTest(String options, String[] args) throws IOException {
        Settings settings = Serializer.deserializeFromFile(options, Settings.class);

        String lazFile = args[0];

        WorldBuilder builder = new WorldBuilder(settings);
        try {
            builder.run(lazFile,
                    FILTERED_LAND_USE_LOCATION,
                    FILTERED_ROADS_LOCATION,
                    FILTERED_WATERWAYS_LOCATION);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void download() {

    }
    public static void main(String[] args) throws IOException {
        init();
        String command = args[0];
        String options = args[1];
        String[] remainingArgs = Arrays.copyOfRange(args, 2,args.length);
        switch (command) {
            case "filter" -> filter(options);
            case "test" -> runTest(options, remainingArgs);
            case "download" -> download();
        }
    }
}
