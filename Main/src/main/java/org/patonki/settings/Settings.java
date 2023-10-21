package org.patonki.settings;

import interfaces.settings.LasReaderSettings;
import endpoint.settings.OpenStreetMapSettings;


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
