package riftforge.app;

import riftforge.data.CardDatabase;
import riftforge.engine.GameEngine;
import riftforge.model.Card;
import riftforge.model.Player;
import riftforge.ui.ConsoleRenderer;

import java.util.List;
import java.util.Scanner;

/** Punto de entrada: abre la interfaz gráfica, o el duelo en consola con {@code --console}. */
public final class Main {
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--console")) {
            Scanner input = new Scanner(System.in);
            while (playGame(input)) {
                System.out.println("\n--- Partida reiniciada: se reparten de nuevo las cartas. ---\n");
            }
            return;
        }
        javax.swing.SwingUtilities.invokeLater(riftforge.ui.gui.GameWindow::new);
    }

    /** @return true si el usuario pidió comenzar una partida nueva. */
    private static boolean playGame(Scanner input) throws Exception {
        GameEngine game = Startup.newEngine();
        List<CardDatabase.CardRow> rows = CardDatabase.load();
        List<Card>[] halves = Startup.splitDecks(rows);

        System.out.print("Nombre del Jugador 1: ");
        String name1 = readLine(input);
        if (name1 == null) return false;
        Player player1 = new Player(nameOrDefault(name1, "Jugador 1"), Startup.STARTING_MANA, Startup.STARTING_LIFE);
        System.out.print("Nombre del Jugador 2: ");
        String name2 = readLine(input);
        if (name2 == null) return false;
        Player player2 = new Player(nameOrDefault(name2, "Jugador 2"), Startup.STARTING_MANA, Startup.STARTING_LIFE);
        for (Card card : Startup.shuffled(halves[0])) player1.deck().push(card);
        for (Card card : Startup.shuffled(halves[1])) player2.deck().push(card);
        System.out.println("Mazos repartidos: " + halves[0].size() + " cartas para " + player1.name()
                + " y " + halves[1].size() + " para " + player2.name() + ".");
        game.addPlayer(player1); game.addPlayer(player2);

        ConsoleRenderer renderer = new ConsoleRenderer();
        renderer.showWelcome();
        renderer.showFinalTurnOrder(game);
        while (true) {
            renderer.showStatus(game);
            Card nextCard = game.previewNextCard();
            System.out.println(nextCard == null ? "No quedan cartas para robar." : "Carta a robar: " + nextCard);
            System.out.print("[J]ugar, [D]escartar, [C]atálogo, [H]istorial, [E]voluciones, [Q]uitar/reiniciar o [X] salir: ");
            String option = readLine(input);
            if (option == null) { System.out.println("Gracias por jugar RiftForge TCG."); return false; }
            option = option.trim().toUpperCase();
            if (option.equals("Q")) {
                System.out.println("Partida terminada por el jugador.");
                return true;
            }
            if (option.equals("X")) { System.out.println("Gracias por jugar RiftForge TCG."); return false; }
            if (option.equals("H")) { renderer.showHistory(game); continue; }
            if (option.equals("C")) { renderer.showCatalog(game); continue; }
            if (option.equals("E")) { renderer.showEvolutionTree(game); continue; }
            if (!option.equals("J") && !option.equals("D")) { System.out.println("Opción no válida."); continue; }

            GameEngine.TurnResult result = game.playTurn(option.equals("J"));
            renderer.showTurn(result, game);
            if (result.target().isDefeated()) {
                System.out.println("\n¡" + result.active().name() + " gana el duelo!");
                renderer.showHistory(game);
                return true;
            }
        }
    }

    private static String nameOrDefault(String name, String fallback) {
        return name == null || name.isBlank() ? fallback : name;
    }

    private static String readLine(java.util.Scanner input) {
        return input.hasNextLine() ? input.nextLine() : null;
    }
}