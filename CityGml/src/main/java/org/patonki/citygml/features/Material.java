package org.patonki.citygml.features;


import org.patonki.color.Color;

public final class Material extends Texture{
    private final Color color;
    public Material(double red, double green, double blue) {
        this.color = new Color((int)(red*255),(int)(green*255),(int)(blue*255));
    }

    public Color getColor() {
        return color;
    }
}
