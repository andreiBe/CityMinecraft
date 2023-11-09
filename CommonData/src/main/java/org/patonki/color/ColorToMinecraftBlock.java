package org.patonki.color;


import org.patonki.data.Block;
import org.patonki.util.Pair;

import java.io.IOException;
import java.util.Arrays;

public class ColorToMinecraftBlock extends ColorBlockConverter{
    private static final int MAX_COLOR = 255;
    private static final int STEP = MAX_COLOR;
    private final byte[] colorsFastLookup = new byte[MAX_COLOR * MAX_COLOR * MAX_COLOR + MAX_COLOR * MAX_COLOR + MAX_COLOR + 1];

    private int location(int r, int g, int b) {
        return (r * MAX_COLOR * MAX_COLOR + g * MAX_COLOR + b);
    }
    private int cc(int c) {
        return (int) ((c / 255.0) * MAX_COLOR);
    }
    public ColorToMinecraftBlock(String texturePath, Block[] banned, ColorToBlockConverterOptions options) throws IOException {
        super(texturePath, options, banned);
        double[] distances = new double[colorsFastLookup.length];

        Arrays.fill(colorsFastLookup, (byte)-1);
        for (byte i = 0; i < this.blockEntries.size(); i++) {
            BlockEntry blockEntry = this.blockEntries.get(i);
            Color averageColor = blockEntry.averageColor();
            int orgR = cc(averageColor.r());
            int orgG = cc(averageColor.g());
            int orgB = cc(averageColor.b());

            int minR = Math.max(0, orgR - STEP);
            int minG = Math.max(0, orgG - STEP);
            int minB = Math.max(0, orgB - STEP);
            int maxR = Math.min(MAX_COLOR, orgR + STEP);
            int maxG = Math.min(MAX_COLOR, orgG + STEP);
            int maxB = Math.min(MAX_COLOR, orgB + STEP);

            assert minR == 0 && minG == 0 && minB == 0;
            assert maxR == MAX_COLOR && maxG == MAX_COLOR && maxB == MAX_COLOR;

            for (int r = minR; r <= maxR; r++) {
                for (int g = minG; g <= maxG; g++) {
                    for (int b = minB; b <= maxB; b++) {
                        double dist = (orgR - r) * (orgR - r) + (orgG - g) * (orgG - g) + (orgB - b) * (orgB - b) + 1;
                        dist *= (1/blockEntry.group().weight());

                        int loc = location(r, g, b);
                        if (distances[loc] == 0 || distances[loc] > dist) {
                            colorsFastLookup[loc] = i;
                            distances[loc] = dist;
                        }
                    }
                }
            }
        }
    }

    public Pair<Color, Block> convertBestPair(Color color) {
        //finding the most similar block compared to the color
        var entry = blockEntries.get(colorsFastLookup[location(cc(color.r()), cc(color.g()), cc(color.b()))]);
        return new Pair<>(entry.averageColor(), entry.block());
    }

    @Override
    public Pair<Group, Block> convertAndGetGroup(Color color) {
        var entry = blockEntries.get(colorsFastLookup[location(cc(color.r()), cc(color.g()), cc(color.b()))]);
        return new Pair<>(entry.group(), entry.block());
    }

    @Override
    public Block convert(Color color) {
        //finding the most similar block compared to the color
        return convertBestPair(color).second();
    }

}
