/**
 * Writing minecraft .schematic files and importing them into minecraft worlds.
 */
module MinecraftSchematic {
    requires org.apache.logging.log4j;
    requires NBT;
    requires org.apache.commons.io;

    exports org.patonki.converter;
}