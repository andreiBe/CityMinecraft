package org.patonki.citygml;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.patonki.citygml.features.ImgTexture;
import org.patonki.citygml.features.Material;
import org.patonki.citygml.features.Point;
import org.patonki.citygml.features.Polygon3D;
import org.patonki.citygml.math.Point2D;
import org.patonki.citygml.math.Vector;
import org.patonki.citygml.math.Vector2D;
import org.patonki.color.Color;
import org.patonki.color.ColorCounter;
import org.patonki.color.IColorToBlockConverter;
import org.patonki.color.ImageAutoLevel;
import org.patonki.data.Block;
import org.patonki.data.BoundingBox;
import org.patonki.util.ImageUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class TextureReader {
    private static final Logger LOGGER = LogManager.getLogger(TextureReader.class);

    public record Scale(double scaleX, double scaleY, double angleDif, Vector2D textureOrigin, Vector2D polygonOrigin,
                        boolean isReversed, Point2D[] texturePoints) {
    }

    private Point to3D(Point2D p) {
        return new Point(p.x(), p.y(), 0);
    }

    private boolean isReversed(Point2D[] polygonPoints, Point2D[] texturePoints) {
        Point2D p0 = polygonPoints[0];
        Point2D p1 = polygonPoints[1];
        Point2D p2 = polygonPoints[2];
        Point2D t0 = texturePoints[0];
        Point2D t1 = texturePoints[1];
        Point2D t2 = texturePoints[2];

        Vector pv1 = Vector.fromAtoB(to3D(p0), to3D(p1));
        Vector pv2 = Vector.fromAtoB(to3D(p0), to3D(p2));
        Vector crossP = pv1.cross(pv2);

        Vector tv1 = Vector.fromAtoB(to3D(t0), to3D(t1));
        Vector tv2 = Vector.fromAtoB(to3D(t0), to3D(t2));
        Vector crossT = tv1.cross(tv2);

        return crossP.z() * crossT.z() < 0;
    }
    private static class ScaleZeroException extends Exception {
        public ScaleZeroException(String msg) {
            super(msg);
        }
    }
    public Scale calculateScale(Point2D[] polygonPoints, Point2D[] texturePoints, int imageHeight, int imageWidth, boolean debug) throws ScaleZeroException {
        assert polygonPoints.length == texturePoints.length;

        boolean isReversed = isReversed(polygonPoints, texturePoints);
        if (isReversed) {
            //reversing the y coordinate
            texturePoints = Arrays.stream(texturePoints).map(p -> new Point2D(p.x(), imageHeight - 1 - p.y())).toArray(Point2D[]::new);
        }
        double maxDistance = 0;
        double angleDif = 0;
        for (int i = 0; i < polygonPoints.length; i++) {
            for (int j = i + 1; j < polygonPoints.length; j++) {
                int k;
                for (k = 0; k < polygonPoints.length; k++) {
                    if (k == i || k == j) continue;
                    if (Vector2D.fromAtoB(texturePoints[i], texturePoints[k]).len() != 0) {
                        break;
                    }
                }
                if (k == polygonPoints.length) {
                    throw new ScaleZeroException("Can't build two vectors to calculate scale: " + Arrays.toString(polygonPoints));
                }

                Vector2D tv1 = Vector2D.fromAtoB(texturePoints[i], texturePoints[j]);
                Vector2D tv2 = Vector2D.fromAtoB(texturePoints[i], texturePoints[k]);
                Vector2D pv1 = Vector2D.fromAtoB(polygonPoints[i], polygonPoints[j]);
                Vector2D pv2 = Vector2D.fromAtoB(polygonPoints[i], polygonPoints[k]);



                if (tv1.len() > maxDistance) {
                    double angleBetweenP = pv1.angle() - pv2.angle();
                    double angleBetweenT = tv1.angle() - tv2.angle();
                    if (debug) {
                        System.out.println(tv1 + " and " + pv1 + " " + tv1.angle() + " " + pv1.angle() + " " + i + " " + j + " " + k + " " + (tv1.angle() - pv1.angle()));
                        System.out.println(angleBetweenP + " " + angleBetweenT);
                    }
                    angleDif = tv1.angle() - pv1.angle() + (angleBetweenP - angleBetweenT);
                    maxDistance = tv1.len();
                }
            }
        }


        double maxScaleX = 0;
        double maxScaleY = 0;
        double maxXDistance = 0;
        double maxYDistance = 0;
        for (int i = 0; i < polygonPoints.length; i++) {
            for (int j = i + 1; j < polygonPoints.length; j++) {
                Vector2D pv = Vector2D.fromAtoB(polygonPoints[i], polygonPoints[j]);
                //rotating by angleDif
                pv = Vector2D.fromAngle(pv.angle() + angleDif).multiply(pv.len());

                Vector2D fromOriginT = Vector2D.fromAtoB(texturePoints[i], texturePoints[j]);

                double scaleX = fromOriginT.x() / pv.x();
                if (!Double.isNaN(scaleX) && Math.abs(fromOriginT.x()) >= maxXDistance) {
                    maxScaleX = scaleX;
                    maxXDistance = Math.abs(fromOriginT.x());
                }
                double scaleY = fromOriginT.y() / pv.y();
                if (!Double.isNaN(scaleY) && Math.abs(fromOriginT.y()) >= maxYDistance) {
                    maxScaleY = scaleY;
                    maxYDistance = Math.abs(fromOriginT.y());
                }
            }
        }
        if (maxScaleX == 0 || maxScaleY == 0)
            throw new ScaleZeroException("Scale cannot be zero: " + maxScaleX + " " + maxScaleY + " " + imageWidth + " " + imageHeight);

        return new Scale(maxScaleX, maxScaleY, angleDif, new Vector2D(texturePoints[0]), new Vector2D(polygonPoints[0]), isReversed, texturePoints);
    }

    private Block[][] createDefault(int w, int h, IColorToBlockConverter converter) {
        return createDefault(w, h, Color.WHITE, converter);
    }

    private Block[][] createDefault(int w, int h, Color color, IColorToBlockConverter converter) {
        return createDefault(w,h, converter.convert(color));
    }
    private Block[][] createDefault(int w, int h, Block block) {
        var res = new Block[w][h];
        for (var row : res) {
            Arrays.fill(row, block);
        }
        return res;
    }
    private Vector2D getTexturePoint(Scale scale, int polygonX, int polygonY) {
        Vector2D toAdd = Vector2D.fromAtoB(scale.polygonOrigin, new Point2D(polygonX, polygonY));
        if (toAdd.len() != 0) {
            Vector2D direction = Vector2D.fromAngle(toAdd.angle() + scale.angleDif).multiply(toAdd.len());
            toAdd = new Vector2D(direction.x() * scale.scaleX(), direction.y() * scale.scaleY());
        }

        Vector2D texturePoint = scale.textureOrigin.add(toAdd);

        int textureX = (int) texturePoint.x();
        int textureY = (int) texturePoint.y();
        return new Vector2D(textureX, textureY);
    }
    private File getAutoleveledImageFile(String texturePath) {
        File textureFile = new File(texturePath);
        String textureFileName = textureFile.getName();
        File leveledFolder = new File(textureFile.getParent()+"/leveled");
        if (!leveledFolder.exists() && !leveledFolder.mkdirs()) {
            LOGGER.warn("Can't create directory: " + leveledFolder.getPath());
        }

        return new File(leveledFolder.getPath()+"/"+ textureFileName);
    }
    private int[][] getAutoLeveledImage(String texturePath) throws IOException {
        File autoLeveled = getAutoleveledImageFile(texturePath);
        if (autoLeveled.exists()) {
            return ImageUtil.convertImageTo2DArray(autoLeveled.getPath());
        } else {
            int[][] colors = ImageUtil.convertImageTo2DArray(texturePath);
            ImageAutoLevel.autoLevel(colors);
            generateImg(colors, autoLeveled.getPath());
            return colors;
        }
    }
    private int[][] reverseYCoordinate(int[][] colors) {
        int[][] newColors = new int[colors.length][colors[0].length];
        for (int y = 0; y < colors.length; y++) {
            System.arraycopy(colors[y], 0, newColors[colors.length - 1 - y], 0, colors[y].length);
        }
        return newColors;
    }
    public Block[][] textureOfPolygon(Polygon3D polygon, IColorToBlockConverter converter, boolean wallOneColor) {
        boolean debug = false;
        var voxelBBox = polygon.getBBox2DVoxel();
        int w = voxelBBox.w();
        int h = voxelBBox.h();
        if (polygon.getTexture() instanceof Material material) return createDefault(w, h, material.getColor(), converter);
        if (!(polygon.getTexture() instanceof ImgTexture texture)) return createDefault(w, h, converter);
        if (texture.getCoordinates().length < 3) {return createDefault(w,h, converter);}

        int[][] colors;
        try {
            colors = getAutoLeveledImage(texture.getImgPath());
        } catch (IOException e) {
            LOGGER.error("Error while reading image", e);
            return createDefault(w,h,converter);
        }
        var ret = new Block[w][h];

        int imageHeight = colors.length;
        int imageWidth = colors[0].length;

        Scale scale;
        try {
            scale = calculateScale(polygon.getPoints2DCopy(), texture.getCoordinates(imageWidth, imageHeight), imageHeight, imageWidth, debug);
        } catch (ScaleZeroException e) {
            LOGGER.warn(e.getMessage());
            return createDefault(w,h, converter);
        }


        if (scale.isReversed) {
            colors = reverseYCoordinate(colors);
        }

        BoundingBox bBox2D = polygon.getBBox2D();
        ColorCounter<Block> counterAll = new ColorCounter<>();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int polygonX = (int) (bBox2D.minX() + x);
                int polygonY = (int) (bBox2D.minY() + y);

                //forming a cube
                Vector2D texturePointStart = getTexturePoint(scale, polygonX, polygonY);
                Vector2D rightLower = getTexturePoint(scale, polygonX+1, polygonY);
                Vector2D leftUpper = getTexturePoint(scale, polygonX, polygonY+1);
                rightLower = Vector2D.fromAtoB(texturePointStart,rightLower);
                leftUpper = Vector2D.fromAtoB(texturePointStart,leftUpper);


                Vector2D v,u;
                u = rightLower.len() == 0 ? Vector2D.ZERO : rightLower.normalize();
                v = leftUpper.len() == 0 ? Vector2D.ZERO : leftUpper.normalize();

                ColorCounter<Block> counter = new ColorCounter<>();
                int xLimit = (int) Math.abs(scale.scaleX()) + 1;
                int yLimit = (int) Math.abs(scale.scaleY()) + 1;
                for (int ux = 0; ux < xLimit; ux++) {
                    for (int vy = 0; vy < yLimit; vy++) {
                        Vector2D texturePoint = texturePointStart
                                .add( u.multiply(ux))
                                .add( v.multiply(vy));
                        int textureY = Math.max(0, Math.min((int)texturePoint.y(), imageHeight-1));
                        int textureX = Math.max(0, Math.min((int)texturePoint.x(), imageWidth-1));
                        int color = colors[textureY][textureX];
                        var pair = converter.convertAndGetGroup(Color.fromInt(color));
                        counter.add(pair.second(), pair.first());
                        counterAll.add(pair.second(), pair.first());
                    }
                }
                if (!wallOneColor)
                    ret[x][y] = counter.isEmpty() ? converter.convert(Color.WHITE) : counter.getMostCommon();
            }
        }

        return wallOneColor ? createDefault(w,h, counterAll.getMostCommon()) : ret;
    }



    private static void generateImg(int[][] blocks, String path) {
        BufferedImage img = new BufferedImage(blocks[0].length, blocks.length, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < blocks.length; y++) {
            int[] row = blocks[y];
            for (int x = 0; x < row.length; x++) {
                img.setRGB(x, y, blocks[y][x]);
            }
        }
        try {
            String format = "png";
            boolean success = ImageIO.write(img, format, new File(path));
            if (!success) {
                LOGGER.error("Failed writing img of format " + format + " to path " + path);
            }
        } catch (IOException e) {
            System.out.println("FAILED " + path);
            LOGGER.error(e);
        }
    }

}
