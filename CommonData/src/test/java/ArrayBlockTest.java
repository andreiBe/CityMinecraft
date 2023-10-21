import blocks.ArrayBlocks;
import data.Block;
import data.Classification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ArrayBlockTest {
    @Test
    void serialization() {
        ArrayBlocks blocks = new ArrayBlocks(500, 200, 50, 1853536, 534534, 160);
        blocks.set(7,6,5, new Block((byte) 7, (byte) 3, Classification.GROUND));
        byte[] serialized = new ArrayBlocks.ArrayBlockSerializer().serialize(blocks);

        ArrayBlocks deserialized = new ArrayBlocks.ArrayBlockSerializer().deserialize(serialized);

        assertEquals(blocks.getWidth(), deserialized.getWidth());
        assertEquals(blocks.getLength(), deserialized.getLength());
        assertEquals(blocks.getHeight(), deserialized.getHeight());

        assertEquals(blocks.getMinX(), deserialized.getMinX());
        assertEquals(blocks.getMinY(), deserialized.getMinY());
        assertEquals(blocks.getMinZ(), deserialized.getMinZ());

        Block block = blocks.get(7,6,5);
        Block deserializedBlock = deserialized.get(7,6,5);

        assertEquals(block.id(), deserializedBlock.id());
        assertEquals(block.data(), deserializedBlock.data());
        assertEquals(block.classification(), deserializedBlock.classification());
    }
}
