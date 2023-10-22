package org.patonki.settings;

import org.patonki.las.settings.LasReaderSettings;
import org.patonki.openstreetmap.settings.OpenStreetMapSettings;


public class Settings {
    private final LasReaderSettings lasSettings;
    private final OpenStreetMapSettings osmSettings;

    public Settings(LasReaderSettings lasSettings, OpenStreetMapSettings osmSettings) {
        this.lasSettings = lasSettings;
        this.osmSettings = osmSettings;
    }

    public LasReaderSettings getLasSettings() {
        return lasSettings;
    }

    public OpenStreetMapSettings getOsmSettings() {
        return osmSettings;
    }
}
