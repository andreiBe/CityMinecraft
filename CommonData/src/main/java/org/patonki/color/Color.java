package org.patonki.color;

public record Color(int r, int g, int b) {
    public Color {
        if (r < 0 || r > 255) throw new IllegalArgumentException("Red must be in range: 0-255 but was: " + r);
        if (g < 0 || g > 255) throw new IllegalArgumentException("Green must be in range: 0-255 but was: " + g);
        if (b < 0 || b > 255) throw new IllegalArgumentException("Blue must be in range: 0-255 but was: " + b);
    }
    public static final Color WHITE = new Color(255, 255, 255);

    public static Color fromInt(int i) {
        int mask = ((1 << 8)-1);
        int red = (i >> 16) & mask;
        int green = (i >> 8) & mask;
        int blue = i & mask;
        return new Color(red, green, blue);
    }

    public int toInt() {
        return 0xff << 24 | r() << 16 | g() << 8 | b();
    }
}