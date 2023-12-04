package org.patonki.color;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.patonki.data.Block;
import org.patonki.data.Classification;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class that is able to convert a color into a minecraft block
 * For example, the color blue might be mapped to a lapis lazuli block
 */
public abstract class ColorBlockConverter implements IColorToBlockConverter {
    protected record BlockEntry(Group group, Block block, Color averageColor, BufferedImage image) {}
    protected final ArrayList<BlockEntry> blockEntries;

    private static final Logger LOGGER = LogManager.getLogger(ColorBlockConverter.class);

    private final Classification classification;

    public ColorBlockConverter(Classification classification, String texturePath, ColorToBlockConverterOptions options, Block[] banned) throws IOException {
        this.classification = classification;
        this.blockEntries = readBlockEntries(texturePath, options);
        this.blockEntries.removeIf(be -> Arrays.stream(banned).anyMatch(b -> b.id() == be.block.id() && b.data() == be.block.data()));
    }
    private ArrayList<BlockEntry> readBlockEntries(String texturePath, ColorToBlockConverterOptions options) throws IOException {
        ArrayList<BlockEntry> res = new ArrayList<>();
        for (IColorToBlockConverter.BlockEntry blockEntry : options.blockEntries()) {
            File textureFile = new File(texturePath+"/"+blockEntry.textureName()+".png");
            if (!textureFile.exists()) {
                LOGGER.warn("Warning texture: " + textureFile.getPath() + " not found!!!");
                continue;
            }
            BufferedImage image = ImageIO.read(textureFile);
            Color average = averageColorOfImage(image);

            res.add(new BlockEntry(blockEntry.group(),
                    new Block((byte) blockEntry.id(), (byte) blockEntry.data(),this.classification),
                    average, image));
        }
        if (res.size() == 0) {
            throw new IllegalArgumentException("No textures found. Is the texture path correct?");
        }
        return res;
    }
    protected Color averageColorOfImage(BufferedImage image) {
        ColorAverage colorAverage = new ColorAverage();

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int color = image.getRGB(x,y);
                colorAverage.addColor(color);
            }
        }
        return colorAverage.getAverage();
    }
    public Block convert(int color) {
        return convert(Color.fromInt(color));
    }

    /**
     * @param color Color
     * @return The most closely matching minecraft block.
     */
    public abstract Block convert(Color color);

}
