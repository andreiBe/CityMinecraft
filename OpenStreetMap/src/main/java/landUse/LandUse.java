package landUse;


import data.Block;
import endpoint.Feature;

import java.awt.*;

public record LandUse(Block block, Polygon polygon, LandUseType type) implements Feature {}
