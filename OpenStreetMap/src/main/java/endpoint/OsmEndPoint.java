package endpoint;

import blocks.Blocks;
import endpoint.settings.OpenStreetMapSettings;
import landUse.LandUseReader;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import roads.RoadReader;
import waterways.WaterwayReader;

import java.io.IOException;

public class OsmEndPoint {
    private final OpenStreetMapSettings settings;
    private final LandUseReader landUseReader;
    private final RoadReader roadReader;
    private final WaterwayReader waterWayReader;
    private final String landUsePath;
    private final String roadsPath;
    private final String waterwaysPath;

    public OsmEndPoint(OpenStreetMapSettings settings, String landUsePath, String roadsPath, String waterwaysPath) {
        this.settings = settings;
        this.landUseReader = new LandUseReader(settings::getLandUseInfo);
        this.roadReader = new RoadReader(settings::getRoadInfo);
        this.waterWayReader = new WaterwayReader(settings::getWaterwayInfo);
        this.landUsePath = landUsePath;
        this.roadsPath = roadsPath;
        this.waterwaysPath = waterwaysPath;
    }
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
