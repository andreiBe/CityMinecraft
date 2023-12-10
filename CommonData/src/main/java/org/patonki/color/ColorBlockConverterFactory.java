package org.patonki.color;

import org.patonki.color.colorBlockConverters.BlackAndWhiteBlocks;
import org.patonki.color.colorBlockConverters.ColorToMinecraftBlock;
import org.patonki.data.Block;
import org.patonki.data.Classification;

import java.io.IOException;
import java.util.HashMap;

public class ColorBlockConverterFactory {
    private record Parameters(Classification classification, String texturesPath, ColorToBlockConverterOptions options) {

    }
    private static final HashMap<Parameters, IColorToBlockConverter> cached = new HashMap<>();

    private interface ColorBlockConverterConstructor {
        IColorToBlockConverter init(Classification classification, String texturesPath,  Block[] bannedBlocks, ColorToBlockConverterOptions options) throws IOException;
    }
    private static IColorToBlockConverter getCached(Classification classification, String texturesPath,
                                                    Block[] bannedBlocks, ColorToBlockConverterOptions options,
                                                    ColorBlockConverterConstructor constructor) throws IOException {
        var params = new Parameters(classification, texturesPath, options);
        if (cached.containsKey(params)) {
            return cached.get(params);
        }

        var result = constructor.init(classification, texturesPath,bannedBlocks,  options);
        cached.put(params, result);
        return result;
    }
    public static IColorToBlockConverter getGrayScaleConverter(Classification classification,
                                                               String texturesPath,  Block[] bannedBlocks,
                                                               ColorToBlockConverterOptions options) throws IOException {
        return getCached(classification, texturesPath, bannedBlocks, options, BlackAndWhiteBlocks::new);
    }
    public static IColorToBlockConverter getColorBlockConverter(Classification classification,
                                                                String texturesPath, Block[] bannedBlocks,
                                                                ColorToBlockConverterOptions options) throws IOException {
        return getCached(classification, texturesPath, bannedBlocks, options, ColorToMinecraftBlock::new);
    }
}
