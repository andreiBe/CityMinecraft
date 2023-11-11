package org.patonki.citygml.features;

import org.patonki.citygml.math.Point2D;

import java.util.ArrayList;
import java.util.List;

public final class ImgTexture extends Texture{
    private final String imgPath;
    private Point2D[] coordinates;

    public ImgTexture(String imgPath, Point2D[] coordinates) {
        this.imgPath = imgPath;
        this.coordinates = coordinates;
    }
    public void removePoints(List<Integer> indexes) {
        ArrayList<Point2D> newCoordinates = new ArrayList<>();
        for (int i = 0; i < coordinates.length; i++) {
            Point2D coordinate = coordinates[i];
            if (!indexes.contains(i)) {
                newCoordinates.add(coordinate);
            }
        }
        this.coordinates = newCoordinates.toArray(new Point2D[0]);
    }

    public String getImgPath() {
        return imgPath;
    }

    public Point2D[] getCoordinates(int imgW, int imgH) {
        Point2D[] copy = new Point2D[coordinates.length];
        for (int i = 0; i < coordinates.length; i++) {
            Point2D p = coordinates[i];
            copy[i] = new Point2D(p.x() * imgW, imgH - 1 - p.y() * imgH);
        }
        return copy;
    }

    public Point2D[] getCoordinates() {
        return coordinates;
    }
}
