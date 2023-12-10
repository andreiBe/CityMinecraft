package org.patonki.groundcolor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.patonki.blocks.Blocks;
import org.patonki.blocks.GroundLayer;
import org.patonki.blocks.XYZBlock;
import org.patonki.color.Color;
import org.patonki.color.ColorBlockConverterFactory;
import org.patonki.color.IColorToBlockConverter;
import org.patonki.data.Block;
import org.patonki.data.Classification;
import org.patonki.util.ImageUtil;

import java.io.IOException;
import java.util.List;

public class GroundColorEndpoint {
    private final GroundColorSettings settings;
    private final String texturePath;
    private final String aerialImageDownloadPath;

    private static final Logger LOGGER = LogManager.getLogger(GroundColorEndpoint.class);

    public GroundColorEndpoint(GroundColorSettings settings, String texturePath, String aerialImageDownloadPath) {
        this.settings = settings;
        this.texturePath = texturePath;
        this.aerialImageDownloadPath = aerialImageDownloadPath;
    }
    private Block getBlock(IColorToBlockConverter colorToBlockConverter, Color color) {
        //this is literally just magic. It just works (sometimes)
        int rgSum = color.r() + color.g();
        if (rgSum < 150) {
            if (color.b() > 70) { //built
                return settings.builtBlock();
            } else { //vegetation
                return settings.vegetationBlock();
            }
        }
        return colorToBlockConverter.convert(color);
    }
    public void colorGround(Blocks blocks) throws IOException {
        LOGGER.info("Starting to differentiate between vegetation and built environments");
        int minX = blocks.getMinX() - blocks.getMinX() % blocks.getSideLength();
        int minY = blocks.getMinY() - blocks.getMinY() % blocks.getSideLength();
        String imagePath = this.aerialImageDownloadPath + "/" + minX + "_"+minY+".png";
        LOGGER.info("Using aerial image from path " + imagePath);

        int[][] colors = ImageUtil.convertImageTo2DArray(imagePath);
        var colorConverter = ColorBlockConverterFactory.getColorBlockConverter(Classification.GROUND, this.texturePath, new Block[0], settings.colorConvert());


        GroundLayer ground = blocks.getGroundLayer();
        for (int x = 0; x < ground.getWidth(); x++) {
            for (int y = 0; y < ground.getLength(); y++) {
                int index = Math.max(0, colors.length - 1 - y);
                int c = colors[index][x];
                Color color = Color.fromInt(c);
                Block block = getBlock(colorConverter, color);
                for (int z = ground.getHeightAt(x,y); z >= 0; z--) {
                    if (blocks.hasClassification(x,y,z, Classification.GROUND)) {
                        blocks.set(x,y,z, block);
                    }
                }
            }
        }
        //if, for example, a vegetation block has lots of built blocks around, it will be converted to a built block
        for (XYZBlock xyzBlock : blocks) {
            Block block = xyzBlock.block();
            Classification classification = block.classification();
            if (classification != Classification.GROUND)
                continue;
            if (!block.equals(settings.vegetationBlock()) && !block.equals(settings.builtBlock())) {
                continue;
            }
            List<XYZBlock> neighbors = blocks.xyNeighborsList(xyzBlock);
            if (neighbors.size() == 0 && xyzBlock.z() != ground.getHeightAt(xyzBlock.x(), xyzBlock.y())) {
                blocks.remove(xyzBlock);
                continue;
            }

            Block opposite = block.equals(settings.vegetationBlock()) ? settings.builtBlock() : settings.vegetationBlock();

            long count = neighbors.stream().filter(n -> n.block().equals(opposite)).count();
            if (count >= neighbors.size() / 2.0) {
                blocks.set(xyzBlock, opposite);
            }
        }
    }
}
