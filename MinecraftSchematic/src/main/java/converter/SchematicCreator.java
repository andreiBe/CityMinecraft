package converter;

import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.*;

import java.io.IOException;

public class SchematicCreator {

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
        NBTUtil.write(namedTag, filename, true);
    }
}
