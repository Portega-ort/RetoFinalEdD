package riftforge.ui.gui;

import riftforge.model.Card;
import riftforge.model.CardType;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;

/**
 * Vista de una carta: la fotografía cubre toda la carta (recorte <em>cover</em>)
 * y sobre ella se dibujan la barra de nombre, el coste, el ATK/DEF y la
 * habilidad. Todo el dibujado se escala de forma uniforme al tamaño real del
 * componente, de modo que nunca se ve distorsionado ni recortado por el layout.
 */
public final class CardView extends JComponent {
    private final Card card;
    private final Image art;
    private final Color element;
    private final boolean glow;
    private final int prefW;
    private final int prefH;

    public CardView(Card card, int width, int height) {
        this(card, width, height, false);
    }

    public CardView(Card card, int width, int height, boolean glow) {
        this.card = card;
        this.glow = glow;
        this.prefW = width;
        this.prefH = height;
        this.element = Theme.element(card.element());
        this.art = ImageStore.forCard(card.id(), Math.max(96, width * 2), Math.max(144, height * 2));
        setPreferredSize(new Dimension(width, height));
        setToolTipText(buildTooltip());
    }

    private String buildTooltip() {
        return "<html><b>" + card.name() + "</b><br>"
                + "Tipo: " + card.type() + " · Elemento: " + card.element()
                + "<br>Coste: " + card.manaCost() + " · ATK: " + card.attack() + " · DEF: " + card.health()
                + (card.special() ? "<br><b>HABILIDAD ESPECIAL</b>" : "")
                + (card.specialAbility() == null ? "" : "<br>" + card.specialAbility().replace("\n", "<br>"))
                + "</html>";
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        if (prefW <= 0 || prefH <= 0) return;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        double scale = Math.min((double) getWidth() / prefW, (double) getHeight() / prefH);
        if (scale <= 0) {
            g.dispose();
            return;
        }
        int w = (int) Math.round(prefW * scale);
        int h = (int) Math.round(prefH * scale);
        g.translate((getWidth() - w) / 2, (getHeight() - h) / 2);
        g.scale(scale, scale);

        paintBackdrop(g);
        if (glow) paintGlow(g);
        paintBorder(g);
        paintName(g);
        paintAbility(g);
        paintChips(g);
        g.dispose();
    }

    private void paintBackdrop(Graphics2D g) {
        int arc = arc();
        if (art != null) {
            int iw = art.getWidth(null);
            int ih = art.getHeight(null);
            if (iw > 0 && ih > 0) {
                double cover = Math.max((double) prefW / iw, (double) prefH / ih);
                int dw = (int) Math.round(iw * cover);
                int dh = (int) Math.round(ih * cover);
                g.drawImage(art, (prefW - dw) / 2, (prefH - dh) / 2, dw, dh, null);
            } else {
                paintPlaceholder(g);
            }
        } else {
            paintPlaceholder(g);
        }
        g.clip(new java.awt.geom.RoundRectangle2D.Float(0, 0, prefW, prefH, arc, arc));
        g.setColor(new Color(0, 0, 0, 70));
        g.fillRect(0, 0, prefW, prefH);
    }

    private void paintPlaceholder(Graphics2D g) {
        GradientPaint gradient = new GradientPaint(0, 0, element, 0, prefH, element.darker());
        g.setPaint(gradient);
        g.fillRoundRect(0, 0, prefW, prefH, arc(), arc());
        g.setColor(new Color(255, 255, 255, 220));
        g.setFont(Typeface.display(Font.BOLD, Math.max(24, prefW / 2)));
        drawCentered(g, String.valueOf(card.element().name().charAt(0)), prefW / 2f, prefH / 2f + 14);
    }

    private void paintGlow(Graphics2D g) {
        g.setColor(new Color(element.getRed(), element.getGreen(), element.getBlue(), 90));
        g.setStroke(new BasicStroke(6f));
        g.drawRoundRect(3, 3, prefW - 7, prefH - 7, arc(), arc());
    }

    private void paintBorder(Graphics2D g) {
        g.setColor(element);
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(1, 1, prefW - 3, prefH - 3, arc(), arc());
    }

    private void paintName(Graphics2D g) {
        int chip = chip();
        int nameH = nameHeight();
        int pad = 6;
        int x = pad + chip + 4;
        int y = pad;
        int width = prefW - x - pad;
        if (width <= 0) return;

        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(x, y, width, nameH, 12, 12);
        g.setColor(Color.WHITE);
        g.setFont(Typeface.body(Font.BOLD, nameFont()));
        String label = fitText(card.name(), width - 12, g.getFontMetrics());
        drawCentered(g, label, x + width / 2f, y + nameH / 2f + ascent(g) / 2f);
    }

    private void paintAbility(Graphics2D g) {
        int chip = chip();
        int abilityH = abilityHeight();
        int x = 8;
        int y = prefH - chip - abilityH - 10;
        int width = prefW - 16;
        if (abilityH <= 6) return;

        g.setColor(new Color(0, 0, 0, 125));
        g.fillRoundRect(x, y, width, abilityH, 12, 12);
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(255, 255, 255, 60));
        g.drawRoundRect(x, y, width, abilityH, 12, 12);

        String text = card.specialAbility();
        if (text == null || text.isBlank()) text = card.type() == null ? "" : "(" + card.type() + ")";
        g.setColor(Color.WHITE);
        g.setFont(Typeface.body(Font.PLAIN, abilityFont()));
        int lines = Math.max(1, abilityH / (g.getFontMetrics().getHeight() + 1));
        drawWrapped(g, text, x + 8, y + abilityH - 8, width - 16, lines);
    }

    private void paintChips(Graphics2D g) {
        int chip = chip();
        int pad = 6;
        int statY = prefH - chip - pad;
        String cost = pad(String.valueOf(card.manaCost()));
        String atk = pad(String.valueOf(card.attack()));
        String def = pad(String.valueOf(card.health()));
        Color defFill = new Color(0x2E7D32);
        if (card.type() == CardType.CRIATURA && card.damageTaken() > 0) {
            def = card.remainingHealth() + "/" + card.health();
            defFill = new Color(0xEF6C00);
        }

        paintChip(g, pad, pad, chip, cost, new Color(0x1565C0), "COSTE");
        paintChip(g, pad, statY, chip, atk, new Color(0xC62828), "ATK");
        paintChip(g, prefW - pad - chip, statY, chip, def, defFill, "DEF");
    }

    private void paintChip(Graphics2D g, int x, int y, int size, String value, Color fill, String label) {
        g.setColor(new Color(0, 0, 0, 80));
        g.fillOval(x + 2, y + 3, size, size);
        g.setColor(fill);
        g.fillOval(x, y, size, size);
        g.setColor(new Color(255, 255, 255, 240));
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval(x + 1, y + 1, size - 2, size - 2);

        g.setFont(Typeface.body(Font.BOLD, chipFont()));
        drawCentered(g, value, x + size / 2f, y + size / 2f + ascent(g) / 2f - 1);

        g.setFont(Typeface.body(Font.BOLD, Math.max(8, chipFont() / 2)));
        drawCentered(g, label, x + size / 2f, y + size - Math.max(9, chipFont() / 2) + 9);
    }

    private void drawWrapped(Graphics2D g, String text, int x, int baseline, int maxWidth, int maxLines) {
        FontMetrics metrics = g.getFontMetrics();
        if (metrics.stringWidth(text) <= maxWidth) {
            g.drawString(text, x, baseline);
            return;
        }
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        int drawn = 0;
        for (String word : words) {
            String probe = line.isEmpty() ? word : line + " " + word;
            if (metrics.stringWidth(probe) > maxWidth) {
                g.drawString(fitText(line.toString(), maxWidth, metrics), x, baseline - drawn * metrics.getHeight());
                drawn++;
                if (drawn >= maxLines) break;
                line.setLength(0);
            }
            if (line.isEmpty()) line.append(word);
            else line.append(' ').append(word);
        }
        if (line.length() > 0 && drawn < maxLines) {
            g.drawString(fitText(line.toString(), maxWidth, metrics), x, baseline - drawn * metrics.getHeight());
        }
    }

    private String fitText(String text, int maxWidth, FontMetrics metrics) {
        if (metrics.stringWidth(text) <= maxWidth) return text;
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (metrics.stringWidth(sb.toString() + c) > maxWidth - 4) break;
            sb.append(c);
        }
        return sb.toString().trim() + "…";
    }

    private String pad(String value) {
        return value.length() == 1 ? "0" + value : value;
    }

    private int ascent(Graphics2D g) {
        return g.getFontMetrics().getAscent();
    }

    private void drawCentered(Graphics2D g, String text, float centerX, float baselineY) {
        float x = centerX - g.getFontMetrics().stringWidth(text) / 2f;
        g.setColor(new Color(0, 0, 0, 120));
        g.drawString(text, x + 1, baselineY + 1);
        g.setColor(Color.WHITE);
        g.drawString(text, x, baselineY);
    }

    private int arc() {
        return Math.max(10, Math.min(20, Math.min(prefW, prefH) / 6));
    }

    private int chip() {
        return Math.max(24, Math.min(38, prefW / 4));
    }

    private int nameHeight() {
        return Math.max(18, Math.min(26, prefH / 12));
    }

    private int nameFont() {
        return Math.max(12, Math.min(16, prefW / 20));
    }

    private int chipFont() {
        return Math.max(13, Math.min(18, prefW / 15));
    }

    private int abilityHeight() {
        return Math.max(18, Math.min(48, prefH / 8));
    }

    private int abilityFont() {
        return Math.max(10, Math.min(13, prefW / 26));
    }
}