/**
 * Adds land use, road and waterway information to the minecraft block landscape.
 */
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

    exports org.patonki.openstreetmap;
    exports org.patonki.openstreetmap.settings;
    exports org.patonki.types;

    opens org.patonki.openstreetmap.settings;
}