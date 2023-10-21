package interfaces;

import blocks.ArrayBlocks;
import blocks.Blocks;
import blocks.OctTreeBlocks;
import data.Classification;
import fixer.BlockDataFixer;
import interfaces.settings.LasReaderSettings;
import reader.LasDataToBlocks;
import reader.LasReader;

public class LASEndPoint {
    private final LasReader reader;
    private final LasDataToBlocks converter;
    private final BlockDataFixer fixer;
    private final LasDataToBlocks.BlockMaker blockMaker;

    public LASEndPoint(LasReaderSettings settings) {
        this.reader = new LasReader(
                settings::mapToClassification,
                settings::classificationShouldBeIgnored
        );
        this.fixer = new BlockDataFixer(
                settings.mapToBlock(Classification.BUILDING),
                settings.mapToBlock(Classification.WATER)
        );
        this.converter = new LasDataToBlocks(settings::mapToBlock);
        this.blockMaker = settings.useOctTree() ? (w, l, h, x, y, z) -> new OctTreeBlocks(w,l,h,x,y,z, 25000)
                : ArrayBlocks::new;
    }
    public Blocks convertLazDataToBlocks(String lasFileLocation) {
        LasReader.LazData lazData = this.reader.read(lasFileLocation);
        return this.converter.convert(lazData, this.blockMaker);
    }
    public void fixProblemsWithLASData(Blocks blocks) {
        fixer.improveData(blocks);
    }
}
