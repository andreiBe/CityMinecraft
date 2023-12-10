package org.patonki.color;

import java.util.List;

public record ColorToBlockConverterOptions(List<IColorToBlockConverter.BlockEntry> blockEntries) {
}
