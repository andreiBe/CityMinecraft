package endpoint;

import data.BoundingBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.data.*;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.data.*;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.geometry.jts.GeometryBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.util.URLs;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class FeatureFilterer {
    public static class FilterException extends Exception {
        public FilterException(String message) {
            super(message);
        }
    }
    private final FeatureFiltererOptions options;

    public FeatureFilterer(FeatureFiltererOptions options) {
        this.options = options;
    }

    public record FeatureFiltererOptions(String osmCoordinateSystem,
                                         String targetCoordinateSystem, BoundingBox boundingBox) {
    }
    private static final Logger LOGGER = LogManager.getLogger(FeatureFilterer.class);

    private void exportToShapefile(SimpleFeatureCollection collection, String typeName, File directory) throws IOException {
        SimpleFeatureType ft = collection.getSchema();

        String fileName = ft.getTypeName();
        File file = new File(directory, fileName + ".shp");

        Map<String, Serializable> creationParams = new HashMap<>();
        creationParams.put("url", URLs.fileToUrl(file));

        FileDataStoreFactorySpi factory = FileDataStoreFinder.getDataStoreFactory("shp");
        DataStore dataStore = factory.createNewDataStore(creationParams);

        dataStore.createSchema(ft);

        SimpleFeatureStore featureStore = (SimpleFeatureStore) dataStore.getFeatureSource(typeName);

        Transaction t = new DefaultTransaction();
        try {
            featureStore.addFeatures(collection);
            t.commit(); // write it out
        } catch (IOException e) {
            e.printStackTrace();
            try {
                t.rollback();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        } finally {
            t.close();
        }
    }
    private boolean inBounds(Geometry geometry, Polygon box) {
        return box.intersects(geometry);
    }
    public void readSingle(String shapefilePath, String exportFolderPath) throws IOException,FilterException {
        try {
            read(shapefilePath, exportFolderPath);
        } catch (FactoryException | TransformException e) {
            e.printStackTrace();
            throw new FilterException("Problem with coordinate conversions");
        }
    }

    private void read(String shapefilePath, String exportFolderPath) throws IOException, FactoryException, TransformException {
        LOGGER.debug("Starting to read features from " + shapefilePath);
        File file = new File(shapefilePath);
        file.setReadOnly();

        FileDataStore dataStore = new ShapefileDataStore(file.toURI().toURL());

        CoordinateReferenceSystem sourceCRS = CRS.decode(options.osmCoordinateSystem, true);
        CoordinateReferenceSystem targetCRS = CRS.decode(options.targetCoordinateSystem, true); //canada 2953, finland? 3130
        MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
        GeometryBuilder geoBuilder = new GeometryBuilder();
        //23450000,6693000, 23469000,6730500
        BoundingBox bBox = options.boundingBox;
        Polygon box = geoBuilder.box(bBox.minX(),bBox.minY(),  bBox.maxX(),bBox.maxY());

        SimpleFeatureSource source = dataStore.getFeatureSource();
        Filter filter = Filter.INCLUDE;

        SimpleFeatureCollection collection = source.getFeatures(filter);
        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();
        try (SimpleFeatureIterator featureIterator = collection.features()) {
            while (featureIterator.hasNext()) {
                SimpleFeature feature = featureIterator.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Coordinate[] coordinates = geometry.getCoordinates();
                for (Coordinate value : coordinates) {
                    JTS.transform(value, value, transform);
                }
                if (!this.inBounds(geometry, box)) {
                    continue;
                }
                featureCollection.add(feature);
            }
        } finally {
            dataStore.dispose();
        }
        LOGGER.debug("Saving to " + exportFolderPath + ". Number of features: " + featureCollection.size());
        if (featureCollection.size() == 0) {
            throw new IllegalArgumentException("0 features inside the boundaries");
        }
        exportToShapefile(featureCollection, featureCollection.getSchema().getTypeName(), new File(exportFolderPath));
    }

    public void readMany(String[] shapefiles, String exportFolderPath) throws IOException, FilterException {
        for (String shapefile : shapefiles) {
            this.readSingle(shapefile, exportFolderPath);
        }
    }
}
