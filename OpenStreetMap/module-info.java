module OpenStreetMap {
    requires org.geotools.api;
    requires org.geotools.shapefile;
    requires org.geotools.swing;
    requires org.geotools.main;
    requires org.geotools.referencing;
    requires org.geotools.metadata;
    requires org.locationtech.jts;
    requires org.apache.logging.log4j;
    requires java.desktop;
    requires CommonData;

    exports endpoint;
    exports endpoint.settings;
}