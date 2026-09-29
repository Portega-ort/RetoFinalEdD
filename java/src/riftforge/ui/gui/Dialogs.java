package riftforge.ui.gui;

import riftforge.app.Startup;
import riftforge.model.BattleEvent;
import riftforge.model.Card;
import riftforge.structures.Tree;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Diálogos de solo lectura que se pueden abrir desde cualquier pantalla: el
 * catálogo de cartas (ordenado por coste o por tipo), el historial de eventos,
 * el árbol de evoluciones y las reglas del duelo. Viven aparte para que la
 * pantalla de inicio y la ventana del duelo ofrezcan exactamente la misma
 * información sin duplicar el código que la compone.
 */
final class Dialogs {
    private static final int CARD_W = 150;
    private static final int CARD_H = 210;
    private static final int COLUMNS = 6;
    private static final int GAP = 12;

    private Dialogs() {
    }

    /** Un bloque del catálogo: su encabezado y las cartas que lo forman. */
    private record Section(String label, List<Card> cards) {
    }

    /**
     * Catálogo de colección: las 40 cartas con su imagen, su nombre, su coste y
     * sus estadísticas, agrupadas por coste de maná o por tipo. Las dos listas
     * llegan ya ordenadas con el Insertion Sort propio y el conmutador de arriba
     * solo decide cuál se pinta, así que cambiar de vista no vuelve a ordenar
     * nada.
     */
    static void showCatalog(Component parent, List<Card> byCost, List<Card> byType) {
        JPanel collection = new JPanel();
        collection.setLayout(new BoxLayout(collection, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(collection);
        scroll.setPreferredSize(new Dimension(980, 680));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(24);

        JButton costButton = new JButton("Coste");
        JButton typeButton = new JButton("Tipo");
        Runnable costView = () -> {
            mark(costButton, typeButton);
            paintCollection(collection, byCost, true);
        };
        Runnable typeView = () -> {
            mark(typeButton, costButton);
            paintCollection(collection, byType, false);
        };
        costButton.addActionListener(e -> costView.run());
        typeButton.addActionListener(e -> typeView.run());

        JLabel caption = new JLabel("Ordenar por:");
        caption.setFont(Typeface.body(Font.BOLD, 14));
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        toolbar.add(caption);
        toolbar.add(costButton);
        toolbar.add(typeButton);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(toolbar, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);

        collection.add(loading());
        Theme.styleButton(costButton, Theme.BTN_SELECTED, 14);
        Theme.styleButton(typeButton, Theme.BTN_SECONDARY, 14);
        costButton.setEnabled(false);
        typeButton.setEnabled(false);
        // El hilo arranca antes de abrir el dialogo: setVisible de un modal
        // bloquea a quien lo llama hasta que se cierra, asi que si se lanzara
        // despues solo empezaria con el dialogo ya cerrado.
        warmThenPaint(costView, byCost);
        open(parent, "Catálogo de cartas", content);
    }

    private static JLabel loading() {
        JLabel label = new JLabel("Cargando ilustraciones de las 40 cartas…");
        label.setAlignmentX(0f);
        label.setBorder(BorderFactory.createEmptyBorder(16, 4, 8, 4));
        label.setFont(Typeface.body(Font.PLAIN, 14));
        label.setForeground(Theme.TEXT_DIM);
        return label;
    }

    /**
     * Decodificar 40 JPEG en el hilo de Swing congelaría la ventana casi dos
     * segundos, así que el calentamiento se hace en un hilo aparte y la
     * conversación vuelve a Swing para pintar. Es un demonio: si el jugador
     * cierra el diálogo antes de tiempo, no impide que la JVM termine.
     */
    private static void warmThenPaint(Runnable paint, List<Card> byCost) {
        Thread worker = new Thread(() -> {
            CardView.warmArt(byCost, CARD_W, CARD_H);
            SwingUtilities.invokeLater(paint);
        }, "riftforge-catalogo-warm");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Rellena la colección con las cartas ya ordenadas, cortando la lista en
     * bloques según el criterio activo. Las especiales se dibujan con el halo
     * de prioridad que usan en la mano.
     */
    private static void paintCollection(JPanel collection, List<Card> cards, boolean byCost) {
        collection.removeAll();
        for (Section section : sections(cards, byCost)) {
            JLabel header = new JLabel(section.label());
            header.setAlignmentX(0f);
            header.setFont(Typeface.display(Font.BOLD, 15));
            header.setForeground(Theme.ACCENT);
            header.setBorder(BorderFactory.createEmptyBorder(16, 4, 8, 4));

            JPanel grid = new JPanel(new GridLayout(0, COLUMNS, GAP, GAP));
            grid.setOpaque(false);
            grid.setAlignmentX(0f);
            for (Card card : section.cards()) grid.add(new CardView(card, CARD_W, CARD_H, card.special()));

            collection.add(header);
            collection.add(grid);
            collection.add(Box.createVerticalStrut(12));
        }
        collection.revalidate();
        collection.repaint();
    }

    /**
     * Corta la lista —que llega ordenada— en bloques consecutivos del mismo
     * coste o del mismo tipo. No reordena nada: solo agrupa lo que ya viene
     * junto, igual que harían los encabezados de la vista de texto.
     */
    private static List<Section> sections(List<Card> cards, boolean byCost) {
        List<Section> out = new ArrayList<>();
        int i = 0;
        while (i < cards.size()) {
            int j = i + 1;
            while (j < cards.size() && sameBlock(cards.get(i), cards.get(j), byCost)) j++;
            List<Card> block = new ArrayList<>(cards.subList(i, j));
            out.add(new Section(blockLabel(cards.get(i), block.size(), byCost), block));
            i = j;
        }
        return out;
    }

    private static boolean sameBlock(Card a, Card b, boolean byCost) {
        return byCost ? a.manaCost() == b.manaCost() : a.type() == b.type();
    }

    private static String blockLabel(Card first, int count, boolean byCost) {
        String key = byCost ? "COSTE " + first.manaCost() : first.type().name();
        return key + "   ·   " + count + (count == 1 ? " carta" : " cartas");
    }

    /**
     * Deja resaltado el botón de la vista activa y el otro en tono neutro, y los
     * habilita: solo se reactivan cuando ya hay ilustraciones que pintar, para
     * que nadie pida un cambio de vista mientras el calentamiento sigue en curso.
     */
    private static void mark(JButton active, JButton other) {
        Theme.styleButton(active, Theme.BTN_SELECTED, 14);
        Theme.styleButton(other, Theme.BTN_SECONDARY, 14);
        active.setEnabled(true);
        other.setEnabled(true);
    }

    static void showHistory(Component parent, List<BattleEvent> events) {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(Typeface.mono(Font.PLAIN, 13));
        StringBuilder sb = new StringBuilder("HISTORIAL DE LA PARTIDA (lista doble)\n");
        for (BattleEvent event : events) {
            sb.append("[").append(String.format("%3d", event.turn())).append("] ").append(event.description()).append("\n");
        }
        area.setText(sb.toString());
        showText(parent, "Historial de eventos", area, 680, 540);
    }

    static void showEvolutions(Component parent, Tree<Card> tree) {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(Typeface.mono(Font.PLAIN, 13));
        StringBuilder sb = new StringBuilder("ÁRBOL DE EVOLUCIONES\n");
        Tree.TreeNode<Card> root = tree.root();
        if (root != null) buildTree(sb, root, 0);
        area.setText(sb.toString());
        showText(parent, "Evoluciones (árbol)", area, 560, 540);
    }

    static void showRules(Component parent) {
        String rules = "CÓMO SE JUEGA\n"
                + "\n"
                + "Cada duelista recibe un mazo de 20 cartas y empieza con "
                + Startup.STARTING_LIFE + " de vida y " + Startup.STARTING_MANA + " de maná.\n"
                + "Gana quien deje al rival sin vida.\n"
                + "\n"
                + "EL TURNO\n"
                + "  1. Se roba la carta de más arriba del mazo.\n"
                + "  2. Se juega la carta de la mano: la recién invocada no ataca ese turno.\n"
                + "  3. Ataca con el campo. El daño lo reciben primero las criaturas\n"
                + "     rivales que bloquean y el exceso se pierde.\n"
                + "  4. El rival solo recibe daño si su campo queda vacío.\n"
                + "\n"
                + "GRIETAS\n"
                + "Cada turno rota una grieta: la que está activa otorga una bonificación\n"
                + "de ataque a las criaturas de su elemento. Hay una grieta por cada\n"
                + "elemento, así que el ritmo del duelo depende del orden en que rotan.\n"
                + "\n"
                + "FICHAS\n"
                + "Las cartas con marca ★ son especiales y se resuelven antes que las\n"
                + "normales cuando se juega una carta.";
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(Typeface.body(Font.PLAIN, 14));
        area.setText(rules);
        showText(parent, "Reglas del duelo", area, 620, 520);
    }

    private static void showText(Component parent, String title, JTextArea area, int width, int height) {
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(width, height));
        open(parent, title, scroll);
    }

    private static void open(Component parent, String title, JComponent content) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), title,
                JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setContentPane(content);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private static void buildTree(StringBuilder sb, Tree.TreeNode<Card> node, int level) {
        for (int i = 0; i < level; i++) sb.append("   ");
        sb.append(node.data().name()).append(node.data().special() ? " ★" : "").append("\n");
        for (Tree.TreeNode<Card> child : node.children().forward()) buildTree(sb, child, level + 1);
    }
}
