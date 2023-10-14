import blocks.Blocks;
import interfaces.LASEndPoint;
import endpoint.OsmEndPoint;
import settings.Settings;

import java.io.IOException;

public class WorldBuilder {
    private final LASEndPoint LASEndPoint;
    private final OsmEndPoint osmEndPoint;

    public WorldBuilder(Settings settings) {
        this.LASEndPoint = new LASEndPoint(settings.getLasSettings());
        this.osmEndPoint = new OsmEndPoint(settings.getOsmSettings());
    }

    public void run(String lazFile, String landUsePath, String roadsPath, String waterwaysPath) throws IOException {
        Blocks blocks = LASEndPoint.convertLazDataToBlocks(lazFile);
        osmEndPoint.addOsmFeatures(blocks, landUsePath, roadsPath, waterwaysPath);

    }
}
