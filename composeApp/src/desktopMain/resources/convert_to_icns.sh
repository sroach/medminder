#!/bin/bash

# Set the source and destination paths
SOURCE_PNG="../../commonMain/composeResources/drawable/medminder.png"
ICONSET_DIR="medminder.iconset"
ICNS_FILE="medminder.icns"

# Create the iconset directory
mkdir -p "$ICONSET_DIR"

# Convert the PNG to various sizes required for an ICNS file
sips -z 16 16 "$SOURCE_PNG" --out "$ICONSET_DIR/icon_16x16.png"
sips -z 32 32 "$SOURCE_PNG" --out "$ICONSET_DIR/icon_16x16@2x.png"
sips -z 32 32 "$SOURCE_PNG" --out "$ICONSET_DIR/icon_32x32.png"
sips -z 64 64 "$SOURCE_PNG" --out "$ICONSET_DIR/icon_32x32@2x.png"
sips -z 128 128 "$SOURCE_PNG" --out "$ICONSET_DIR/icon_128x128.png"
sips -z 256 256 "$SOURCE_PNG" --out "$ICONSET_DIR/icon_128x128@2x.png"
sips -z 256 256 "$SOURCE_PNG" --out "$ICONSET_DIR/icon_256x256.png"
sips -z 512 512 "$SOURCE_PNG" --out "$ICONSET_DIR/icon_256x256@2x.png"
sips -z 512 512 "$SOURCE_PNG" --out "$ICONSET_DIR/icon_512x512.png"
sips -z 1024 1024 "$SOURCE_PNG" --out "$ICONSET_DIR/icon_512x512@2x.png"

# Convert the iconset to ICNS
iconutil -c icns "$ICONSET_DIR" -o "$ICNS_FILE"

# Clean up the temporary iconset directory
rm -rf "$ICONSET_DIR"

echo "Conversion complete. ICNS file created at $ICNS_FILE"