package org.patonki.openstreetmap.settings;


import org.patonki.data.Block;

/**
 * Different land use areas have a different block covering the ground
 * For example, a residential area might have a minecraft stone block as the ground
 */
public record LandUseInfo(Block block) {
}
