package org.patonki.color;

import java.util.ArrayList;

public class ColorAverage {
    private float num;
    private int r, g, b;
    private float h, s, v;
    private final Color defaultColor = new Color(255, 255, 255);

    private final ArrayList<Color> colors = new ArrayList<>();

    //https://stackoverflow.com/questions/649454/what-is-the-best-way-to-average-two-colors-that-define-a-linear-gradient/73287322#73287322
    private Color labAverage(ArrayList<Color> colors) {
        double[][] labs = new double[colors.size()][3];
        for (int i = 0; i < colors.size(); i++) {
            labs[i] = XYZtoLAB(RGBtoXYZ(colors.get(i)));
        }
        var ave = simpleAverage(labs);
        var newXYZ = LabToXYZ(ave);
        return XYZtoRGB(newXYZ);
    }

    public double[] RGBtoXYZ(Color RGB) {
        //https://stackoverflow.com/questions/15408522/rgb-to-xyz-and-lab-colours-conversion
        int R = RGB.r();
        int G = RGB.g();
        int B = RGB.b();

        double var_R = R / 255.0;        //R from 0 to 255
        double var_G = G / 255.0;        //G from 0 to 255
        double var_B = B / 255.0;        //B from 0 to 255

        if (var_R > 0.04045) var_R = Math.pow((var_R + 0.055) / 1.055, 2.4);
        else var_R = var_R / 12.92;
        if (var_G > 0.04045) var_G = Math.pow((var_G + 0.055) / 1.055, 2.4);
        else var_G = var_G / 12.92;
        if (var_B > 0.04045) var_B = Math.pow((var_B + 0.055) / 1.055, 2.4);
        else var_B = var_B / 12.92;

        var_R = var_R * 100;
        var_G = var_G * 100;
        var_B = var_B * 100;

        //Observer. = 2°, Illuminant = D65
        double X = var_R * 0.4124 + var_G * 0.3576 + var_B * 0.1805;
        double Y = var_R * 0.2126 + var_G * 0.7152 + var_B * 0.0722;
        double Z = var_R * 0.0193 + var_G * 0.1192 + var_B * 0.9505;
        return new double[]{X, Y, Z};
    }

    public double[] XYZtoLAB(double[] XYZ) {
        //https://stackoverflow.com/questions/15408522/rgb-to-xyz-and-lab-colours-conversion
        double x = XYZ[0];
        double y = XYZ[1];
        double z = XYZ[2];
        var ref_X = 95.047;
        var ref_Y = 100.000;
        var ref_Z = 108.883;
        double var_X = x / ref_X;     //ref_X =  95.047   Observer= 2°, Illuminant= D65
        double var_Y = y / ref_Y;     //ref_Y = 100.000
        double var_Z = z / ref_Z;     //ref_Z = 108.883

        if (var_X > 0.008856) var_X = Math.pow(var_X, (1 / 3d));
        else var_X = (7.787 * var_X) + (16 / 116d);
        if (var_Y > 0.008856) var_Y = Math.pow(var_Y, (1 / 3d));
        else var_Y = (7.787 * var_Y) + (16 / 116d);
        if (var_Z > 0.008856) var_Z = Math.pow(var_Z, (1 / 3d));
        else var_Z = (7.787 * var_Z) + (16 / 116d);

        var CIE_L = (116 * var_Y) - 16;
        var CIE_a = 500 * (var_X - var_Y);
        var CIE_b = 200 * (var_Y - var_Z);

        return new double[]{CIE_L, CIE_a, CIE_b};
    }

    private double[] simpleAverage(double[]... colors) {
        double one = 0;
        double two = 0;
        double three = 0;
        for (double[] color : colors) {
            one += color[0];
            two += color[1];
            three += color[2];
        }
        return new double[]{one / colors.length, two / colors.length, three / colors.length};
    }

    private double[] LabToXYZ(double[] lab) {
        //adapted from easyRGB.com
        //The tristimulus values are (X, Y, Z) = (109.85, 100.00, 35.58)
        var ref_X = 95.047;
        var ref_Y = 100.000;
        var ref_Z = 108.883;
        var l = lab[0];
        var a = lab[1];
        var b = lab[2];

        var var_Y = (l + 16) / 116;
        var var_X = a / 500 + var_Y;
        var var_Z = var_Y - b / 200;

        if (Math.pow(var_Y,3) > 0.008856 ) var_Y = Math.pow(var_Y,3);
        else var_Y = (var_Y - 16 / 116d) / 7.787;
        if (Math.pow(var_X,3) > 0.008856 ) var_X = Math.pow(var_X,3);
        else var_X = (var_X - 16 / 116d) / 7.787;
        if (Math.pow(var_Z,3) > 0.008856 ) var_Z = Math.pow(var_Z,3);
        else var_Z = (var_Z - 16 / 116d) / 7.787;

        var X = var_X * ref_X;
        var Y = var_Y * ref_Y;
        var Z = var_Z * ref_Z;
        return new double[]{X, Y, Z};
    }

    private Color XYZtoRGB(double[] xyz) {
        //adapted from easyRGB.com
        var X = xyz[0];
        var Y = xyz[1];
        var Z = xyz[2];

        var var_X = X / 100d;
        var var_Y = Y / 100d;
        var var_Z = Z / 100d;

        var var_R = var_X * 3.2406 + var_Y * -1.5372 + var_Z * -0.4986;
        var var_G = var_X * -0.9689 + var_Y * 1.8758 + var_Z * 0.0415;
        var var_B = var_X * 0.0557 + var_Y * -0.2040 + var_Z * 1.0570;

        if (var_R > 0.0031308) var_R = 1.055 * (Math.pow(var_R,(1 / 2.4)) )-0.055;
         else var_R = 12.92 * var_R;
        if (var_G > 0.0031308) var_G = 1.055 * (Math.pow(var_G,(1 / 2.4)) )-0.055;
        else var_G = 12.92 * var_G;
        if (var_B > 0.0031308) var_B = 1.055 * (Math.pow(var_B,(1 / 2.4)) )-0.055;
        else var_B = 12.92 * var_B;

        int sR = (int) (var_R * 255);
        int sG = (int) (var_G * 255);
        int sB = (int) (var_B * 255);

        return new Color(sR, sG, sB);
    }

    public void addColor(int color) {
        int mask = ((1 << 8) - 1);
        //argb
        int red = (color >> 16) & mask;
        int green = (color >> 8) & mask;
        int blue = color & mask;
        float[] hsbValues = java.awt.Color.RGBtoHSB(red, green, blue, null);

        h += hsbValues[0];
        s += hsbValues[1];
        v += hsbValues[2];

        r += red * red;
        g += green * green;
        b += blue * blue;
        num++;
        //addColor(new Color(red, green, blue));
    }

    public void addColor(Color color) {
        r += color.r() * color.r();
        g += color.g() * color.g();
        b += color.b() * color.b();
        float[] hsbValues = java.awt.Color.RGBtoHSB(color.r(), color.g(), color.b(), null);
        //if (hsbValues[1] < 0.1) return;
        h += hsbValues[0];
        s += hsbValues[1];
        v += hsbValues[2];
        num++;

        this.colors.add(color);
    }

    public Color getAverage() {
        if (num == 0) {
            return defaultColor;
        }
//        java.awt.Color c = new java.awt.Color(java.awt.Color.HSBtoRGB(h / num, s / num, v / num));
//        return new Color(c.getRed(), c.getGreen(), c.getBlue());
        return new Color((int) Math.sqrt(r/num), (int) Math.sqrt(g/num), (int) Math.sqrt(b/num));
        //return labAverage(colors);
    }
}
