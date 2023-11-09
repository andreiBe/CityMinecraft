package org.patonki.citygml.endpoint;

import org.patonki.color.ColorToBlockConverterOptions;
import org.patonki.data.Block;

public record GmlOptions(TexturingType texturingType,
                         ColoringType coloringType, Block[] bannedBlocks,

                         ColorToBlockConverterOptions colorConvert,

                         ColorToBlockConverterOptions blackAndWhiteColorOptions,
                         boolean debug) {
    public enum TexturingType {
        USE_MINECRAFT_TEXTURES, USE_GRAY_SCALE
    }

    public enum ColoringType {
        ALL_DIFFERENT, STRUCTURES_SAME
    }
}