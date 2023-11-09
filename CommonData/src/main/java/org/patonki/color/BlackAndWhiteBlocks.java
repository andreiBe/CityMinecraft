package org.patonki.color;

import org.patonki.data.Block;
import org.patonki.util.Pair;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class BlackAndWhiteBlocks extends ColorBlockConverter {
    private static final int MAX_COLOR = 10000;
    private final int[] colorsFastLookup = new int[MAX_COLOR];

    public BlackAndWhiteBlocks(String texturePath, ColorToBlockConverterOptions options) throws IOException {
        super(texturePath, options, new Block[0]);
        double[] distances = new double[colorsFastLookup.length];
        for (int i = 0; i < blockEntries.size(); i++) {
            var entry = blockEntries.get(i);
            Color averageColor = entry.averageColor();
            double average = averageColor.r() / 255.0; // r,g and b are all the same so it does not matter which one we take
            for (int j = 0; j < this.colorsFastLookup.length; j++) {
                double dist = Math.abs(average - j / (double) MAX_COLOR) + 1; //1.68
                if (distances[j] == 0 || distances[j] > dist) {
                    colorsFastLookup[j] = i;
                    distances[j] = dist;
                }
            }
        }
    }
    private double rgbToBlackAndWhite(Color rgb) {
        return 0.3 * (rgb.r()/255.0) + 0.59 * (rgb.g()/255.0) + 0.11 * (rgb.b()/255.0);
    }

    @Override
    protected Color averageColorOfImage(BufferedImage image) {
        double sum = 0;
        double number = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgb = image.getRGB(x,y);

                sum += rgbToBlackAndWhite(Color.fromInt(rgb));
                number++;
            }
        }
        int averageColor = (int) (255 * sum / number);
        return new Color(averageColor, averageColor, averageColor);
    }

    public Block convert(int color) {
        int mask = ((1 << 8)-1);
        int red = (color >> 16) & mask;
        int green = (color >> 8) & mask;
        int blue = color & mask;
        Color c = new Color(red, green, blue);

        return convert(c);
    }
    private BlockEntry getBest(Color color) {
        double grayScale = rgbToBlackAndWhite(color);
        //finding the most similar block compared to the color
        return blockEntries.get(colorsFastLookup[(int) (grayScale * MAX_COLOR)]);
    }
    public Pair<Color, Block> convertBestPair(Color color) {
        var entry = getBest(color);
        int colorComponent = entry.averageColor().r();// r,g and b are all the same so it does not matter which one we take
        Color gray = new Color(colorComponent, colorComponent, colorComponent);
        return new Pair<>(gray, entry.block());
    }

    @Override
    public Pair<Group, Block> convertAndGetGroup(Color color) {
        var entry = getBest(color);
        return new Pair<>(entry.group(), entry.block());
    }

    @Override
    public Block convert(Color color) {
        return convertBestPair(color).second();
    }
}
