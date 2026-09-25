@echo off
rem ============================================================
rem  RiftForge TCG - Compilador de instalador para Windows
rem  Requisitos: JDK 17+ (javac, jar, jlink, jpackage) en PATH y,
rem  para el MSI, WiX Toolset 3.11 (https://wixtoolset.org).
rem  Ejecutar dentro de la carpeta del proyecto (doble clic o cmd).
rem ============================================================
setlocal
cd /d "%~dp0"

echo [1/5] Compilando...
if exist out rd /s /q out
mkdir out
dir /s /b java\src\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
if errorlevel 1 (
    echo Compilacion fallida.
    del sources.txt
    exit /b 1
)
del sources.txt

echo [2/5] Empaquetando JAR autocontenido...
if exist "%TEMP%\rfstage" rd /s /q "%TEMP%\rfstage"
mkdir "%TEMP%\rfstage"
jar --create --file "%TEMP%\rfstage\RiftForge.jar" --main-class riftforge.app.Main ^
    -C out . ^
    -C java/resources cards2.csv ^
    -C java/resources cards ^
    -C java/resources backgrounds ^
    -C java/resources fonts
copy /y "%TEMP%\rfstage\RiftForge.jar" RiftForge.jar >nul

echo [3/5] Generando runtime ligero (jlink)...
if exist runtime-image rd /s /q runtime-image
jlink --add-modules java.base,java.desktop,java.logging ^
    --strip-debug --no-header-files --no-man-pages ^
    --compress=zip-6 --output runtime-image

echo [4/5] Creando instalador MSI (requiere WiX)...
if not exist dist mkdir dist
jpackage --type msi --name "RiftForge" ^
    --input "%TEMP%\rfstage" ^
    --main-jar RiftForge.jar ^
    --main-class riftforge.app.Main ^
    --runtime-image runtime-image ^
    --dest dist ^
    --win-menu ^
    --app-version 1.0.0
if errorlevel 1 (
    echo No fue posible generar el MSI. Creando app portable:
    jpackage --type app-image --name "RiftForge" ^
        --input "%TEMP%\rfstage" ^
        --main-jar RiftForge.jar ^
        --main-class riftforge.app.Main ^
        --runtime-image runtime-image ^
        --dest dist
)

echo [5/5] Listo. Revisa la carpeta dist\
endlocal