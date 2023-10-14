package interfaces;

import blocks.ArrayBlocks;
import blocks.Blocks;
import interfaces.settings.LasReaderSettings;
import reader.LasDataToBlocks;
import reader.LasReader;

public class LASEndPoint {
    private final LasReader reader;
    private final LasDataToBlocks converter;

    public LASEndPoint(LasReaderSettings settings) {
        this.reader = new LasReader(
                settings::mapToClassification,
                settings::classificationShouldBeIgnored
        );
        this.converter = new LasDataToBlocks(settings::mapToBlock);
    }
    public Blocks convertLazDataToBlocks(String lasFileLocation) {
        LasReader.LazData lazData = this.reader.read(lasFileLocation);
        LasDataToBlocks.BlockMaker maker = ArrayBlocks::new;
        return this.converter.convert(lazData, maker);
    }
}
