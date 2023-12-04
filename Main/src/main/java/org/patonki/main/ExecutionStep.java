package org.patonki.main;

public enum ExecutionStep {
    BEGINNING(0),
    READ_LAS(1),
    FIX_LAS(2),
    AERIAL_IMAGES(3),
    OSM(4),
    GML(5),
    DECORATE(6),
    SCHEMATIC(7),
    MINECRAFT_WORLD(8),
    END(9);

    public final int number;

    public static ExecutionStep getStepByNumber(int number) {
        for (ExecutionStep value : ExecutionStep.values()) {
            if (value.number == number) return value;
        }
        throw new IllegalArgumentException("No execution step found for " + number);
    }
    ExecutionStep(int number) {
        this.number = number;
    }
}