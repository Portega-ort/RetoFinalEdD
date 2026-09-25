package riftforge.ui.gui;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Carga y cachea los fondos decorativos de la mesa:
 * {@code resources/backgrounds/board.}* (panel de juego),
 * {@code resources/backgrounds/table.}* (mesa de la mano) y
 * {@code resources/backgrounds/log.}* (bitácora). Se leen desde el classpath
 * (JAR/app) o desde {@code java/resources}; se decodifican con {@link ImageIO}
 * de forma síncrona y, si el archivo no existe, se devuelve {@code null} y la
 * interfaz cae al degradado por defecto.
 */
final class BackgroundStore {
    private static final String BOARD = "backgrounds/board";
    private static final String TABLE = "backgrounds/table";
    private static final String LOG = "backgrounds/log";
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
            try (InputStream in = Resources.open(base + extension)) {
                if (in != null) {
                    image = ImageIO.read(in);
                    CACHE.put(base, image);
                    return image;
                }
            } catch (IOException ignored) {
                // se prueba la siguiente extensión
            }
        }
        return null;
    }
}