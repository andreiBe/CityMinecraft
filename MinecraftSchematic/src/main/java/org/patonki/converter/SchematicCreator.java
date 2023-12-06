package org.patonki.converter;

import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class SchematicCreator {
    private static final Logger LOGGER = LogManager.getLogger(SchematicCreator.class);
    /**
     * Writes the minecraft blocks into a schematic. Google .schematic folder to understand how
     * the block id and data arrays work.
     * @param filename The path where the schematic will be written
     * @param blockArray the block ids
     * @param dataArray the block data
     * @param width width of the area
     * @param length length of the area
     * @param height height of the area
     * @throws IOException if the writing fails
     * @see <a href="https://minecraft.fandom.com/wiki/Schematic_file_format">Schematic format</a>
     */
    public void writeSchematic(String filename, byte[] blockArray, byte[] dataArray, short width, short length, short height) throws IOException {
        CompoundTag tag = new CompoundTag();
        NamedTag namedTag = new NamedTag("schematic", tag);

        tag.put("Width", new ShortTag(width));
        tag.put("Height", new ShortTag(height));
        tag.put("Length", new ShortTag(length));
        ByteArrayTag blocks = new ByteArrayTag(blockArray);
        ByteArrayTag nbt_data = new ByteArrayTag(dataArray);
        tag.put("Blocks", blocks);
        tag.put("Data", nbt_data);

        tag.put("Materials", new StringTag("Alpha"));
        tag.put("'WEOriginX'", new IntTag(0));
        tag.put("'WEOriginY'", new IntTag(0));
        tag.put("'WEOriginZ'", new IntTag(0));
        tag.put("'WEOffsetX'", new IntTag(0));
        tag.put("'WEOffsetY'", new IntTag(0));
        tag.put("'WEOffsetZ'", new IntTag(0));
        tag.put("Entities", new ListTag<>(IntTag.class));
        tag.put("TileEntities", new ListTag<>(IntTag.class));

        LOGGER.info("Writing schematic file to " + filename);
        NBTUtil.write(namedTag, filename, false);
    }
}
