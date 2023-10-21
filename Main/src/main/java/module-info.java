module Main {
    requires CommonData;
    requires LAS;
    requires OpenStreetMap;
    requires com.google.gson;
    requires org.apache.logging.log4j;
    requires MinecraftSchematic;
    requires WorldDecorator;
    requires jdk.unsupported;

    opens org.patonki.downloader to com.google.gson;
    opens org.patonki.settings to com.google.gson;
}