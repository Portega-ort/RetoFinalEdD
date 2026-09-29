package riftforge.ui.gui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

/**
 * Fondo a sangre completa de una zona de la interfaz: dibuja la imagen
 * escalándola en formato «cover» (sobresale por el lado más largo y se
 * recorta el otro) y superpone una veladura oscura para que el texto y las
 * cartas se sigan leyendo sobre la ilustración. Si la imagen no existe, cae
 * al degradado por defecto.
 */
final class Backdrop extends JPanel {
    private final Image image;
    private final Color top;
    private final Color bottom;
    private final int overlay;

    Backdrop(Image image, Color top, Color bottom, int overlay) {
        this.image = image;
        this.top = top;
        this.bottom = bottom;
        this.overlay = overlay;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        int width = getWidth();
        int height = getHeight();
        if (image != null && width > 0 && height > 0) {
            int iw = image.getWidth(null);
            int ih = image.getHeight(null);
            if (iw > 0 && ih > 0) {
                double cover = Math.max((double) width / iw, (double) height / ih);
                int dw = (int) Math.ceil(iw * cover);
                int dh = (int) Math.ceil(ih * cover);
                g.drawImage(image, (width - dw) / 2, (height - dh) / 2, dw, dh, null);
            }
        } else {
            g.setPaint(new GradientPaint(0, 0, top, 0, height, bottom));
        }
        g.setColor(new Color(0, 0, 0, overlay));
        g.fillRect(0, 0, width, height);
        g.dispose();
    }

    @Override
    public void update(Graphics g) {
        paintComponent(g);
    }
}
