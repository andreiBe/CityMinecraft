package org.patonki.downloader;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.patonki.citygml.citygml.CityGmlDownloader;
import org.patonki.data.IntBoundingBox;
import org.patonki.openstreetmap.FeatureFilterer;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Class used to download the needed data from the internet
 */
public class Downloader {
    private final DownloadSettings settings;

    private static final Logger LOGGER = LogManager.getLogger(Downloader.class);

    public Downloader(DownloadSettings settings) {
        this.settings = settings;
    }

    public record LasDownloadSettings(String lasDataPath,
                                      int stepMeter, IntBoundingBox[] boxes, String lasFormat) {}
    public record AerialDownloadSettings(String aerialDataPath, int stepMeter, IntBoundingBox[] boxes) {}

    public record OsmDownloadSettings(String osmDataPath, FeatureFilterer.FeatureFiltererOptions filterOptions) {}

    public record GmlDownloadSettings(String gmlDataPath, String gmlVersion, int stepMeter, IntBoundingBox[] boxes) {}
    public record DownloadSettings(LasDownloadSettings lasSettings, AerialDownloadSettings aerialSettings,
                                   OsmDownloadSettings osmSettings, GmlDownloadSettings gmlSettings) {
    }
    public void download(boolean las, boolean osm, boolean gml, boolean aerial, boolean overrideParsed,
                         String lasDownloadFolder, String aerialDownloadFolder, String osmDownloadFolder, String gmlDownloadFolder,
                         String texturePackFolder, String minecraftWorldFolder,
                         String[] unfilteredShapefileLocations, String filteredInputDataPath) {
        if (osm) {
            downloadOsmData(settings.osmSettings(), osmDownloadFolder, unfilteredShapefileLocations, filteredInputDataPath);
        }
        if (las) {
            downloadLasData(settings.lasSettings(), lasDownloadFolder);
        }
        if (aerial) {
            downloadAerialData(settings.aerialSettings(), aerialDownloadFolder);
        }
        if (gml) {
            downloadGmlData(settings.gmlSettings(), gmlDownloadFolder, texturePackFolder, overrideParsed);
        }
        checkTemplateWorldExists(minecraftWorldFolder);
    }

    private void checkTemplateWorldExists(String path) {
        LOGGER.info("Making sure that the user has placed a minecraft world into the folder: " + path);
        File file = new File(path);
        if (!file.exists()) {
            String message = "User must place a minecraft world in the location: " + path;
            String message2 ="The world folder must be renamed to match the folder structure";
            LOGGER.warn(message);
            LOGGER.warn(message2);
        } else {
            LOGGER.info("Minecraft world found!");
        }
    }


    private static void downloadFileFromInternet(String path, String resultPath) {
        try (BufferedInputStream in = new BufferedInputStream(new URL(path).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(resultPath)) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("Not found " + resultPath + " from url: " + path);
        } catch (IOException e) {
            LOGGER.error(e);
            //TODO handle exception
        }
    }
    private interface DownloadArea {
        void download(int x, int y);
    }

    private void downloadAreas(IntBoundingBox[] boxes, int step, DownloadArea func) {
        for (IntBoundingBox box : boxes) {
            downloadArea(box, step, func);
        }
    }
    private void downloadArea(IntBoundingBox box, int step, DownloadArea func) {
        int smallestX = box.minX();
        int smallestY = box.minY();
        int largestX = box.maxX();
        int largestY = box.maxY();
        for (int x = smallestX; x <= largestX; x += step) {
            for (int y = smallestY; y <= largestY; y += step) {
                func.download(x,y);
            }
        }
    }
    private void createFolder(String path) {
        File folder = new File(path);
        if (!folder.exists() && folder.mkdirs()) {
            LOGGER.error("Unable to create aerial download folder at " + folder.getPath());
        }
    }
    private void downloadAerialData(AerialDownloadSettings settings, String aerialDownloadFolder) {
        createFolder(aerialDownloadFolder);

        downloadAreas(settings.boxes(), settings.stepMeter(), (x, y) -> {
            String outputPath = aerialDownloadFolder + "/" + x + "_"+y+".png";

            String url = settings.aerialDataPath()
                    .replace("$XMIN", x+"")
                    .replace("$YMIN", y+"")
                    .replace("$XMAX", (x+settings.stepMeter())+"")
                    .replace("$YMAX", (y+settings.stepMeter())+"")
                    .replace("$WIDTH", settings.stepMeter()+"")
                    .replace("$HEIGHT", settings.stepMeter()+"");

            if (new File(outputPath).exists()) {
                LOGGER.info("Aerial image downloaded from " + url + " already exists!");
                return;
            }
            LOGGER.info("Downloading aerial image: " + url);
            downloadFileFromInternet(url, outputPath);
        });
    }
    private void downloadLasData(LasDownloadSettings settings, String lasDownloadFolder) {
        createFolder(lasDownloadFolder);
        downloadAreas(settings.boxes(), settings.stepMeter(), (x, y) -> {
            String downloadFilePath = settings.lasDataPath() + "/" + settings.lasFormat()
                    .replace("$X", x + "")
                    .replace("$Y", y + "");
            String resultPath = lasDownloadFolder + "/" + x + "_" + y + ".laz";
            if (new File(resultPath).exists()) {
                LOGGER.info("Las file downloaded from " + downloadFilePath + " already exists!");
                return;
            }
            LOGGER.info("Downloading las file: " + downloadFilePath);
            downloadFileFromInternet(downloadFilePath, resultPath);
        });
    }
    private static class MojangResponse {
        public String tag_name;
    }
    private void downloadTexturePackFromMicrosoft(String texturesFolder) {
        String url = "https://api.github.com/repos/Mojang/bedrock-samples/releases/latest";
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            Gson gson = new Gson();
            MojangResponse resp = gson.fromJson(rd, MojangResponse.class);
            System.out.println(resp.tag_name);

            String zipDownloadUrl = "https://github.com/Mojang/bedrock-samples/archive/refs/tags/" + resp.tag_name + ".zip";
            this.downloadZipToFolder(texturesFolder, zipDownloadUrl);

            File parentDir = new File(texturesFolder);
            for (File file : Objects.requireNonNull(parentDir.listFiles())) {
                if (file.getName().startsWith("bedrock-samples") && file.isDirectory()) {
                    File rename = new File(file.getParent()+"/texturePack");
                    boolean success = file.renameTo(rename);
                    if (!success) {
                        LOGGER.error("Unable to rename file to " + rename.getPath());
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error while downloading texture pack");
            LOGGER.error(e);
        }
    }
    private void downloadGmlData(GmlDownloadSettings settings, String gmlDownloadFolder, String texturesFolder, boolean overrideParsed) {
        File minecraftTexturePackFolder = new File(texturesFolder);
        createFolder(texturesFolder);
        if (Objects.requireNonNull(minecraftTexturePackFolder.list()).length == 0) {
            this.downloadTexturePackFromMicrosoft(texturesFolder);
        }
        CityGmlDownloader downloader = new CityGmlDownloader(gmlDownloadFolder);

        downloadAreas(settings.boxes(), settings.stepMeter(), (x, y) -> {
            LOGGER.info("Downloading gml area " + x + "," + y + " to " +(x + settings.stepMeter()) + "," + (y + settings.stepMeter()));
            try {
                downloader.downloadAndParseGml(x, y, x+settings.stepMeter, y+settings.stepMeter, settings.gmlDataPath(), settings.gmlVersion(), overrideParsed);
            } catch (IOException e) {
                LOGGER.error("Error while downloading CityGml buildings");
                LOGGER.error("Error downloading gml area " + x + " " + y, e);
            }
        });
    }
    private void downloadZipToFolder(String downloadFolderPath, String urlPath) {
        File downloadFolder = new File(downloadFolderPath);
        createFolder(downloadFolderPath);

        try {
            URL url = new URL(urlPath);
            byte[] buffer = new byte[1024];
            try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(url.openStream()))) {
                ZipEntry zipEntry = zis.getNextEntry();
                while (zipEntry != null) {
                    File newFile = newFile(downloadFolder, zipEntry);
                    if (zipEntry.isDirectory()) {
                        if (!newFile.isDirectory() && !newFile.mkdirs()) {
                            throw new IOException("Failed to create directory " + newFile);
                        }
                    } else {
                        File parent = newFile.getParentFile();
                        if (!parent.isDirectory() && !parent.mkdirs()) {
                            throw new IOException("Failed to create directory " + parent);
                        }
                        // write file content
                        try (FileOutputStream fos = new FileOutputStream(newFile)) {
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }

                    }
                    zipEntry = zis.getNextEntry();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void downloadOsmData(OsmDownloadSettings settings, String osmDownloadFolder, String[] unfilteredShapeFileLocations, String filteredInputFolder) {
        if (new File(osmDownloadFolder).exists()) {
            LOGGER.warn("OpenStreetMap data has already been installed. If you want to reinstall, delete the folder: " + osmDownloadFolder);
        } else {
            LOGGER.info("Downloading zip from " + settings.osmDataPath + " to " + osmDownloadFolder + ". Might take some time");
            this.downloadZipToFolder(osmDownloadFolder, settings.osmDataPath);
        }

        boolean allFilesExist = true;
        for (String unfilteredShapeFileLocation : unfilteredShapeFileLocations) {
            File file = new File(unfilteredShapeFileLocation);

            file = new File(filteredInputFolder+"/" + file.getName());
            if (!file.exists()) {
                allFilesExist = false;
                break;
            }
        }
        if (allFilesExist) {
            LOGGER.warn("The OpenStreetMap data has already been filtered.");
        } else {
            FeatureFilterer featureFilterer = new FeatureFilterer(settings.filterOptions());
            try {
                featureFilterer.readMany(unfilteredShapeFileLocations, filteredInputFolder);
            } catch (IOException e) {
                LOGGER.error("IOException while downloading OpenStreetMap");
                LOGGER.error(e);
            } catch (FeatureFilterer.FilterException e) {
                LOGGER.error(e);
            }
        }
    }

    private File newFile(File destDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destDir, zipEntry.getName());

        String destDirPath = destDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }


}
