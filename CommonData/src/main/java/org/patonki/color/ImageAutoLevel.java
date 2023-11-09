package org.patonki.color;


import java.util.Arrays;

public class ImageAutoLevel {
    private record MinMax(int minR, int maxR, int minG, int maxG, int minB, int maxB) {}
    private static int percentile(int[] values, double percentile) {
        int index = (int) Math.ceil(percentile * values.length);
        return values[index-1];
    }
    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
    private static MinMax getMinMax(int[][] colors) {
        int[] redChannel = new int[colors.length * colors[0].length];
        int[] greenChannel = new int[colors.length * colors[0].length];
        int[] blueChannel = new int[colors.length * colors[0].length];

        int index = 0;
        for (int[] row : colors) {
            for (int color : row) {
                Color c = Color.fromInt(color);
                redChannel[index] = c.r();
                greenChannel[index] = c.g();
                blueChannel[index] = c.b();
                index++;
            }
        }
        Arrays.sort(redChannel);
        Arrays.sort(greenChannel);
        Arrays.sort(blueChannel);
        double pr = 0.05;
        int minR = percentile(redChannel, pr);
        int maxR = percentile(redChannel, 1-pr);
        int minG = percentile(greenChannel, pr);
        int maxG = percentile(greenChannel, 1-pr);
        int minB = percentile(blueChannel, pr);
        int maxB = percentile(blueChannel, 1-pr);

        return new MinMax(minR, maxR, minG, maxG, minB, maxB);
    }
    private static int reMap(int value, int min, int max) {
        return clamp((int) ((value - min) * 255.0/(max - min)), 0, 255);
    }
    private static int getRgb(int color, MinMax minMax) {
        Color c = Color.fromInt(color);
        int r = reMap(c.r(), minMax.minR, minMax.maxR);
        int g = reMap(c.g(), minMax.minG, minMax.maxG);
        int b = reMap(c.b(), minMax.minB, minMax.maxB);
        c = new Color(r,g,b);
        return c.toInt();
    }
    //https://pippin.gimp.org/image-processing/chapter-automaticadjustments.html
    //https://stackoverflow.com/questions/1175393/white-balance-algorithm
    public static void autoLevel(int[][] colors) {
        MinMax minMax = getMinMax(colors);
        for (int y = 0; y < colors.length; y++) {
            for (int x = 0; x < colors[0].length; x++) {
                colors[y][x] = getRgb(colors[y][x], minMax);
            }
        }
    }

}
