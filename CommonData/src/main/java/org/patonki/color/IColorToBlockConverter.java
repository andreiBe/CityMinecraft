package org.patonki.color;

import org.patonki.data.Block;
import org.patonki.util.Pair;

public interface IColorToBlockConverter {
    record BlockEntry(Group group, String name, int id, int data, String textureName) {}
    record Group(String name, double weight) {}
    Block convert(Color color);

    Pair<Color, Block> convertBestPair(Color color);

    Pair<Group, Block> convertAndGetGroup(Color color);
}
