package org.patonki.reader;

import com.github.mreutegg.laszip4j.LASHeader;
import com.github.mreutegg.laszip4j.LASPoint;
import com.github.mreutegg.laszip4j.LASReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.patonki.data.Classification;

import java.io.File;
import java.util.ArrayList;
import java.util.function.Predicate;

public class LasReader {
    private static final Logger LOGGER = LogManager.getLogger(LasReader.class);
    //if the input data contains more points classified as water than this,
    //the area must contain a large pool of water
    private static final int MINIMUM_WATER_POINTS_TO_FILL = 1000;
    private final ClassificationSupplier supplier;
    private final Predicate<Classification> classificationIgnore;

    /**
     * @param supplier Maps the file's integer representation of the classifications to the real classifications
     * @param classificationIgnore The points with these classifications will be ignored
     */
    public LasReader(ClassificationSupplier supplier, Predicate<Classification> classificationIgnore) {
        this.supplier = supplier;
        this.classificationIgnore = classificationIgnore;
    }

    public interface ClassificationSupplier {
        Classification get(int number);
    }

    //Represents a single laz point
    public static class LazPoint {
        public int x,y,z;
        public Classification classification;
        public LazPoint(int x, int y, int z, Classification classification) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.classification = classification;
        }

        @Override
        public String toString() {
            return "MyPoint{" +
                    "x=" + x +
                    ", y=" + y +
                    ", z=" + z +
                    '}';
        }
    }
    public record LazData(int minXCord, int minYCord, int minZCord, LazPoint[] points, int width, int length, int height, int waterLevel) {
    }

    private int floor(double d) {
        //if the difference is this small, a floating point error has probably occurred
        if (Math.ceil(d) - d < 0.001) {
            return (int) Math.ceil(d);
        }
        return (int) Math.floor(d);
    }

    public LazData read(String filePath) {
        LOGGER.info("Las reader starting to read!");
        long start = System.currentTimeMillis();
        LASReader reader = new LASReader(new File(filePath));
        LASHeader header = reader.getHeader();

        //finding the bounding box of the points
        int minX = Integer.MAX_VALUE; int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE; int maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE; int maxZ = Integer.MIN_VALUE;
        ArrayList<LazPoint> points = new ArrayList<>((int)header.getNumberOfPointRecords());

        for (LASPoint point : reader.getPoints()) {
            //the file contains classifications as integer, so they must be mapped to classifications
            Classification classification = this.supplier.get(point.getClassification());
            //ignoring some points
            if (classificationIgnore.test(classification)) {
                continue;
            }
            double scaledX = point.getX() * header.getXScaleFactor();
            double scaledY = point.getY() * header.getYScaleFactor();
            double scaledZ = point.getZ() * header.getZScaleFactor();

            int x = floor(scaledX);
            int y = floor(scaledY);
            int z = floor(scaledZ);
            LazPoint p = new LazPoint(x,y,z, classification);

            points.add(p);

            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);
            minZ = Math.min(minZ, p.z);
            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
            maxZ = Math.max(maxZ, p.z);
        }
        int width = maxX-minX+1;
        int length = maxY-minY+1;
        int height = maxZ-minZ+1;
        //making sure the height is an odd number so the center point can be determined easier
        if (height % 2 == 0) height++;

        int minZCord = minZ;
        int minXCord = (int) header.getMinX();
        int minYCord = (int) header.getMinY();

        long waterlevelsum = 0;
        int waterblocks = 0;
        for (LazPoint point : points) {
            //offsetting the points in order to have smaller coordinates
            //that are easier to work with
            point.x = point.x-minX;
            point.y = point.y-minY;
            point.z = point.z-minZ;

            if (point.classification == Classification.WATER) {
                waterlevelsum += point.z;
                waterblocks++;
            }
        }
        int waterLevel = waterblocks > MINIMUM_WATER_POINTS_TO_FILL ? (int)(waterlevelsum/waterblocks) : -1;
        //sorting the points so equal points will be next to each other
        points.sort((p1, p2) -> {
            if (p1.x == p2.x) {
                if (p1.y == p2.y) {
                    return Integer.compare(p1.z,p2.z);
                }
                return Integer.compare(p1.y, p2.y);
            }
            return Integer.compare(p1.x, p2.x);
        });
        long end = System.currentTimeMillis();
        LOGGER.info("Reading LAS-points finished. Time: " + (end-start) +"ms Points: " + points.size()
        + " Original number of points " + header.getNumberOfPointRecords());
        return new LazData(
                minXCord, minYCord, minZCord, points.toArray(LazPoint[]::new), width, length, height, waterLevel
        );
    }

}
