package org.patonki.settings;

import org.patonki.citygml.citygml.GmlOptions;
import org.patonki.groundcolor.GroundColorSettings;
import org.patonki.las.settings.LasReaderSettings;
import org.patonki.openstreetmap.settings.OpenStreetMapSettings;


public class Settings {
    private final LasReaderSettings lasSettings;
    private final OpenStreetMapSettings osmSettings;

    private final GmlOptions gmlSettings;

    private final GroundColorSettings groundColorSettings;

    private final int threadCount;


    public Settings(LasReaderSettings lasSettings, OpenStreetMapSettings osmSettings, GmlOptions gmlSettings, GroundColorSettings groundColorSettings, int threadCount) {
        this.lasSettings = lasSettings;
        this.osmSettings = osmSettings;
        this.gmlSettings = gmlSettings;
        this.groundColorSettings = groundColorSettings;
        this.threadCount = threadCount;
    }

    public GroundColorSettings getGroundColorSettings() {
        return groundColorSettings;
    }

    public LasReaderSettings getLasSettings() {
        return lasSettings;
    }

    public OpenStreetMapSettings getOsmSettings() {
        return osmSettings;
    }

    public GmlOptions getGmlSettings() {
        return gmlSettings;
    }

    public int getThreadCount() {
        return threadCount;
    }
}
