package org.patonki.citygml.downloader;

import org.patonki.citygml.features.*;
import org.patonki.citygml.math.Point2D;
import org.xmlobjects.gml.model.geometry.AbstractGeometry;
import org.citygml4j.core.model.appearance.*;
import org.citygml4j.core.model.appearance.Color;
import org.citygml4j.core.model.building.Building;
import org.citygml4j.core.model.construction.AbstractConstructionSurface;
import org.citygml4j.core.model.construction.RoofSurface;
import org.citygml4j.core.model.construction.WallSurface;
import org.citygml4j.core.model.core.AbstractFeature;
import org.citygml4j.core.util.reference.DefaultReferenceResolver;
import org.citygml4j.core.visitor.ObjectWalker;
import org.citygml4j.xml.CityGMLContext;
import org.citygml4j.xml.CityGMLContextException;
import org.citygml4j.xml.reader.CityGMLInputFactory;
import org.citygml4j.xml.reader.CityGMLReadException;
import org.citygml4j.xml.reader.CityGMLReader;
import org.xmlobjects.gml.model.geometry.GeometricPosition;
import org.xmlobjects.gml.model.geometry.aggregates.MultiSurface;
import org.xmlobjects.gml.model.geometry.primitives.AbstractRing;
import org.xmlobjects.gml.model.geometry.primitives.AbstractSurface;
import org.xmlobjects.gml.model.geometry.primitives.LinearRing;
import org.xmlobjects.gml.model.geometry.primitives.Polygon;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Reads CityGml files and returns Building Objects
 */
public class CityGmlParser {
    private final String imgDownloadLocation;

    /**
     * Creates a CityGmlParser
     *
     * @param imgDownloadLocation Folder that will contain the texture files downloaded from an external source.
     */
    public CityGmlParser(String imgDownloadLocation) {
        this.imgDownloadLocation = imgDownloadLocation;
        File folder = new File(imgDownloadLocation);
        if (!folder.exists()) {
            boolean success = folder.mkdir();
            if (!success) {
                throw new IllegalStateException("Not able to create cache folder! " + imgDownloadLocation);
            }
        }
    }

    /**
     * Loads an image from an external source and downloads it locally
     * to the folder specified in the constructor
     * this is done so the images only need to be downloaded once through the internet
     *
     * @param uri path to image
     * @return the local path where the image is located after downloading
     */

    private String loadImage(String uri) {
        String name = uri;
        int slashIndex = uri.lastIndexOf('/');
        if (slashIndex != -1) {
            name = uri.substring(slashIndex + 1);
        }
        String path = imgDownloadLocation + "/" + name;
        File file = new File(path);
        //already downloaded
        if (file.exists()) return path;
        //downloads the file and writes it to local file
        try (BufferedInputStream in = new BufferedInputStream(new URL(uri).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(path)) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed writing file! " + path);
        }
        return path;
    }

    /**
     * Parses a Building object from the cityGML library and returns my Building implementation
     *
     * @param building Building in the gml file
     * @return Building object
     */
    private org.patonki.citygml.features.Building parseBuilding(Building building) {
        //structure = wall or roof

        //maps structure ids to Textures
        HashMap<String, Texture> textureMap = new HashMap<>();

        ArrayList<Wall> walls = new ArrayList<>();
        ArrayList<Roof> roofs = new ArrayList<>();

        //references inside the gml file must be resolved
        //for example, textures reference the structures that they provide textures to
        DefaultReferenceResolver resolver = DefaultReferenceResolver.newInstance();
        resolver.resolveReferences(building);
        //traversing the textures of the file
        building.accept(new TextureParser(textureMap));
        //traversing the structures of the file
        building.accept(new StructureParser(building.getId(), textureMap, walls, roofs));
        //returning the finished building
        return new org.patonki.citygml.features.Building(walls.toArray(new Wall[0]), roofs.toArray(new Roof[0]));
    }

    private class TextureParser extends ObjectWalker {
        private final HashMap<String, Texture> textureMap;

        public TextureParser(HashMap<String, Texture> textureMap) {
            this.textureMap = textureMap;
        }

        @Override
        public void visit(X3DMaterial material) {
            Color diffuseColor = material.getDiffuseColor();
            for (GeometryReference target : material.getTargets()) {
                String id = target.getReferencedObject().getId();
                textureMap.put(id, new Material(diffuseColor.getRed(), diffuseColor.getGreen(), diffuseColor.getBlue()));
            }
        }

        @Override
        public void visit(ParameterizedTexture texture) {
            String imgUri = texture.getImageURI();
            imgUri = loadImage(imgUri);
            //the referenced structures of the texture
            List<TextureAssociationProperty> associations = texture.getTextureParameterizations();
            for (TextureAssociationProperty associationProperty : associations) {
                TextureAssociation association = associationProperty.getObject();
                TexCoordList textureParams = (TexCoordList) association.getTextureParameterization().getObject();
                //the textures contain something called textureCoordinates that define how the texture
                //should be positioned on a surface
                //https://www.web3d.org/documents/specifications/19775-1/V3.3/Part01/components/texturing.html#Texturecoordinates
                List<TextureCoordinates> textureCoordinates = textureParams.getTextureCoordinates();
                List<Double> coordinatesDoubleList = textureCoordinates.get(0).getValue();
                Point2D[] coordinates = new Point2D[coordinatesDoubleList.size() / 2];

                for (int i = 0; i < coordinatesDoubleList.size(); i += 2) {
                    double x = coordinatesDoubleList.get(i);

                    double y = coordinatesDoubleList.get(i + 1);
                    coordinates[i / 2] = new Point2D(x, y);
                }
                AbstractGeometry refObj = association.getTarget().getReferencedObject();
                if (refObj == null) return;
                String id = refObj.getId();
                ;
                textureMap.put(id, new ImgTexture(imgUri, coordinates));
            }
        }
    }

    private static class StructureParser extends ObjectWalker {
        private final String buildingId;
        private final HashMap<String, Texture> textureMap;
        private final ArrayList<Wall> walls;
        private final ArrayList<Roof> roofs;

        public StructureParser(String buildingId, HashMap<String, Texture> textureMap, ArrayList<Wall> walls, ArrayList<Roof> roofs) {
            this.buildingId = buildingId;
            this.textureMap = textureMap;
            this.walls = walls;
            this.roofs = roofs;
        }

        //interface for creating a structure based on the parameters
        private interface StructureMaker<T> {
            T create(Polygon3D polygon3D, String id);
        }

        /**
         * Creates a structure of the house. Without this function the creation of walls and roofs would
         * have a lot of repeated code. Returns null if the structure cannot be created
         *
         * @param surface The 3D shape of the structure
         * @param maker   Basically the constructor of the structure (wall or roof)
         * @param <T>     The type of structure to be created
         * @return Structure that has a shape and texture or null if cannot be created
         */
        private <T> T createStructure(AbstractConstructionSurface surface,
                                      StructureMaker<T> maker) {
            MultiSurface multiSurface = surface.getLod2MultiSurface().getObject();
            AbstractSurface surfaceMember = multiSurface.getSurfaceMember().get(0).getObject();
            //making sure the geometry is defined as a polygon
            if (!(surfaceMember instanceof Polygon polygon)) {
                throw new IllegalArgumentException("Expected surface member of polygon but got: " + surfaceMember.getClass().getSimpleName());
            }
            String id = polygon.getId();
            //TODO REMOVE
            //if (!id.equals("Roof_Solid_336188758_103_72")) return null;
            AbstractRing exterior = polygon.getExterior().getObject();
            //The polygon must be defined as a LinearRing
            if (!(exterior instanceof LinearRing linear)) {
                throw new IllegalArgumentException("Expected LinearRing but got: " + exterior.getClass().getSimpleName());
            }
            //reading the points of the polygon
            List<GeometricPosition> geoPoints = linear.getControlPoints().getGeometricPositions();
            Point[] points = new Point[geoPoints.size()];
            for (int i = 0; i < geoPoints.size(); i++) {
                GeometricPosition geometricPosition = geoPoints.get(i);
                List<Double> c = geometricPosition.getPos().getValue();
                Point point = new Point(c.get(0), c.get(1), c.get(2));
                points[i] = point;
            }
            //using the id of the polygon to find the matching texture
            Texture texture = this.textureMap.get(id);

            try {
                Polygon3D pol = new Polygon3D(points, texture);
                return maker.create(pol, id);
            } catch (Polygon3D.PolygonException e) {
                //e.printStackTrace();
                return null;
            }
        }

        @Override
        public void visit(RoofSurface roofSurface) {
            //System.out.println("Creating ROOF");
            Roof roof = createStructure(roofSurface, Roof::new);
            if (roof != null) {
                //System.out.println("Roof id " + roof.getId());
                roofs.add(roof);
            }
        }

        @Override
        public void visit(WallSurface wallSurface) {
            //System.out.println("Creating WALL");
            Wall wall = createStructure(wallSurface, Wall::new);
            if (wall != null) {
                //System.out.println("Wall id " + wall.getId());
                walls.add(wall);
            }
        }
    }

    public static class GmlParseException extends Exception {
        public GmlParseException(String msg) {
            super(msg);
        }
    }

    //Separate private method so error handling is easier
    //The custom error class is needed because the user of this class should not need
    //any classes from the CityGML library
    private BuildingCollection readFeaturesImpl(String path) throws CityGMLContextException, CityGMLReadException {
        CityGMLContext context = CityGMLContext.newInstance();

        CityGMLInputFactory in = context.createCityGMLInputFactory();
        ArrayList<org.patonki.citygml.features.Building> buildings = new ArrayList<>();
        try (CityGMLReader reader = in.createCityGMLReader(new File(path))) {
            while (reader.hasNext()) {
                AbstractFeature building = reader.next();
                if (!(building instanceof Building)) {
                    System.out.println("Found something that isn't a building: " + building.getClass().getSimpleName());
                    continue;
                }
                //if (!building.getId().contains("Building_1155304")) continue;
                var b = parseBuilding((Building) building);
                buildings.add(b);
            }
        }
        return new BuildingCollection(buildings.toArray(new org.patonki.citygml.features.Building[0]));
    }

    /**
     * Reads and parses a GML file. Returns a {@link BuildingCollection} that is a collection of {@link org.patonki.citygml.features.Building}.
     *
     * @param path path of city gml file (.gml file extension)
     * @return Collection of Features (right now only buildings)
     * @throws GmlParseException If an error occurs while reading the file
     */
    public BuildingCollection readFeatures(String path) throws GmlParseException {
        try {
            return readFeaturesImpl(path);
        } catch (CityGMLContextException | CityGMLReadException e) {
            e.printStackTrace();
            throw new GmlParseException(e.getMessage());
        }
    }

}

