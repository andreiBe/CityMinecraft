/**
 * Module for converting LAS (Lidar) data to minecraft blocks
 */
module LAS {
    exports org.patonki.las;
    exports org.patonki.las.settings;
    requires CommonData;
    requires laszip4j;
    requires org.apache.logging.log4j;

    opens org.patonki.las.settings;
}