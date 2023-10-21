module LAS {
    exports interfaces;
    exports interfaces.settings;
    requires CommonData;
    requires laszip4j;
    requires org.apache.logging.log4j;

    opens interfaces.settings;
}