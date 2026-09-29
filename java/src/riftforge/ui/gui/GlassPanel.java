package riftforge.ui.gui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Panel translúcido con las esquinas redondeadas, para que el tablero, la mesa
 * o el fondo de la pantalla de inicio se vean alrededor de cada zona de juego
 * o de cada botón de menú.
 */
final class GlassPanel extends JPanel {
    private final Color fill;

    GlassPanel(Color color, int alpha) {
        this.fill = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(fill);
        int arc = Math.min(22, Math.min(getWidth(), getHeight()) / 6);
        g.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        g.dispose();
    }

    @Override
    public void update(Graphics g) {
        paintComponent(g);
    }
}
