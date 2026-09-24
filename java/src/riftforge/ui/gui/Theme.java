package riftforge.ui.gui;

import riftforge.model.Element;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.util.EnumMap;
import java.util.Map;

/** Paleta y utilerías de tema de la interfaz; los colores siguen el elemento de cada carta. */
public final class Theme {
    private Theme() {
    }

    public static final Color BG_TOP = new Color(0x141826);
    public static final Color BG_BOTTOM = new Color(0x20263A);
    public static final Color PANEL = new Color(0x252E44);
    public static final Color PANEL_LIGHT = new Color(0x33405C);
    public static final Color TEXT = new Color(0xF1F3F8);
    public static final Color TEXT_DIM = new Color(0xB7BFCE);
    public static final Color ACCENT = new Color(0xF9B400);
    public static final Color LIFE = new Color(0x43A047);
    public static final Color LIFE_LOW = new Color(0xE53935);
    public static final Color MANA = new Color(0x2E8BD9);
    public static final Color BTN_SECONDARY = new Color(0x44516F);
    public static final Color BTN_PLAY = new Color(0x2E7D32);
    public static final Color BTN_DISCARD = new Color(0x59647C);

    private static final Map<Element, Color> ELEMENT = new EnumMap<>(Element.class);

    static {
        ELEMENT.put(Element.FUEGO, new Color(0xE65A3B));
        ELEMENT.put(Element.AGUA, new Color(0x2E8BD9));
        ELEMENT.put(Element.ELECTRICO, new Color(0xF2B705));
        ELEMENT.put(Element.SOMBRA, new Color(0x7B4FB8));
        ELEMENT.put(Element.DRAGON, new Color(0x12BEAA));
        ELEMENT.put(Element.VACIO, new Color(0x9667E0));
    }

    public static Color element(Element element) {
        Color color = ELEMENT.get(element);
        return color == null ? PANEL_LIGHT : color;
    }

    public static void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // paleta por defecto del sistema
        }
        UIManager.put("Button.background", BTN_SECONDARY);
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("Button.select", new Color(0x2A3145));
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Label.font", Typeface.body(Font.PLAIN, 14));
        UIManager.put("Panel.background", PANEL);
        UIManager.put("TextArea.background", PANEL);
        UIManager.put("TextArea.foreground", TEXT);
        UIManager.put("TextArea.caretForeground", TEXT);
    }

    /** Da estilo explícito a un botón para que el texto siempre contraste con el fondo. */
    public static void styleButton(JButton button, Color background, int fontSize) {
        button.setOpaque(true);
        button.setBackground(background);
        button.setForeground(TEXT);
        button.setFont(Typeface.body(Font.BOLD, fontSize));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 40), 1),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));
        button.setRolloverEnabled(false);
        button.setContentAreaFilled(true);
    }

    /** Estilo para botones de menú (cabecera). */
    public static void styleSecondary(JButton button) {
        styleButton(button, BTN_SECONDARY, 14);
    }
}