package org.patonki.citygml;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.patonki.color.ColorCounter;
import org.patonki.color.ImageAutoLevel;
import org.patonki.citygml.features.ImgTexture;
import org.patonki.citygml.features.Material;
import org.patonki.citygml.features.Point;
import org.patonki.citygml.features.Polygon3D;
import org.patonki.citygml.math.Point2D;
import org.patonki.citygml.math.Vector;
import org.patonki.citygml.math.Vector2D;
import org.patonki.data.Block;
import org.patonki.data.BoundingBox;
import org.patonki.color.Color;
import org.patonki.color.IColorToBlockConverter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

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

    public Scale calculateScale(Point2D[] polygonPoints, Point2D[] texturePoints, int imageHeight, int imageWidth, boolean debug) {
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
                    if (Vector2D.fromAtoB(texturePoints[i], texturePoints[k]).len() == 0) {
                        continue;
                    }
                    break;
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
            throw new IllegalArgumentException("Scale cannot be zero: " + maxScaleX + " " + maxScaleY + " " + imageWidth + " " + imageHeight);

        return new Scale(maxScaleX, maxScaleY, angleDif, new Vector2D(texturePoints[0]), new Vector2D(polygonPoints[0]), isReversed, texturePoints);
    }

    private Block[][] createDefault(int w, int h, IColorToBlockConverter converter) {
        return createDefault(w, h, new Color(255, 255, 255), converter);
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
        int[][] colors;
        if (autoLeveled.exists()) {
            colors = convertImageTo2DArray(autoLeveled.getPath());
        } else {
            colors = convertImageTo2DArray(texturePath);
            ImageAutoLevel.autoLevel(colors);
            generateImg(colors, autoLeveled.getPath());
        }
        return colors;
    }
    private int[][] reverseYCoordinate(int[][] colors) {
        int[][] newColors = new int[colors.length][colors[0].length];
        for (int y = 0; y < colors.length; y++) {
            System.arraycopy(colors[y], 0, newColors[colors.length - 1 - y], 0, colors[y].length);
        }
        return newColors;
    }
    public Block[][] textureOfPolygon(Polygon3D polygon, IColorToBlockConverter converter, boolean wallOneColor) throws IOException {
        boolean debug = false;
        var voxelBBox = polygon.getBBox2DVoxel();
        int w = voxelBBox.w();
        int h = voxelBBox.h();
        if (polygon.getTexture() instanceof Material material) return createDefault(w, h, material.getColor(), converter);
        if (!(polygon.getTexture() instanceof ImgTexture texture)) return createDefault(w, h, converter);
        if (texture.getCoordinates().length < 3) {return createDefault(w,h, converter);}

        int[][] colors = getAutoLeveledImage(texture.getImgPath());
        var ret = new Block[w][h];

        int imageHeight = colors.length;
        int imageWidth = colors[0].length;
        Scale scale = calculateScale(polygon.getPoints2DCopy(), texture.getCoordinates(imageWidth, imageHeight), imageHeight, imageWidth, debug);


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
                        colors[textureY][textureX] = y % 2 == 0 ? 0xffff00ff : (x % 2 == 0 ? 0xffff0000: 0xff0000ff);
                    }
                }
                if (!wallOneColor)
                    ret[x][y] = counter.getMostCommon();
            }
        }

        return wallOneColor ? createDefault(w,h, counterAll.getMostCommon()) : ret;
    }

    //stolen from https://stackoverflow.com/a/17175454
    public static int[][] convertImageTo2DArray(String imagePath) throws IOException {
        BufferedImage image = imagePath.startsWith("/") ? ImageIO.read(Objects.requireNonNull(TextureReader.class.getResourceAsStream(imagePath)))
                : ImageIO.read(new File(imagePath));

        final byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        final int width = image.getWidth();
        final int height = image.getHeight();
        final boolean hasAlphaChannel = image.getAlphaRaster() != null;

        int[][] result = new int[height][width];
        if (hasAlphaChannel) {
            final int pixelLength = 4;
            for (int pixel = 0, row = 0, col = 0; pixel + 3 < pixels.length; pixel += pixelLength) {
                int argb = 0;
                argb += (((int) pixels[pixel] & 0xff) << 24); // alpha
                argb += ((int) pixels[pixel + 1] & 0xff); // blue
                argb += (((int) pixels[pixel + 2] & 0xff) << 8); // green
                argb += (((int) pixels[pixel + 3] & 0xff) << 16); // red
                result[row][col] = argb;
                col++;
                if (col == width) {
                    col = 0;
                    row++;
                }
            }
        } else {
            final int pixelLength = 3;
            for (int pixel = 0, row = 0, col = 0; pixel + 2 < pixels.length; pixel += pixelLength) {
                int argb = 0;
                argb -= 16777216; // 255 alpha
                argb += ((int) pixels[pixel] & 0xff); // blue
                argb += (((int) pixels[pixel + 1] & 0xff) << 8); // green
                argb += (((int) pixels[pixel + 2] & 0xff) << 16); // red
                result[row][col] = argb;
                col++;
                if (col == width) {
                    col = 0;
                    row++;
                }
            }
        }

        return result;
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
            ImageIO.write(img, path.substring(path.lastIndexOf('.')+1), new File(path));
        } catch (IOException e) {
            System.out.println("FAILED " + path);
            e.printStackTrace();
        }
    }

}
