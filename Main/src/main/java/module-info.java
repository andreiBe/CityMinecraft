/**
 * The Main module that combines the other modules and creates the final product.
 */
module Main {
    requires CommonData;
    requires LAS;
    requires OpenStreetMap;
    requires com.google.gson;
    requires org.apache.logging.log4j;
    requires MinecraftSchematic;
    requires WorldDecorator;
    requires jdk.unsupported;
    requires CityGml;
    requires org.apache.logging.log4j.core;

    exports org.patonki.downloader;
    exports org.patonki.main;
    exports org.patonki.serialize;
    exports org.patonki.settings;

    opens org.patonki.downloader to com.google.gson;
    opens org.patonki.settings to com.google.gson;
}