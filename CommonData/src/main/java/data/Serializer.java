package data;

public abstract class Serializer<T> {
    public abstract byte[] serialize(T val);
    public abstract T deserialize(byte[] ar);
    protected void writeIntToByteArray(byte[] arr, int offset, int i) {
        arr[offset] = (byte) ((i & 0xFF000000) >> 24);
        arr[offset+1] = (byte) ((i & 0x00FF0000) >> 16);
        arr[offset+2] = (byte) ((i & 0x0000FF00) >> 8);
        arr[offset+3] = (byte) ((i & 0x000000FF));
    }
    protected void writeInts(byte[] arr, int offset, int... ints) {
        for (int i = 0; i < ints.length; i++) {
            writeIntToByteArray(arr, offset + i * 4, ints[i]);
        }
    }
    protected static int readInt(byte[] arr, int offset) {
        return (0xFF & arr[offset]) << 24 | (0xFF & arr[offset+1]) << 16 | (0xFF & arr[offset+2]) << 8 | (0xFF & arr[offset+3]);
    }
    protected static int[] readInts(byte[] arr, int offset, int number) {
        int[] ret = new int[number];
        for (int i = 0; i < number; i++) {
            ret[i] = readInt(arr, offset+i*4);
        }
        return ret;
    }
}
