package riftforge.ui.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Abre un recurso del juego primero desde el classpath (dentro del JAR o de la
 * app) y, si no existe, desde la carpeta {@code java/resources} del directorio
 * de trabajo (modo desarrollo). Los nombres van relativos a la raíz de
 * recursos: {@code "cards/CHR_01.jpg"}, {@code "backgrounds/board.jpeg"},
 * {@code "fonts/display.ttf"}.
 */
final class Resources {
    private Resources() {
    }

    /** @return el flujo del recurso, o {@code null} si no se encuentra. */
    static InputStream open(String name) {
        InputStream in = Resources.class.getResourceAsStream("/" + name);
        if (in != null) return in;
        File file = new File("java/resources", name);
        if (file.isFile()) {
            try {
                return new FileInputStream(file);
            } catch (IOException ignored) {
                return null;
            }
        }
        return null;
    }
}