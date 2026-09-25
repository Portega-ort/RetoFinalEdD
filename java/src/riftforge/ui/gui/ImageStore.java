package riftforge.ui.gui;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Carga y cachea la imagen de cada carta (ruta {@code <uid>.jpeg} en
 * resources/cards), desde el classpath o la carpeta de desarrollo. Se decodifica
 * con {@link ImageIO} de forma síncrona para que la imagen esté completa en la
 * primera pintada y nunca dependa de un repintado posterior (p. ej. al
 * maximizar la ventana).
 */
final class ImageStore {
    private static final Map<String, Image> SCALED = new HashMap<>();
    private static final Map<String, Image> RAW = new HashMap<>();
    private static final String[] EXTENSIONS = {".jpeg", ".jpg", ".png"};

    private ImageStore() {
    }

    /** Devuelve la imagen de la carta escalada al tamaño pedido, o {@code null} si no existe. */
    static Image forCard(String uid, int width, int height) {
        String key = uid + "@" + width + "x" + height;
        Image cached = SCALED.get(key);
        if (cached != null) return cached;
        Image raw = loadRaw(uid);
        Image scaled = raw == null ? null : raw.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        SCALED.put(key, scaled);
        return scaled;
    }

    private static Image loadRaw(String uid) {
        Image raw = RAW.get(uid);
        if (raw != null) return raw;
        for (String extension : EXTENSIONS) {
            try (InputStream in = Resources.open("cards/" + uid + extension)) {
                if (in != null) {
                    raw = ImageIO.read(in);
                    RAW.put(uid, raw);
                    return raw;
                }
            } catch (IOException ignored) {
                // se prueba la siguiente extensión
            }
        }
        return null;
    }
}