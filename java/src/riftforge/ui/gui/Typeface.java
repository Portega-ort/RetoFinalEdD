package riftforge.ui.gui;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Resuelve la tipografía del juego en tres roles: {@code display} (títulos y
 * números), {@code body} (interfaz y cartas) y {@code mono} (bitácora).
 * Orden de preferencia por rol:
 * <ol>
 *   <li>archivo empaquetado en {@code java/resources/fonts/role.ttf|.otf},</li>
 *   <li>familia instalada en el sistema (p. ej. Orbitron, Futura),</li>
 *   <li>fuente lógica por defecto de Java.</li>
 * </ol>
 */
final class Typeface {
    private static final String FONT_DIR = "java/resources/fonts/";
    private static final String[] EXTENSIONS = {".ttf", ".otf"};

    private static final Font DISPLAY = resolve("display",
            new String[]{"Orbitron", "Aldrich", "Futura", "Avenir Next", "Helvetica Neue"},
            new Font(Font.SANS_SERIF, Font.PLAIN, 1));
    private static final Font BODY = resolve("body",
            new String[]{"Rajdhani", "Avenir Next", "Segoe UI", "Helvetica Neue", "Arial"},
            new Font(Font.SANS_SERIF, Font.PLAIN, 1));
    private static final Font MONO = resolve("mono",
            new String[]{"Share Tech Mono", "Menlo", "Consolas", "Courier New"},
            new Font(Font.MONOSPACED, Font.PLAIN, 1));

    private static Set<String> installed;

    private Typeface() {
    }

    static Font display(int style, int size) {
        return DISPLAY.deriveFont(style, (float) size);
    }

    static Font body(int style, int size) {
        return BODY.deriveFont(style, (float) size);
    }

    static Font mono(int style, int size) {
        return MONO.deriveFont(style, (float) size);
    }

    private static Font resolve(String role, String[] families, Font fallback) {
        for (String extension : EXTENSIONS) {
            File file = new File(FONT_DIR + role + extension);
            if (file.isFile()) {
                try {
                    return Font.createFont(Font.TRUETYPE_FONT, file);
                } catch (IOException | java.awt.FontFormatException ignored) {
                    // se prueba la siguiente extensión / estrategia
                }
            }
        }
        for (String family : families) {
            if (installed().contains(family)) {
                return new Font(family, Font.PLAIN, 1);
            }
        }
        return fallback;
    }

    private static Set<String> installed() {
        if (installed == null) {
            installed = new HashSet<>(java.util.Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        }
        return installed;
    }
}
