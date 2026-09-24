package riftforge.ui.gui;

import riftforge.app.Startup;
import riftforge.data.CardDatabase;
import riftforge.engine.GameEngine;
import riftforge.model.BattleEvent;
import riftforge.model.Card;
import riftforge.model.Player;
import riftforge.structures.Tree;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/** Ventana principal del duelo Rift-Forge TCG: dos zonas de campo, maná/vida y mano. */
public final class GameWindow extends JFrame {
    private static final int FIELD_CARD_W = 140;
    private static final int FIELD_CARD_H = 196;
    private static final int HAND_CARD_W = 300;
    private static final int HAND_CARD_H = 414;

    private GameEngine game;
    private boolean over;

    private final JLabel turnLabel = new JLabel();
    private final JLabel riftLabel = new JLabel();
    private final JTextArea log = new JTextArea();
    private final JPanel opponentZone = new JPanel(new BorderLayout());
    private final JPanel activeZone = new JPanel(new BorderLayout());
    private final JPanel handZone = new JPanel();
    private final JButton playButton = new JButton("Jugar la carta");
    private final JButton discardButton = new JButton("Descartar");

    public GameWindow() {
        super("Rift-Forge TCG");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Theme.applyLookAndFeel();
        wireActions();
        resetMatch();
        buildUi();
        setMinimumSize(new Dimension(1180, 800));
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        SwingUtilities.invokeLater(this::refresh);
    }

    private void wireActions() {
        playButton.addActionListener(e -> play(true));
        discardButton.addActionListener(e -> play(false));
    }

    private void resetMatch() {
        try {
            List<CardDatabase.CardRow> rows = CardDatabase.load();
            game = Startup.newEngine();
            List<Card>[] halves = Startup.splitDecks(rows);
            Player p1 = new Player("Jugador 1", Startup.STARTING_MANA, Startup.STARTING_LIFE);
            Player p2 = new Player("Jugador 2", Startup.STARTING_MANA, Startup.STARTING_LIFE);
            for (Card card : Startup.shuffled(halves[0])) p1.deck().push(card);
            for (Card card : Startup.shuffled(halves[1])) p2.deck().push(card);
            game.addPlayer(p1);
            game.addPlayer(p2);
            over = false;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se pudo cargar la ficha de cartas:\n" + e, "Rift-Forge TCG", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel stripe = new JPanel(new GridLayout(2, 1));
        stripe.setOpaque(false);

        JPanel rowTop = new JPanel(new BorderLayout());
        rowTop.setOpaque(false);
        JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        title.setOpaque(false);
        JLabel brand = new JLabel("RIFT-FORGE TCG");
        brand.setFont(Typeface.display(Font.BOLD, 26));
        brand.setForeground(Theme.ACCENT);
        JLabel sub = new JLabel("  bloqueo estricto: el daño se queda en las cartas · la recién invocada no ataca");
        sub.setFont(Typeface.body(Font.PLAIN, 13));
        sub.setForeground(Theme.TEXT_DIM);
        title.add(brand);
        title.add(sub);
        rowTop.add(title, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        buttons.setOpaque(false);
        buttons.add(menuButton("Catálogo", this::showCatalog));
        buttons.add(menuButton("Historial", this::showHistory));
        buttons.add(menuButton("Evoluciones", this::showEvolutions));
        buttons.add(menuButton("Barajar", this::shuffleDecks));
        buttons.add(menuButton("Reiniciar", this::restart));
        buttons.add(menuButton("Salir", () -> System.exit(0)));
        rowTop.add(buttons, BorderLayout.EAST);
        stripe.add(rowTop);

        JPanel rowBottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 2));
        rowBottom.setOpaque(false);
        turnLabel.setFont(Typeface.body(Font.BOLD, 16));
        turnLabel.setForeground(Theme.TEXT);
        riftLabel.setFont(Typeface.body(Font.BOLD, 16));
        riftLabel.setForeground(Theme.TEXT);
        rowBottom.add(turnLabel);
        rowBottom.add(riftLabel);
        stripe.add(rowBottom);

        JPanel header = new GradientPanel();
        header.setLayout(new BorderLayout());
        header.add(stripe, BorderLayout.CENTER);
        return header;
    }

    private JButton menuButton(String label, Runnable action) {
        JButton button = new JButton(label);
        Theme.styleSecondary(button);
        button.addActionListener(e -> action.run());
        return button;
    }

    private JPanel buildCenter() {
        JPanel center = new Backdrop(BackgroundStore.table(), Theme.BG_TOP, Theme.BG_BOTTOM, 110);
        center.setLayout(new BorderLayout());

        opponentZone.setOpaque(false);
        opponentZone.setBorder(BorderFactory.createEmptyBorder(6, 12, 2, 12));
        center.add(opponentZone, BorderLayout.NORTH);

        JPanel middle = new Backdrop(BackgroundStore.board(), Theme.PANEL, Theme.BG_BOTTOM, 70);
        middle.setLayout(new BorderLayout(10, 0));

        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setFont(Typeface.mono(Font.PLAIN, 13));
        log.setOpaque(false);
        log.setForeground(Theme.TEXT);
        JScrollPane logScroll = new JScrollPane(log);
        logScroll.setOpaque(false);
        logScroll.getViewport().setOpaque(false);
        logScroll.setBorder(BorderFactory.createEmptyBorder());
        logScroll.setPreferredSize(new Dimension(430, 150));
        JPanel logPan = new Backdrop(BackgroundStore.log(), Theme.PANEL, Theme.BG_BOTTOM, 100);
        logPan.setLayout(new BorderLayout());
        logPan.setBorder(BorderFactory.createTitledBorder("Bitácora del duelo"));
        logPan.add(logScroll, BorderLayout.CENTER);
        middle.add(logPan, BorderLayout.WEST);

        handZone.setLayout(new BorderLayout());
        handZone.setOpaque(false);
        handZone.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 12));
        middle.add(handZone, BorderLayout.CENTER);
        center.add(middle, BorderLayout.CENTER);

        activeZone.setOpaque(false);
        activeZone.setBorder(BorderFactory.createEmptyBorder(2, 12, 10, 12));
        center.add(activeZone, BorderLayout.SOUTH);
        return center;
    }

    private void refresh() {
        List<Player> order = game.turnOrder();
        Player active = game.nextPlayer();
        Player rival = order.stream().filter(p -> p != active).findFirst().orElse(null);

        turnLabel.setText("Turno " + game.turn() + "  ·  Turno de " + active.name());
        riftLabel.setText("Grieta actual: " + game.activeRift().name()
                + "  (+" + game.activeRift().attackBonus() + " " + game.activeRift().bonusElement() + ")");
        riftLabel.setForeground(Theme.element(game.activeRift().bonusElement()));

        rebuildZone(opponentZone, rival, false);
        rebuildZone(activeZone, active, true);
        rebuildHand(active);

        log.setCaretPosition(log.getDocument().getLength());
        revalidate();
        repaint();
    }

    private void rebuildZone(JPanel container, Player player, boolean isActive) {
        container.removeAll();
        if (player == null) {
            container.add(new JLabel("Sin rival"));
            container.revalidate();
            container.repaint();
            return;
        }
        JPanel zone = new GlassPanel(Theme.PANEL, 205);
        zone.setLayout(new BorderLayout(0, 4));
        zone.setBorder(BorderFactory.createCompoundBorder(
                isActive ? BorderFactory.createLineBorder(Theme.ACCENT, 2)
                        : BorderFactory.createLineBorder(Theme.PANEL_LIGHT, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        zone.add(titleBar(player, isActive), BorderLayout.NORTH);
        zone.add(bars(player), BorderLayout.WEST);
        zone.add(fieldRow(player), BorderLayout.CENTER);

        container.add(zone, BorderLayout.CENTER);
        container.revalidate();
        container.repaint();
    }

    private JPanel titleBar(Player player, boolean isActive) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        JLabel name = new JLabel((isActive ? "▶ " : "") + player.name() + (isActive ? "  (activo)" : ""));
        name.setFont(Typeface.body(Font.BOLD, 16));
        name.setForeground(isActive ? Theme.ACCENT : Theme.TEXT);
        bar.add(name, BorderLayout.WEST);

        JLabel stats = new JLabel("Mazo " + player.deck().size() + "  ·  Cementerio " + player.graveyard().size()
                + "  ·  Blindaje " + player.shieldPool()
                + (player.fieldAttackBonus() > 0 ? "  ·  Mejoras +" + player.fieldAttackBonus() + " ATK" : ""));
        stats.setFont(Typeface.body(Font.PLAIN, 14));
        stats.setForeground(Theme.TEXT_DIM);
        bar.add(stats, BorderLayout.EAST);
        return bar;
    }

    private JPanel bars(Player player) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.add(bar("Vida", player.life(), Startup.STARTING_LIFE, player.life() <= 5 ? Theme.LIFE_LOW : Theme.LIFE));
        panel.add(Box.createVerticalStrut(3));
        panel.add(bar("Maná", player.mana(), 10, Theme.MANA));
        return panel;
    }

    private JPanel bar(String label, int value, int max, Color color) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel caption = new JLabel(label);
        caption.setFont(Typeface.body(Font.BOLD, 11));
        caption.setForeground(Theme.TEXT);
        row.add(caption, BorderLayout.NORTH);
        JProgressBar bar = new JProgressBar(0, max);
        bar.setValue(value);
        bar.setStringPainted(true);
        bar.setForeground(color);
        bar.setBackground(Theme.PANEL_LIGHT);
        bar.setFont(Typeface.body(Font.BOLD, 12));
        bar.setPreferredSize(new Dimension(140, 20));
        row.add(bar, BorderLayout.SOUTH);
        return row;
    }

    private JPanel fieldRow(Player player) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        row.setOpaque(false);
        List<Card> creatures = new ArrayList<>();
        for (Card card : player.field()) if (card != null) creatures.add(card);
        if (creatures.isEmpty()) {
            JLabel empty = new JLabel("Campo vacío — juega una criatura.");
            empty.setFont(Typeface.body(Font.PLAIN, 13));
            empty.setForeground(Theme.TEXT_DIM);
            row.add(empty);
            return row;
        }
        for (Card card : creatures) row.add(new CardView(card, FIELD_CARD_W, FIELD_CARD_H));
        return row;
    }

    private void rebuildHand(Player active) {
        handZone.removeAll();

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        Card next = game.previewNextCard();
        JLabel caption = new JLabel("Próxima carta", SwingConstants.CENTER);
        caption.setFont(Typeface.body(Font.BOLD, 15));
        caption.setForeground(Theme.ACCENT);
        left.add(caption, BorderLayout.NORTH);
        if (next == null) {
            left.add(new JLabel("Sin cartas para robar.", SwingConstants.CENTER), BorderLayout.CENTER);
            playButton.setEnabled(false);
            discardButton.setEnabled(false);
        } else {
            left.add(new CardView(next, HAND_CARD_W, HAND_CARD_H, true), BorderLayout.CENTER);
            playButton.setEnabled(!over && active.canPay(next));
            discardButton.setEnabled(!over);
        }
        handZone.add(left, BorderLayout.CENTER);

        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.setOpaque(false);
        actions.add(Box.createVerticalGlue());
        Theme.styleButton(playButton, Theme.BTN_PLAY, 18);
        actions.add(playButton);
        actions.add(Box.createVerticalStrut(12));
        Theme.styleButton(discardButton, Theme.BTN_DISCARD, 16);
        actions.add(discardButton);
        actions.add(Box.createVerticalStrut(20));
        JLabel hint = new JLabel("<html><center>Las criaturas bloquean: tu ataque<br>"
                + "golpea primero a las criaturas<br>"
                + "rivales y el exceso se pierde.<br>"
                + "El rival solo recibe daño si su<br>"
                + "campo queda vacío. La recién<br>"
                + "invocada no ataca ese turno.</center></html>");
        hint.setForeground(Theme.TEXT_DIM);
        hint.setFont(Typeface.body(Font.PLAIN, 13));
        actions.add(hint);
        actions.add(Box.createVerticalGlue());
        handZone.add(actions, BorderLayout.EAST);

        handZone.revalidate();
        handZone.repaint();
    }

    private void play(boolean wantsToPlay) {
        if (over) return;
        GameEngine.TurnResult result = game.playTurn(wantsToPlay);
        log.append(">>> " + result.active().name() + " → " + result.action() + "\n");
        trimLog();
        activeZone.repaint();
        if (result.target().isDefeated()) {
            over = true;
            refresh();
            JOptionPane.showMessageDialog(this, "¡" + result.active().name() + " gana el duelo!");
            return;
        }
        refresh();
    }

    private void trimLog() {
        try {
            while (log.getLineCount() > 300) {
                int end = log.getLineEndOffset(0);
                log.replaceRange("", 0, end);
            }
        } catch (Exception ignored) {
            // límite aproximado de la bitácora
        }
    }

    private void restart() {
        log.setText("");
        resetMatch();
        refresh();
    }

    /** Baraja de nuevo el orden de robo de ambos mazos (Fisher–Yates propio). */
    private void shuffleDecks() {
        for (Player player : game.turnOrder()) {
            List<Card> cards = new ArrayList<>();
            while (!player.deck().isEmpty()) cards.add(player.deck().pop());
            for (Card card : Startup.shuffled(cards)) player.deck().push(card);
        }
        refresh();
    }

    private void showCatalog() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(Typeface.mono(Font.PLAIN, 13));
        StringBuilder sb = new StringBuilder("CATÁLOGO ORDENADO POR COSTE (Insertion Sort propio)\n");
        int lastCost = -1;
        for (Card card : game.catalogSortedByCost()) {
            if (card.manaCost() != lastCost) {
                sb.append("\n== coste ").append(card.manaCost()).append(" ==\n");
                lastCost = card.manaCost();
            }
            sb.append(String.format("%-4s %-30s %-10s %-9s ATK %2d DEF %2d%s%n",
                    card.id(), tr(card.name(), 30), card.type(), card.element(), card.attack(), card.health(),
                    card.special() ? " ★" : ""));
        }
        area.setText(sb.toString());
        showDialog("Catálogo ordenado por coste", area, 580, 540);
    }

    private void showHistory() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(Typeface.mono(Font.PLAIN, 13));
        StringBuilder sb = new StringBuilder("HISTORIAL DE LA PARTIDA (lista doble)\n");
        for (BattleEvent event : game.history().forward()) {
            sb.append("[").append(String.format("%3d", event.turn())).append("] ").append(event.description()).append("\n");
        }
        area.setText(sb.toString());
        showDialog("Historial de eventos", area, 680, 540);
    }

    private void showEvolutions() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(Typeface.mono(Font.PLAIN, 13));
        StringBuilder sb = new StringBuilder("ÁRBOL DE EVOLUCIONES\n");
        Tree.TreeNode<Card> root = game.evolutionTree().root();
        if (root != null) buildTree(sb, root, 0);
        area.setText(sb.toString());
        showDialog("Evoluciones (árbol)", area, 560, 540);
    }

    private void buildTree(StringBuilder sb, Tree.TreeNode<Card> node, int level) {
        for (int i = 0; i < level; i++) sb.append("   ");
        sb.append(node.data().name()).append(node.data().special() ? " ★" : "").append("\n");
        for (Tree.TreeNode<Card> child : node.children().forward()) buildTree(sb, child, level + 1);
    }

    private void showDialog(String title, JTextArea area, int width, int height) {
        JDialog dialog = new JDialog(this, title, true);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(width, height));
        dialog.setContentPane(scroll);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private static String tr(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    /** Fondo degradado para la cabecera. */
    private static final class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setPaint(new GradientPaint(0, 0, Theme.BG_TOP, getWidth(), getHeight(), Theme.BG_BOTTOM));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.dispose();
        }

        @Override
        public void update(Graphics g) {
            paintComponent(g);
        }
    }

    /**
     * Fondo con imagen (tablero o mesa) a sangre completa; sin imagen usa el
     * degradado por defecto. Se aplica una veladura oscura para que las cartas
     * y el texto sigan leyéndose sobre la ilustración.
     */
    private static final class Backdrop extends JPanel {
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

    /**
     * Panel translúcido con las esquinas redondeadas para que el tablero o la
     * mesa de fondo se vean alrededor de cada zona de juego.
     */
    private static final class GlassPanel extends JPanel {
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
}