#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

rm -rf out dist runtime-image /tmp/rfstage /tmp/rficon
mkdir -p out dist

echo "[1/6] Compilando..."
javac -encoding UTF-8 -d out $(find java/src -name '*.java')

echo "[2/6] Empaquetando JAR autocontenido..."
mkdir -p /tmp/rfstage
jar --create --file /tmp/rfstage/RiftForge.jar --main-class riftforge.app.Main \
    -C out . \
    -C java/resources cards2.csv \
    -C java/resources cards \
    -C java/resources backgrounds \
    -C java/resources fonts
cp /tmp/rfstage/RiftForge.jar RiftForge.jar

echo "[3/6] Generando runtime ligero (jlink)..."
jlink --add-modules java.base,java.desktop,java.logging \
    --strip-debug --no-header-files --no-man-pages \
    --compress=zip-6 --output runtime-image

echo "[4/6] Creando icono (icns)..."
mkdir -p /tmp/rficon/icon.iconset
sips -c 1024 1024 java/resources/backgrounds/table.jpeg --out /tmp/rficon/master.png >/dev/null 2>&1
for spec in "16:icon_16x16.png" "32:icon_16x16@2x.png" "32:icon_32x32.png" \
            "64:icon_32x32@2x.png" "128:icon_128x128.png" "256:icon_128x128@2x.png" \
            "256:icon_256x256.png" "512:icon_256x256@2x.png" "512:icon_512x512.png" \
            "1024:icon_512x512@2x.png"; do
    s="${spec%%:*}"; f="${spec##*:}"
    sips -z "$s" "$s" /tmp/rficon/master.png --out "/tmp/rficon/tmp_$f" >/dev/null 2>&1
    sips -s format png "/tmp/rficon/tmp_$f" --out "/tmp/rficon/icon.iconset/$f" >/dev/null 2>&1
    rm -f "/tmp/rficon/tmp_$f"
done
iconutil -c icns /tmp/rficon/icon.iconset -o /tmp/rficon/RiftForge.icns

echo "[5/6] Creando la app..."
jpackage --type app-image --name "RiftForge" \
    --input /tmp/rfstage \
    --main-jar RiftForge.jar \
    --main-class riftforge.app.Main \
    --runtime-image runtime-image \
    --dest dist \
    --icon /tmp/rficon/RiftForge.icns \
    --mac-package-identifier com.riftforge.tcg \
    --mac-package-name "RiftForge" \
    --app-version 1.0 \
    --java-options "-Dapple.awt.application.name=RiftForge"

echo "[6/6] Creando instalador DMG..."
jpackage --type dmg --name "RiftForge" \
    --input /tmp/rfstage \
    --main-jar RiftForge.jar \
    --main-class riftforge.app.Main \
    --runtime-image runtime-image \
    --dest dist \
    --icon /tmp/rficon/RiftForge.icns \
    --mac-package-identifier com.riftforge.tcg \
    --mac-package-name "RiftForge" \
    --app-version 1.0 \
    --java-options "-Dapple.awt.application.name=RiftForge"

echo "Listo: dist/RiftForge.app y dist/RiftForge-1.0.dmg"