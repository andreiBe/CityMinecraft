package org.patonki.groundcolor;

import org.patonki.color.ColorToBlockConverterOptions;
import org.patonki.data.Block;

public record GroundColorSettings(ColorToBlockConverterOptions colorConvert, Block vegetationBlock, Block builtBlock) {
}
