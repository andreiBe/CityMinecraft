package org.patonki.openstreetmap.settings;


import org.patonki.data.Block;

/**
 * Each road type has a different block covering the ground and a different width
 */
public record RoadInfo(Block block, int width) {
}
