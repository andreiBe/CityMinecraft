package org.patonki.citygml.citygml;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.patonki.citygml.parser.CityGmlParser;
import org.patonki.citygml.features.BuildingCollection;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import org.patonki.citygml.features.ImgTexture;
import org.patonki.citygml.features.Material;
import org.patonki.citygml.features.Texture;

public class CityGmlDownloader {
    private static final Logger LOGGER = LogManager.getLogger(CityGmlDownloader.class);
    private final String downloadFolder;

    private final Gson buildingSerializer;
    public CityGmlDownloader(String downloadFolder) {
        this.downloadFolder = downloadFolder;
        this.buildingSerializer = new GsonBuilder()
                .registerTypeAdapter(Texture.class, new TextureSerializer())
                .create();
    }
    private String getAreaDownloadFolder(int minX, int minY, int maxX, int maxY) {
        return downloadFolder + "/"+minX+" " + minY + " " + maxX + " " + maxY;
    }
    private String getImgDownloadFolder(int minX, int minY, int maxX, int maxY) {
       return getAreaDownloadFolder(minX, minY, maxX, maxY)+"/images";
    }


    private String getPathToFilteredBuildingCollection(int minX, int minY, int maxX, int maxY) {
        return getPathToDownloadedGmlFile(minX, minY, maxX, maxY).replace(".gml", ".json");
    }

    private String getPathToDownloadedGmlFile(int minX, int minY, int maxX, int maxY) {
        return getAreaDownloadFolder(minX, minY, maxX, maxY) +"/" + minX+" " + minY + " " + maxX + " " + maxY+".gml";
    }
    private void downloadFileFromWFSServer(int minX, int minY, int maxX, int maxY, String downloadUrl, String gmlVersion) throws IOException {
        String outputPath = getPathToDownloadedGmlFile(minX, minY, maxX, maxY);
        File output = new File(outputPath);
        File parent = output.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            LOGGER.warn("Unable to create folder: " + parent.getPath());
        }

        String url = downloadUrl
                .replace("$XMIN", minX+"")
                .replace("$YMIN", minY+"")
                .replace("$XMAX", maxX+"")
                .replace("$YMAX", maxY+"");

        //no need to download the same file multiple times
        if (output.exists()) {
            LOGGER.info("Not downloading file " + outputPath + " because it already exist. From url: " + url);
            return;
        }
        LOGGER.info("Downloading gml file: " + outputPath + ". From url: " + url);
        //Connecting to the WFS server to get the features
        //https://docs.geoserver.org/stable/en/user/services/wfs/reference.html#getfeature
        //https://www.google.com/url?sa=t&rct=j&q=&esrc=s&source=web&cd=&ved=2ahUKEwic1Kq2rIKBAxXgLhAIHV0UBwgQFnoECBQQAQ&url=https%3A%2F%2Fportal.ogc.org%2Ffiles%2F%3Fartifact_id%3D8339&usg=AOvVaw1OdoGKt46Zpt4JsbMboIVz&opi=89978449

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestProperty("Content-Type", "text/xml; subtype=gml/" + gmlVersion);
        con.setRequestMethod("GET");

        OutputStream outputFile = new FileOutputStream(outputPath);
        InputStream inputStream = con.getInputStream();
        byte[] res = new byte[2048];
        int i;
        while ((i = inputStream.read(res)) != -1) {
            outputFile.write(res, 0, i);
        }
        inputStream.close();
        outputFile.close();
    }
    //https://stackoverflow.com/questions/3629596/deserializing-an-abstract-class-in-gson
    private static class TextureSerializer implements JsonSerializer<Object>, JsonDeserializer<Object> {

        private static final String CLASS_META_KEY = "CLASS_META_KEY";

        private static final HashMap<String, Class<?>> classMap = new HashMap<>();
        static {
            classMap.put(ImgTexture.class.getCanonicalName(), ImgTexture.class);
            classMap.put(Material.class.getCanonicalName(), Material.class);
        }
        @Override
        public Object deserialize(JsonElement jsonElement, Type type,
                                  JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject jsonObj = jsonElement.getAsJsonObject();
            String className = jsonObj.get(CLASS_META_KEY).getAsString();
            Class<?> clz = classMap.get(className);
            if (clz == null) throw new JsonParseException("Unknown texture class: " + className);
            return jsonDeserializationContext.deserialize(jsonElement, clz);
        }

        @Override
        public JsonElement serialize(Object object, Type type, JsonSerializationContext jsonSerializationContext) {
            Gson gson = new Gson();
            gson.toJson(object, object.getClass());

            JsonElement jsonEle = gson.toJsonTree(object);
            jsonEle.getAsJsonObject().addProperty(CLASS_META_KEY,
                    object.getClass().getCanonicalName());
            return jsonEle;
        }

    }

    public BuildingCollection deserialize(int minX, int minY, int maxX, int maxY, int sideLength) throws FileNotFoundException {
        LOGGER.debug("The coordinates before fix: " + minX + " " + minY + " " + maxX + " " + maxY);
        minX -= minX % sideLength;
        minY -= minY % sideLength;
        if (maxX % sideLength > 5) maxX = maxX - maxX % sideLength + sideLength;
        else maxX -= maxX % sideLength;
        if (maxY % sideLength > 5) maxY = maxY - maxY % sideLength + sideLength;
        else maxY -= maxY % sideLength;


        String filtered = getPathToFilteredBuildingCollection(minX, minY, maxX, maxY);
        LOGGER.info("Reading gml from " + filtered);
        JsonReader jsonReader = new JsonReader(new FileReader(filtered));
        return this.buildingSerializer.fromJson(jsonReader, BuildingCollection.class);
    }
    public void downloadAndParseGml(int minX, int minY, int maxX, int maxY, String url, String gmlVersion) throws IOException {
        downloadFileFromWFSServer(minX, minY, maxX, maxY, url, gmlVersion);
        CityGmlParser parser = new CityGmlParser(getImgDownloadFolder(minX, minY, maxX, maxY));

        try {
            BuildingCollection collection = parser.readFeatures(getPathToDownloadedGmlFile(minX, minY, maxX, maxY));
            String jsonData = this.buildingSerializer.toJson(collection);

            String pathToWrite = getPathToFilteredBuildingCollection(minX, minY, maxX, maxY);
            try (FileWriter writer = new FileWriter(pathToWrite)) {
                writer.write(jsonData);
            }
            LOGGER.info("Gml file has been parsed and saved to: " + pathToWrite);
        } catch (CityGmlParser.GmlParseException e) {
            throw new IOException(e);
        }
    }
}
