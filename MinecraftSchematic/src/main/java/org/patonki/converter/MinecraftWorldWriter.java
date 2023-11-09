package org.patonki.converter;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

public class MinecraftWorldWriter {
    private static final Logger LOGGER = LogManager.getLogger(MinecraftWorldWriter.class);
    private final String absolutePathToResultingMinecraftWorld;
    private final int minimumXCoordinateOfAllAreas;
    private final int minimumYCoordinateOfAllAreas;
    private final ReentrantLock lock = new ReentrantLock();

    private boolean pythonPackagesInstalled = false;

    public MinecraftWorldWriter(String absolutePathToResultingMinecraftWorld, int minimumXCoordinateOfAllAreas, int minimumYCoordinateOfAllAreas) {
        this.absolutePathToResultingMinecraftWorld = absolutePathToResultingMinecraftWorld;
        this.minimumXCoordinateOfAllAreas = minimumXCoordinateOfAllAreas;
        this.minimumYCoordinateOfAllAreas = minimumYCoordinateOfAllAreas;
    }

    private void installPythonPackages() {
        if (pythonPackagesInstalled) return;
        pythonPackagesInstalled = true;

        LOGGER.info("Installing python packages");

        try {
            runCommandLineCommand("pip install amulet-core");
        } catch (InterruptedException e) {
            LOGGER.error("Thread interrupted while installing python packages");
        } catch (IOException e) {
            LOGGER.error("Unable to install python packages: " + e.getMessage());
        }
    }

    private void copyDirectory(String sourceDirectoryLocation, String destinationDirectoryLocation) {
        try (Stream<Path> walk = Files.walk(Paths.get(sourceDirectoryLocation))) {
            walk.forEach(source -> {
                Path destination = Paths.get(destinationDirectoryLocation, source.toString()
                        .substring(sourceDirectoryLocation.length()));
                try {
                    Files.copy(source, destination);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Copies the result of {@link #writeSchematicToWorld(String)} to the users minecraft world folder.
     * Useful for debugging purposes
     */
    public void copyWorldToMinecraftWorldsFolder() throws IOException {
        String minecraftWorldPath = System.getProperty("user.home")  + "\\AppData\\Local\\Packages\\Microsoft.MinecraftUWP_8wekyb3d8bbwe\\LocalState\\games\\com.mojang\\minecraftWorlds\\output";
        LOGGER.info("Copying minecraft world to " + minecraftWorldPath);
        FileUtils.deleteDirectory(new File(minecraftWorldPath));
        copyDirectory(absolutePathToResultingMinecraftWorld, minecraftWorldPath);
    }

    /**
     * Copies the empty minecraft world to the folder that {@link #writeSchematicToWorld(String)} uses.
     * @param templateMinecraftWorldPath template world path
     * @throws IOException if copying fails
     */
    public void copyTemplateWorld(String templateMinecraftWorldPath) throws IOException {
        File templateMinecraftWorld = new File(templateMinecraftWorldPath);
        if (!templateMinecraftWorld.exists()) {
            throw new IOException("Template minecraft world does not exist in location: " + templateMinecraftWorld);
        }
        FileUtils.deleteDirectory(new File(absolutePathToResultingMinecraftWorld));
        copyDirectory(templateMinecraftWorldPath, absolutePathToResultingMinecraftWorld);
    }

    private void runCommandLineCommand(String command) throws InterruptedException, IOException {
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
        builder.redirectErrorStream(true);
        Process p = builder.start();
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while (true) {
            line = r.readLine();
            if (line == null) {
                break;
            }
            LOGGER.info(line);
        }
        p.waitFor(); //waiting for the python script to finish working
    }

    /**
     * Writes the schematic to the minecraft world at {@link #absolutePathToResultingMinecraftWorld}
     * @param schematicFilename path to the .schematic file
     * @throws IOException if the writing fails
     */
    public void writeSchematicToWorld(String schematicFilename) throws IOException {
        LOGGER.debug("Thread " + Thread.currentThread().getName() + " waiting to write to minecraft world");
        lock.lock(); //prevent multiple threads from calling this method simultaneously
        installPythonPackages();
        try {
            LOGGER.debug("Thread " + Thread.currentThread().getName() + " is writing to minecraft world");
            //Calling the external python code that uses Amulet editor to import the schematic to the world
            String command = "python python/main.py"
                    + " " + schematicFilename
                    + " " + absolutePathToResultingMinecraftWorld
                    + " " + minimumXCoordinateOfAllAreas
                    + " " + minimumYCoordinateOfAllAreas;
            runCommandLineCommand(command);
        } catch (InterruptedException e) {
            LOGGER.error("Thread interrupted exception while writing the minecraft world!");
            throw new IOException(e);
        } finally {
            LOGGER.debug("Thread " + Thread.currentThread().getName() +
                    " has finished writing to the minecraft world!");
            lock.unlock();
        }
    }
}