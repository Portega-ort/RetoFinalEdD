package riftforge.app;

import riftforge.engine.GameEngine;
import riftforge.model.*;
import riftforge.ui.ConsoleRenderer;

import java.util.Scanner;

/** Punto de entrada de un duelo local para dos personas. */
public final class Main {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        while (playGame(input)) {
            System.out.println("\n--- Partida reiniciada: se restauraron vida, maná, decks y turnos. ---\n");
        }
    }

    /** @return true si el usuario pidió comenzar una partida nueva. */
    private static boolean playGame(Scanner input) {
        GameEngine game = new GameEngine();
        game.addRift(new Rift("Fuego Ígneo", Element.FUEGO, 2));
        game.addRift(new Rift("Tormenta Eléctrica", Element.ELECTRICO, 3));
        game.addRift(new Rift("Vacío Abisal", Element.VACIO, 1));
        game.addRift(new Rift("Marea Cristalina", Element.AGUA, 2));

        Card caballero = new Card("Caballero de Fuego", Element.FUEGO, CardType.CRIATURA, 3, 5, 6);
        Card mago = new Card("Mago Eléctrico", Element.ELECTRICO, CardType.HECHIZO, 4, 6, 3);
        Card dragon = new Card("Dragón del Vacío", Element.VACIO, CardType.CRIATURA, 6, 8, 8);
        Card monstruo = new Card("Monstruo de Agua", Element.AGUA, CardType.CRIATURA, 2, 3, 7);
        game.registerCard(caballero); game.registerCard(mago); game.registerCard(dragon); game.registerCard(monstruo);

        System.out.print("Nombre del Jugador 1: ");
        Player player1 = new Player(nameOrDefault(input.nextLine(), "Jugador 1"), 10, 25);
        System.out.print("Nombre del Jugador 2: ");
        Player player2 = new Player(nameOrDefault(input.nextLine(), "Jugador 2"), 10, 25);
        fillDeck(player1, caballero, mago, dragon, monstruo);
        fillDeck(player2, monstruo, dragon, mago, caballero);
        game.addPlayer(player1); game.addPlayer(player2);

        ConsoleRenderer renderer = new ConsoleRenderer();
        renderer.showWelcome();
        while (true) {
            renderer.showStatus(game);
            Card nextCard = game.previewNextCard();
            System.out.println(nextCard == null ? "No quedan cartas para robar." : "Carta preparada: " + nextCard);
            System.out.print("[J]ugar, [D]escartar, [C]atálogo, [H]istorial, [Q]uitar/reiniciar o [X] salir: ");
            String option = input.nextLine().trim().toUpperCase();
            if (option.equals("Q")) {
                System.out.println("Partida terminada por el jugador.");
                renderer.showFinalTurnOrder(game);
                return true;
            }
            if (option.equals("X")) { System.out.println("Gracias por jugar RiftForge TCG."); return false; }
            if (option.equals("H")) { renderer.showHistory(game); continue; }
            if (option.equals("C")) { renderer.showCatalog(game); continue; }
            if (!option.equals("J") && !option.equals("D")) { System.out.println("Opción no válida."); continue; }

            GameEngine.TurnResult result = game.playTurn(option.equals("J"));
            renderer.showTurn(result, game);
            if (result.target().isDefeated()) {
                System.out.println("\n¡" + result.active().name() + " gana el duelo!");
                renderer.showHistory(game);
                renderer.showFinalTurnOrder(game);
                return true;
            }
        }
    }

    private static void fillDeck(Player player, Card... cards) {
        for (Card card : cards) player.deck().push(card);
    }

    private static String nameOrDefault(String name, String fallback) {
        return name.isBlank() ? fallback : name;
    }
}
