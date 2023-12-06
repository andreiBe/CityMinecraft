package org.patonki.citygml.citygml;

import org.patonki.blocks.Blocks;
import org.patonki.citygml.BuildingReplacer;
import org.patonki.citygml.GmlFeaturesInArea;
import org.patonki.citygml.features.BuildingCollection;
import org.patonki.data.Block;
import org.patonki.data.Classification;

public class CityGmlEndpoint {
    private final GmlOptions options;

    private final String texturesFolder;
    private final boolean multiThread;

    private final Block buildingBlock;
    private final Block roofBlock;
    private final String downloadFolder;

    public CityGmlEndpoint(String downloadFolder, String texturesFolder, GmlOptions options, boolean multiThread,
                           Block buildingBlock, Block roofBlock) {
        this.downloadFolder = downloadFolder;
        this.options = options;
        this.texturesFolder = texturesFolder;
        this.multiThread = multiThread;
        this.buildingBlock = buildingBlock;
        this.roofBlock = roofBlock;
    }

    public void applyBuildings(Blocks blocks) throws Exception {
        CityGmlDownloader downloader = new CityGmlDownloader(this.downloadFolder);
        BuildingCollection buildings = downloader.deserialize(blocks.getMinX(), blocks.getMinY(),
                blocks.getMinX() + blocks.getWidth(), blocks.getMinY() + blocks.getLength(), blocks.getSideLength());

        GmlFeaturesInArea processor = new GmlFeaturesInArea();

        //The buildings produced by the las data are replaced by placeholders
        //This is done because the old buildings are removed if they are close to the new "better" city gml buildings
        //but if the new city gml buildings contain the blocks used for the buildings, those blocks would be removed
        //from the new building

        //these placeholders are blocks that will never be in the data because they don't exist in minecraft
        Block placeHolderBuildingBlock = new Block(6, 20, Classification.BUILDING);
        Block placeHolderRoofBlock = new Block(27, 20, Classification.BUILDING);
        BuildingReplacer buildingReplacer = new BuildingReplacer(blocks, placeHolderBuildingBlock, placeHolderRoofBlock);

        blocks.forEachSet((x, y, z, block) -> {
            if (block == null) return null;
            if (block.equals(buildingBlock)) return placeHolderBuildingBlock;
            if (block.equals(roofBlock)) return placeHolderRoofBlock;
            return block;
        });
        processor.process(buildings, options, buildingReplacer, texturesFolder,this.multiThread);
        //Replacing the placeholders with the old blocks
        blocks.forEachSet((x,y,z,block) -> {
            if (block == null) return null;
            if (block.equals(placeHolderBuildingBlock)) return buildingBlock;
            if (block.equals(placeHolderRoofBlock)) return roofBlock;
            return block;
        });
    }
}
