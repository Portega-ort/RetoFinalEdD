package riftforge.ui.gui;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Carga y cachea los fondos decorativos de la mesa:
 * {@code java/resources/backgrounds/board.}* (panel de juego) y
 * {@code java/resources/backgrounds/table.}* (mesa de la mano).
 * Se decodifican con {@link ImageIO} de forma síncrona; si el archivo no
 * existe se devuelve {@code null} y la interfaz cae al degradado por defecto.
 */
final class BackgroundStore {
    private static final String BOARD = "java/resources/backgrounds/board";
    private static final String TABLE = "java/resources/backgrounds/table";
    private static final String LOG = "java/resources/backgrounds/log";
    private static final String[] EXTENSIONS = {".png", ".jpeg", ".jpg"};
    private static final Map<String, Image> CACHE = new HashMap<>();

    private BackgroundStore() {
    }

    static Image board() {
        return load(BOARD);
    }

    static Image table() {
        return load(TABLE);
    }

    static Image log() {
        return load(LOG);
    }

    private static Image load(String base) {
        Image cached = CACHE.get(base);
        if (cached != null) return cached;
        Image image = null;
        for (String extension : EXTENSIONS) {
            File file = new File(base + extension);
            if (file.isFile()) {
                try {
                    image = ImageIO.read(file);
                    CACHE.put(base, image);
                    return image;
                } catch (IOException ignored) {
                    // se prueba la siguiente extensión
                }
            }
        }
        return null;
    }
}