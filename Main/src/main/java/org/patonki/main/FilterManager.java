package org.patonki.main;

import org.patonki.openstreetmap.FeatureFilterer;

import java.io.File;
import java.io.IOException;

public class FilterManager {
    private final FeatureFilterer osmFilterer;
    private final String filterFolderPath;

    public FilterManager(String filterFolderPath, FeatureFilterer.FeatureFiltererOptions osmOptions) {
        this.filterFolderPath = filterFolderPath;
        File filterFolder = new File(this.filterFolderPath);
        if (!filterFolder.exists()) filterFolder.mkdirs();
        this.osmFilterer = new FeatureFilterer(osmOptions);
    }
    public void prepareData(String[] shapefiles) throws FeatureFilterer.FilterException, IOException {
        this.osmFilterer.readMany(shapefiles, filterFolderPath);
    }
}
