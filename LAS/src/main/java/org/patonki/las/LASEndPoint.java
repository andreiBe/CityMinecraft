package org.patonki.las;

import org.patonki.blocks.ArrayBlocks;
import org.patonki.blocks.Blocks;
import org.patonki.blocks.OctTreeBlocks;
import org.patonki.data.Classification;
import org.patonki.fixer.BlockDataFixer;
import org.patonki.las.settings.LasReaderSettings;
import org.patonki.reader.LasDataToBlocks;
import org.patonki.reader.LasReader;

/**
 * Takes in a las file that contains a lidar point cloud and converts it into a
 * minecraft block mesh. <br> Also includes a method to fix some inconsistencies/faults in the
 * input las data.
 */
public class LASEndPoint {
    private final LasReader reader;
    private final LasDataToBlocks converter;
    private final BlockDataFixer fixer;
    private final LasDataToBlocks.BlockMaker blockMaker;

    /**
     * Takes in the settings that will be used in the conversion process
     * @param settings settings
     */
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
        //the implementation to use for the storage of blocks
        this.blockMaker = settings.useOctTree()
                ? (w, l, h, x, y, z) -> new OctTreeBlocks(w,l,h,x,y,z, 25000)
                : ArrayBlocks::new;
    }

    /**
     * Takes in a las file and converts the data into minecraft blocks
     * The resulting {@link Blocks} object is going to have some faults, for example,
     * floating buildings, random blocks all over, holes in the ground ect.
     * Therefore, the data should be "fixed" with {@link #fixProblemsWithLASData(Blocks)}
     * <br><br>
     * The conversion algorithm basically just gathers all points that lie inside one cube
     * and finds the most plentiful las-classification and determines the minecraft block based on that
     * <br>
     * @param lasFileLocation path to the las file
     * @return The minecraft blocks
     * @see <a href="https://www.asprs.org/wp-content/uploads/2010/12/LAS_1_4_r13.pdf">Las file specification</a>
     */
    public Blocks convertLazDataToBlocks(String lasFileLocation) {
        LasReader.LazData lazData = this.reader.read(lasFileLocation);
        return this.converter.convert(lazData, this.blockMaker);
    }

    /**
     * Improves the data in multiple ways because the lidar technology does not produce perfect results
     * The data can, for example, contain building blocks in the sky for some reason
     * (maybe due to some weird reflections). The lidar data also classifies some blocks incorrectly.
     * Some blocks that are supposed to be buildings may be classified as plants.
     * @param blocks The minecraft blocks
     */
    public void fixProblemsWithLASData(Blocks blocks) {
        fixer.improveData(blocks);
    }
}
