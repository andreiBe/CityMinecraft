package data;

public record Block(byte id, byte data, Classification classification) {
    public boolean hasSameClassification(Block block) {
        return block != null && this.classification == block.classification;
    }
}
