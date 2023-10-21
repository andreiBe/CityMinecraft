package data;

/**
 * Represents a minecraft block with a block id, data and classification <br>
 * The id and data uniquely identify a minecraft block
 * @see <a href="https://minecraftitemids.com/">Minecraft item ids</a>
 */
public record Block(byte id, byte data, Classification classification) { }
