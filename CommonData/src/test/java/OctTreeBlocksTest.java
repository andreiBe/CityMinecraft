
import blocks.OctTreeBlocks;
import data.Block;
import data.Classification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OctTreeBlocksTest {
    @Test
    void serialization() {
        OctTreeBlocks blocks = new OctTreeBlocks(500, 200, 50, 1853536, 534534, 160, 25000);
        blocks.set(7,6,5, new Block((byte) 7, (byte) 3, Classification.GROUND));
        byte[] serialized = new OctTreeBlocks.OctTreeBlocksSerializer().serialize(blocks);

        OctTreeBlocks deserialized = new OctTreeBlocks.OctTreeBlocksSerializer().deserialize(serialized);

        assertEquals(blocks.getWidth(), deserialized.getWidth());
        assertEquals(blocks.getLength(), deserialized.getLength());
        assertEquals(blocks.getHeight(), deserialized.getHeight());

        assertEquals(blocks.getMinX(), deserialized.getMinX());
        assertEquals(blocks.getMinY(), deserialized.getMinY());
        assertEquals(blocks.getMinZ(), deserialized.getMinZ());

        assertEquals(blocks.getMaxSize(), deserialized.getMaxSize());

        Block block = blocks.get(7,6,5);
        Block deserializedBlock = deserialized.get(7,6,5);

        assertEquals(block.id(), deserializedBlock.id());
        assertEquals(block.data(), deserializedBlock.data());
        assertEquals(block.classification(), deserializedBlock.classification());
    }
}
