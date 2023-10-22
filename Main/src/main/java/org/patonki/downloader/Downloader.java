package org.patonki.downloader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.patonki.data.IntBoundingBox;

import java.io.*;
import java.net.URL;
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
                                      int stepMeter, IntBoundingBox box, String lasFormat) {}

    public record OsmDownloadSettings(String osmDataPath) {}
    public record DownloadSettings(LasDownloadSettings lasSettings, OsmDownloadSettings osmSettings) {
    }

    public void download(boolean las, boolean osm, boolean gml,
                         String lasDownloadFolder, String osmDownloadFolder) {
        if (osm) {
            downloadOsmData(settings.osmSettings(), osmDownloadFolder);
        }
        if (las) {
            downloadLasData(settings.lasSettings(), lasDownloadFolder);
        }
    }
    private static void downloadFile(String path, String resultPath) {
        try (BufferedInputStream in = new BufferedInputStream(new URL(path).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(resultPath)) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("Not found " + resultPath);
        } catch (IOException e) {
            e.printStackTrace();
            // handle exception
        }
    }

    private void downloadLasData(LasDownloadSettings settings, String lasDownloadFolder) {
        File dataFolder = new File(lasDownloadFolder);
        dataFolder.mkdirs();
        int smallestX = settings.box().minX();
        int smallestY = settings.box().minY();
        int largestX = settings.box().maxX();
        int largestY = settings.box().maxY();
        for (int x = smallestX; x <= largestX; x += settings.stepMeter()) {
            for (int y = smallestY; y <= largestY; y += settings.stepMeter()) {
                String downloadFilePath = settings.lasDataPath() + "/" + settings.lasFormat()
                        .replace("$X", x + "")
                        .replace("$Y", y + "");
                downloadFile(downloadFilePath, lasDownloadFolder + "/" + x + "_" + y + ".laz");
            }
        }
    }


    private void downloadOsmData(OsmDownloadSettings settings, String osmDownloadFolder) {
        File downloadFolder = new File(osmDownloadFolder);
        if (!downloadFolder.exists()) {
            downloadFolder.mkdirs();
        }

        try {
            URL url = new URL(settings.osmDataPath());
            byte[] buffer = new byte[1024];
            try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(url.openStream()))) {
                ZipEntry zipEntry = zis.getNextEntry();
                while (zipEntry != null) {
                    System.out.println(zipEntry.getName());
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
                        try (FileOutputStream fos = new FileOutputStream(newFile);) {
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

    private File newFile(File destDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destDir, zipEntry.getName());

        String destDirPath = destDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }


    private void downloadUsingStream(String urlStr, String file) throws IOException {
        URL url = new URL(urlStr);
        try (BufferedInputStream bis = new BufferedInputStream(url.openStream());
             FileOutputStream fis = new FileOutputStream(file)) {
            byte[] buffer = new byte[1024];
            int count;
            while ((count = bis.read(buffer, 0, 1024)) != -1) {
                fis.write(buffer, 0, count);
            }
        }
    }

}
