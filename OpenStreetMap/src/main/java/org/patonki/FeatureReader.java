package org.patonki;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.filter.Filter;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.patonki.util.ArrayMap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

//Base class for reading OpenStreetMap features from shapefiles
public abstract class FeatureReader<T, TYPE, INFO> {
    protected final Logger LOGGER = LogManager.getLogger(this);

    public FeatureReader(InfoSupplier<TYPE, INFO> supplier) {
        this.supplier = supplier;
    }

    public interface InfoSupplier<TYPE, INFO> {
        INFO get(TYPE type);
    }
    protected final InfoSupplier<TYPE, INFO> supplier;

    private static final ArrayMap<String, ArrayList<Object>> cachedReads = new ArrayMap<>();
    private static final ReentrantLock lock = new ReentrantLock();
    protected record Feature(Coordinate[] coordinates,
                             SimpleFeature feature,
                             Geometry geometry) {

        public Object getAttribute(String name) {
            return this.feature.getAttribute(name);
        }
    }
    protected boolean isValid(Feature feature) {
        return true;
    }

    protected T process(Feature feature) {
        return null;
    }

    protected void processAfterRead(ArrayList<T> list) {

    }

    @SuppressWarnings("unchecked")
    public final ArrayList<T> read(String path) throws FactoryException, IOException, TransformException {
        lock.lock();
        try {
            if (cachedReads.containsKey(path)) {
                LOGGER.debug("The features are cached from path " + path);
                return (ArrayList<T>) cachedReads.get(path);
            }
            LOGGER.debug("Starting to read features from " + path);
            File file = new File(path);

            FileDataStore dataStore = new ShapefileDataStore(file.toURI().toURL());

            SimpleFeatureSource source = dataStore.getFeatureSource();
            Filter filter = Filter.INCLUDE;

            SimpleFeatureCollection collection = source.getFeatures(filter);
            ArrayList<T> items = new ArrayList<>();
            try (SimpleFeatureIterator featureIterator = collection.features()) {
                while (featureIterator.hasNext()) {
                    SimpleFeature feature = featureIterator.next();
                    Geometry geometry = (Geometry) feature.getDefaultGeometry();
                    Coordinate[] coordinates = geometry.getCoordinates();

                    Feature f = new Feature(coordinates, feature, geometry);
                    if (!this.isValid(f)) continue;
                    T item = this.process(f);
                    items.add(item);
                }
            } finally {
                dataStore.dispose();
            }
            processAfterRead(items);
            LOGGER.debug("Found " + items.size() +" features");

            cachedReads.put(path, (ArrayList<Object>) items);
            return items;
        } finally {
            lock.unlock();
        }

    }
}
