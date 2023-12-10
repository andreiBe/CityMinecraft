package org.patonki.landUse;


import org.patonki.Feature;
import org.patonki.data.Block;
import org.patonki.types.LandUseType;

import java.awt.*;

public record LandUse(Block block, Polygon polygon, LandUseType type) implements Feature {}
