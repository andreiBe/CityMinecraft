import sys
import amulet

print("Python script starting")
schematics_file = sys.argv[1]
minecraft_world_path = sys.argv[2]
minX = int(sys.argv[3])
minY = int(sys.argv[4])
# the minecraft world to write to
level = amulet.load_level(minecraft_world_path)

# reading the coordinates from the filename
filename = schematics_file.split("/")[-1].split(".")[0]
(x, y, z, width, length, height) = (int(x) for x in filename.split("_"))
x -= minX
y -= minY
schematic = amulet.load_level(schematics_file)
# the normal world, not nether or end in minecraft
dimension = schematic.dimensions[0]
bounding_box = schematic.bounds(dimension)

selection_box = bounding_box.selection_boxes[0]
# minecraft has a weird coordinate system where Y is the height instead of Z
level.paste(schematic, dimension,
            bounding_box, level.dimensions[0],
            # the center of the structure (not the lower corner)
            (y + length // 2, z + height // 2 + 1, x + width // 2), include_blocks=True, include_entities=False
            )
level.save()
level.close()

