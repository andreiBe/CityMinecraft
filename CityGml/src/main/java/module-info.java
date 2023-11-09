module CityGml {
    requires CommonData;
    requires org.apache.logging.log4j;
    requires commons.math3;
    requires org.citygml4j.core;
    requires org.citygml4j.xml;
    requires java.desktop;
    requires com.google.gson;

    exports org.patonki.citygml.features to com.google.gson;
    exports org.patonki.citygml.endpoint;

    opens org.patonki.citygml.features to com.google.gson;
    opens org.patonki.citygml.math to com.google.gson;
    opens org.patonki.citygml.endpoint to com.google.gson;
}