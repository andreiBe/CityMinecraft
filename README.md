# CityMinecraft

Program to generate the city of Turku in minecraft using open data from multiple sources
including open data offered by Turku and OpenStreetMap.

Download the minecraft bedrock version world at: https://drive.google.com/file/d/1ANYLMlJxY1Xfg4vZFJR9dnkfz1F-MEdv/view?usp=sharing

Java version world: https://drive.google.com/file/d/1hzzTfAduNJWQHlPH9cQlYsKtgz6Sz8sd/view?usp=sharing
 
![View of the city](imgs/example.png)

The program uses a combination of lidar data and vector based data to build the Minecraft city.
The conversion process from these data sources to a Minecraft world consists of the following steps:
- READ_LAS &rarr; Reads the lidar files and converts them into Minecraft blocks
- FIX_LAS &rarr; Improves the result of the previous step
- AERIAL_IMAGES &rarr; Colors the ground using aerial images to be either grass or stone (vegetation vs human built)
- OSM &rarr; Adds roads, waterways and land use information from OpenStreetMap
- GML &rarr; Uses 3d-models of buildings to make the buildings more detailed
- DECORATE &rarr; Does small improvements like adding trunks to trees and placing flowers on grass
- SCHEMATIC &rarr; Creates .schematic files from the minecraft blocks that may be used in Minecraft world editor programs like world edit
- MINECRAFT_WORLD &rarr; Writes the schematic files to a minecraft world using a program called Amulet

## Customizing the result
Customizing the result is possible using json config files.
These config files control the following things among many others:
- How the lidar data points are mapped to Minecraft blocks. 
- The blocks used for roads, fields and other land covers
- How the textures of the buildings are converted to Minecraft blocks
- The area to download from the data providers like The City of Turku
- The number of threads that the program will use
## Running the program using IntelliJ
Open the project in IntelliJ and IntelliJ should install Maven automatically. Then run the program.
See below instruction for Maven to see the command line arguments of the program

## Running the program using Maven

### Compiling
```
mvn package
```
Creates an executable jar into the directory Main/target/
### Creating json config templates
```
java -jar Main/target/Main-1.0-SNAPSHOT-bin.jar createTemplates
```

### Downloading the data
```
java -Xmx10g -jar Main/target/Main-1.0-SNAPSHOT-bin.jar download <path to json config>
```
Optional command line arguments (inserted at the end of the above command separated by spaces)
- -las &rarr; downloads the lidar data (point cloud)
- -osm &rarr; downloads the osm data (roads, land use, waterways)
- -gml &rarr; download the cityGml building models
- -aerial &rarr; downloads the aerial images
- -nolog &rarr; only log warnings and errors
- -replace &rarr; download everything again even if it already has been downloaded

Example:
```
java -Xmx10g -jar Main/target/Main-1.0-SNAPSHOT-bin.jar download config/download.json -las -osm -nolog
```

### Running the generated jar
```
java -Xmx10g -jar Main/target/Main-1.0-SNAPSHOT-bin.jar run <path to json config> <path to folder with lidar files>
```
Optional command line arguments (inserted at the end of the above command separated by spaces)
- --files <files inside the lidar data folder separated by dots\> &rarr; only runs the specified files in the folder
- --log <WARN | ERROR | INFO | DEBUG> &rarr; log4j log level. Only logs events  equally or more important than the level
- --skip <execution steps separated by dots\> &rarr; skips these steps
- --cache <execution steps separated by dots\> &rarr; caches the result of these steps
- --start <execution step\> &rarr; Defines which cache to start from. Does not run earlier steps if the start step is cached
- --end <execution step\> &rarr; Defines which cache to stop execution at. Caches this step and does not run any steps after it.
- --copy &rarr; Copies the minecraft world to the default world location of the Minecraft bedrock edition
- --overwrite <true | false\> &rarr; whether to overwrite the old cache
- --delete <true | false> &rarr; whether to delete the old cache

Example:
```
java -Xmx10g -jar Main/target/Main-1.0-SNAPSHOT-bin.jar run config/run-config.json inputData/lasFiles --files 23459500_6705000.laz,23459000_6705000.laz --skip OSM,DECORATE --log WARN --end GML --copy --overwrite true
```

## Used libraries:
- https://github.com/mreutegg/laszip4j (reading lidar data)
- https://github.com/geotools/geotools (reading osm data)
- https://github.com/Querz/NBT (writing minecraft schematics)
- https://github.com/Amulet-Team/Amulet-Core (writing minecraft worlds)
