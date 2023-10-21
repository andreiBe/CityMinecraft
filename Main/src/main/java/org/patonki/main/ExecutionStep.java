package org.patonki.main;

public enum ExecutionStep {
    BEGINNING(0), READ_LAS(1), FIX_LAS(2), OSM(3), GML(4), DECORATE(5), SCHEMATIC(6), MINECRAFT_WORLD(7), END(8);

    public final int number;
    ExecutionStep(int number) {
        this.number = number;
    }
}