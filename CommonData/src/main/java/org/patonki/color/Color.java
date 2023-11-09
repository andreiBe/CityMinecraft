package org.patonki.color;

public record Color(int r, int g, int b) {
    public static final Color WHITE = new Color(255, 255, 255);

    public static Color fromInt(int i) {
        int mask = ((1 << 8)-1);
        int red = (i >> 16) & mask;
        int green = (i >> 8) & mask;
        int blue = i & mask;
        return new Color(red, green, blue);
    }
    
    public double distance(Color color) {
        return Math.sqrt( ( this.r - color.r) * (this.r - color.r) + (this.g - color.g) * (this.g - color.g) + (this.b - color.b) * (this.b - color.b));
    }

    public int toInt() {
        return 0xff << 24 | r() << 16 | g() << 8 | b();
    }
}