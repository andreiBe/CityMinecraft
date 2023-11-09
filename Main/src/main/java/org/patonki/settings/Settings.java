package org.patonki.settings;

import org.patonki.citygml.endpoint.GmlOptions;
import org.patonki.las.settings.LasReaderSettings;
import org.patonki.openstreetmap.settings.OpenStreetMapSettings;


public class Settings {
    private final LasReaderSettings lasSettings;
    private final OpenStreetMapSettings osmSettings;

    private final GmlOptions gmlSettings;

    public Settings(LasReaderSettings lasSettings, OpenStreetMapSettings osmSettings, GmlOptions gmlSettings) {
        this.lasSettings = lasSettings;
        this.osmSettings = osmSettings;
        this.gmlSettings = gmlSettings;
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
}
