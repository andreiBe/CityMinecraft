/**
 * Module that contains dataclasses used to represent minecraft data
 */
module CommonData
{
    requires org.apache.logging.log4j;
    requires java.desktop;

    requires org.jetbrains.annotations;

    opens org.patonki.blocks;

    exports org.patonki.data;
    exports org.patonki.blocks;
    exports org.patonki.util;
    exports org.patonki.color;
}