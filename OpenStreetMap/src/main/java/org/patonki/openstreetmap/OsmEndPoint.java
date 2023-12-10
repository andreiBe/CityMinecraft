package org.patonki.openstreetmap;

import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.patonki.OsmFeaturesToMinecraft;
import org.patonki.blocks.Blocks;
import org.patonki.landUse.LandUseReader;
import org.patonki.openstreetmap.settings.OpenStreetMapSettings;
import org.patonki.roads.RoadReader;
import org.patonki.waterways.WaterwayReader;

import java.io.IOException;

/**
 * Class for adding land use, road and waterway information to the block model.
 * Takes in settings to customize the process.
 */
public class OsmEndPoint {
    private final OpenStreetMapSettings settings;
    private final LandUseReader landUseReader;
    private final RoadReader roadReader;
    private final WaterwayReader waterWayReader;
    private final String landUsePath;
    private final String roadsPath;
    private final String waterwaysPath;
    /**
     * @param settings settings
     * @param landUsePath Path to the filtered land use shapefile
     * @param roadsPath Path to the filtered roads shapefile
     * @param waterwaysPath Path to the filtered waterways shapefile
     */
    public OsmEndPoint(OpenStreetMapSettings settings, String landUsePath, String roadsPath, String waterwaysPath) {
        this.settings = settings;
        this.landUseReader = new LandUseReader(settings::getLandUseInfo);
        this.roadReader = new RoadReader(settings::getRoadInfo);
        this.waterWayReader = new WaterwayReader(settings::getWaterwayInfo);
        this.landUsePath = landUsePath;
        this.roadsPath = roadsPath;
        this.waterwaysPath = waterwaysPath;
    }

    /**
     * Add land use, roads and waterways to the landscape
     * There are different types of roads that have different surface materials and widths
     * Land use affects the block of the ground
     * Waterways fill water where needed
     * @param blocks The blocks object to be modified
     * @throws IOException if the osm features can't be read from the shapefiles
     */
    public void addOsmFeatures(Blocks blocks) throws IOException {
        try {
            var landUseList = settings.isAddLandUse() ? landUseReader.read(landUsePath) : null;
            var roadList = settings.isAddRoads() ? roadReader.read(roadsPath) : null;
            var waterWaysList = settings.isAddWaterWays() ? waterWayReader.read(waterwaysPath) : null;
            var converter = new OsmFeaturesToMinecraft(landUseList, roadList, waterWaysList);
            converter.applyFeatures(blocks, settings.isAddLandUse(), settings.isAddRoads(), settings.isAddWaterWays());
        } catch (FactoryException | TransformException e) {
            throw new RuntimeException(e);
        }
    }
}
