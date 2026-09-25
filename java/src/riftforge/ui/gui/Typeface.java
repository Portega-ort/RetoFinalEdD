package riftforge.ui.gui;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Resuelve la tipografía del juego en tres roles: {@code display} (títulos y
 * números), {@code body} (interfaz y cartas) y {@code mono} (bitácora).
 * Orden de preferencia por rol:
 * <ol>
 *   <li>archivos empaquetados en {@code java/resources/fonts/role.ttf|.otf}
 *       y su variante {@code role-bold.ttf|.otf} para el estilo negrita,</li>
 *   <li>familia instalada en el sistema (p. ej. Orbitron, Futura),</li>
 *   <li>fuente lógica por defecto de Java.</li>
 * </ol>
 */
final class Typeface {
    private static final String[] EXTENSIONS = {".ttf", ".otf"};

    private static final String[][] ROLE_FAMILIES = {
            {"display", "Orbitron", "Aldrich", "Futura", "Avenir Next", "Helvetica Neue", "SansSerif"},
            {"body", "Rajdhani", "Avenir Next", "Segoe UI", "Helvetica Neue", "Arial", "SansSerif"},
            {"mono", "Share Tech Mono", "Menlo", "Consolas", "Courier New", "Monospaced"},
    };

    private static final Map<String, Font[]> WEIGHTS = new HashMap<>();
    private static final Map<String, Font> FALLBACKS = new HashMap<>();
    private static Set<String> installed;

    private Typeface() {
    }

    static Font display(int style, int size) {
        return weight("display", style).deriveFont(style, (float) size);
    }

    static Font body(int style, int size) {
        return weight("body", style).deriveFont(style, (float) size);
    }

    static Font mono(int style, int size) {
        return weight("mono", style).deriveFont(style, (float) size);
    }

    private static Font weight(String role, int style) {
        boolean bold = (style & Font.BOLD) != 0;
        Font[] pair = WEIGHTS.computeIfAbsent(role, Typeface::resolvePair);
        Font font = pair[bold ? 1 : 0];
        return font != null ? font : pair[0];
    }

    private static Font[] resolvePair(String role) {
        Font plain = load(role);
        Font bold = load(role + "-bold");
        Font fallback = fallbackFor(role);
        if (plain == null) plain = fallback;
        if (bold == null) {
            bold = plain;
        }
        return new Font[]{plain, bold};
    }

    private static Font load(String role) {
        for (String extension : EXTENSIONS) {
            try (InputStream in = Resources.open("fonts/" + role + extension)) {
                if (in != null) {
                    return Font.createFont(Font.TRUETYPE_FONT, in);
                }
            } catch (IOException | java.awt.FontFormatException ignored) {
                // se prueba la siguiente extensión / estrategia
            }
        }
        return null;
    }

    private static Font fallbackFor(String role) {
        Font cached = FALLBACKS.get(role);
        if (cached != null) return cached;
        String[] families = null;
        for (String[] candidate : ROLE_FAMILIES) {
            if (candidate[0].equals(role)) {
                families = candidate;
                break;
            }
        }
        Font fallback = new Font(Font.SANS_SERIF, Font.PLAIN, 1);
        if (families != null) {
            for (int i = 1; i < families.length; i++) {
                if (installed().contains(families[i])) {
                    fallback = new Font(families[i], Font.PLAIN, 1);
                    break;
                }
            }
        }
        FALLBACKS.put(role, fallback);
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