import org.junit.jupiter.api.Test;
import org.patonki.blocks.ArrayBlocks;
import org.patonki.data.Block;
import org.patonki.data.Classification;

import java.io.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ArrayBlockTest {

    @Test
    void serializationHardcore() throws IOException, NoSuchFieldException, IllegalAccessException {
        File serializedFile = new File("lol.dat");
        try {
            ArrayBlocks blocks = new ArrayBlocks(500, 500, 200, 1853536, 534534, 160, 500);
            Block[][][] correct = new Block[500][500][200];

            Random rng = new Random();
            for (int x = 0; x < blocks.getWidth(); x++) {
                for (int y = 0; y < blocks.getLength(); y++) {
                    for (int z = 0; z < blocks.getHeight(); z++) {
                        if (rng.nextBoolean()) continue;
                        Classification classification = switch (rng.nextInt(4)) {
                            case 1 -> Classification.BUILDING;
                            case 2 -> Classification.LOW_VEGETATION;
                            case 3 -> Classification.BRIDGE;
                            default -> Classification.UNKNOWN;
                        };
                        correct[x][y][z] = new Block(rng.nextInt(10)+1, rng.nextInt(3), classification);
                        blocks.set(x,y,z, correct[x][y][z]);
                    }
                }
            }

            byte[] serialized = new ArrayBlocks.ArrayBlockSerializer().serialize(blocks);
            try (FileOutputStream out = new FileOutputStream(serializedFile)) {
                out.write(serialized);
            }

            try (FileInputStream in = new FileInputStream(serializedFile)) {
                ArrayBlocks deserialized = new ArrayBlocks.ArrayBlockSerializer().deserialize(in.readAllBytes());
                for (int x = 0; x < blocks.getWidth(); x++) {
                    for (int y = 0; y < blocks.getLength(); y++) {
                        for (int z = 0; z < blocks.getHeight(); z++) {
                            Block block = deserialized.get(x,y,z);
                            assertEquals(block, correct[x][y][z]);
                        }
                    }
                }
                Field idField = ArrayBlocks.class.getDeclaredField("ids");
                idField.setAccessible(true);

                HashMap<Block, Integer> idsOriginal = (HashMap<Block, Integer>) idField.get(blocks);
                HashMap<Block, Integer> ids = (HashMap<Block, Integer>) idField.get(deserialized);
                assertEquals(idsOriginal, ids);
            }

        } catch (Exception e) {
            serializedFile.delete();
            throw e;
        }

    }
    @Test
    void serialization() {
        ArrayBlocks blocks = new ArrayBlocks(500, 200, 50, 1853536, 534534, 160, 500);
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
