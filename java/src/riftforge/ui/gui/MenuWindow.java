package riftforge.ui.gui;

import riftforge.app.Startup;
import riftforge.engine.GameEngine;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;

/**
 * Pantalla de inicio: se muestra antes del duelo, con el fondo
 * {@code resources/backgrounds/fondo_inicio.*} y el menú principal. Al elegir
 * «Jugar» se abre la {@link GameWindow} y esta pantalla se cierra.
 *
 * <p>«Catálogo» y «Evoluciones» también se pueden consultar sin empezar la
 * partida: para eso se arma un motor sin jugadores (solo con la ficha de cartas
 * y las grietas) la primera vez que se pulsa cualquiera de los dos.
 */
public final class MenuWindow extends JFrame {
    private static final int BUTTON_W = 300;
    private static final int BUTTON_H = 52;

    private GameEngine preview;

    public MenuWindow() {
        super("Rift-Forge TCG");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Theme.applyLookAndFeel();
        buildUi();
        setMinimumSize(new Dimension(900, 560));
        setSize(1120, 720);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void buildUi() {
        JPanel background = new Backdrop(BackgroundStore.inicio(), Theme.BG_TOP, Theme.BG_BOTTOM, 130);
        background.setLayout(new BorderLayout(0, 16));
        background.setBorder(BorderFactory.createEmptyBorder(40, 40, 28, 40));
        background.add(buildTitle(), BorderLayout.NORTH);
        background.add(buildMenu(), BorderLayout.CENTER);
        background.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(background);
    }

    private JPanel buildTitle() {
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel brand = new JLabel("RIFT-FORGE TCG", JLabel.CENTER);
        brand.setAlignmentX(0.5f);
        brand.setFont(Typeface.display(Font.BOLD, 46));
        brand.setForeground(Theme.ACCENT);
        box.add(brand);

        box.add(Box.createVerticalStrut(4));

        JLabel subtitle = new JLabel("Duelo de cartas sobre estructuras de datos propias", JLabel.CENTER);
        subtitle.setAlignmentX(0.5f);
        subtitle.setFont(Typeface.body(Font.PLAIN, 16));
        subtitle.setForeground(Theme.TEXT_DIM);
        box.add(subtitle);
        return box;
    }

    private JPanel buildMenu() {
        JPanel frame = new JPanel(new GridBagLayout());
        frame.setOpaque(false);

        JPanel column = new GlassPanel(Theme.PANEL, 205);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 40), 1),
                BorderFactory.createEmptyBorder(22, 26, 22, 26)));

        column.add(Box.createVerticalGlue());
        column.add(menuButton("JUGAR", Theme.BTN_PLAY, 22, this::startGame));
        column.add(Box.createVerticalStrut(10));
        column.add(menuButton("CATÁLOGO", Theme.BTN_SECONDARY, 17, this::showCatalog));
        column.add(Box.createVerticalStrut(10));
        column.add(menuButton("EVOLUCIONES", Theme.BTN_SECONDARY, 17, this::showEvolutions));
        column.add(Box.createVerticalStrut(10));
        column.add(menuButton("CÓMO SE JUEGA", Theme.BTN_SECONDARY, 17, () -> Dialogs.showRules(this)));
        column.add(Box.createVerticalStrut(10));
        column.add(menuButton("SALIR", Theme.BTN_DISCARD, 17, () -> System.exit(0)));
        column.add(Box.createVerticalGlue());

        frame.add(column);
        return frame;
    }

    private JButton menuButton(String label, Color color, int fontSize, Runnable action) {
        JButton button = new JButton(label);
        Theme.styleButton(button, color, fontSize);
        button.setAlignmentX(0.5f);
        button.setPreferredSize(new Dimension(BUTTON_W, BUTTON_H));
        button.setMaximumSize(new Dimension(BUTTON_W, BUTTON_H));
        button.addActionListener(e -> action.run());
        return button;
    }

    private JPanel buildFooter() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        row.setOpaque(false);
        JLabel hint = new JLabel("40 cartas en 6 grietas  ·  bloqueo estricto  ·  la recién invocada no ataca");
        hint.setFont(Typeface.body(Font.PLAIN, 13));
        hint.setForeground(Theme.TEXT_DIM);
        row.add(hint);
        return row;
    }

    private void startGame() {
        new GameWindow();
        dispose();
    }

    private void showCatalog() {
        GameEngine engine = previewEngine();
        if (engine != null) Dialogs.showCatalog(this, engine.catalogSortedByCost(), engine.catalogSortedByType());
    }

    private void showEvolutions() {
        GameEngine engine = previewEngine();
        if (engine != null) Dialogs.showEvolutions(this, engine.evolutionTree());
    }

    /**
     * Motor sin jugadores para consultar la ficha antes de jugar; se construye
     * una sola vez y se reutiliza en las siguientes aperturas.
     *
     * @return el motor, o {@code null} si la ficha no se pudo cargar (ya avisa).
     */
    private GameEngine previewEngine() {
        if (preview == null) {
            try {
                preview = Startup.newEngine();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "No se pudo cargar la ficha de cartas:\n" + e,
                        "Rift-Forge TCG", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        return preview;
    }
}
