package riftforge.ui;

import riftforge.engine.GameEngine;

/** Vista de consola: concentra el formato, sin contaminar el motor con IO. */
public final class ConsoleRenderer {
    public void showWelcome() {
        System.out.println("============================================================");
        System.out.println("                 RIFTFORGE TCG - DUELO LOCAL");
        System.out.println("Cada turno recupera hasta 2 de maná (máximo 10).");
        System.out.println("============================================================");
    }

    public void showStatus(GameEngine engine) {
        var active = engine.nextPlayer();
        System.out.printf("%nGrieta: %s (%s +%d ATK)%n", engine.activeRift().name(), engine.activeRift().bonusElement(), engine.activeRift().attackBonus());
        System.out.printf("Turno de %s | vida: %d | maná: %d | deck: %d | cementerio: %d%n",
                active.name(), active.life(), active.mana(), active.deck().size(), active.graveyard().size());
    }

    public void showHistory(GameEngine engine) {
        System.out.println("\n--- Historial hacia adelante (primera jugada primero) ---");
        if (engine.history().size() == 0) System.out.println("Aún no hay jugadas.");
        else {
            engine.history().forward().forEach(event -> System.out.println("> " + event));
            System.out.println("--- Historial hacia atrás (más reciente primero) ---");
            engine.history().backward().forEach(event -> System.out.println("> " + event));
        }
    }

    /** Recorre la lista simplemente enlazada desde la carta más recientemente registrada. */
    public void showCatalog(GameEngine engine) {
        System.out.println("\n--- Catálogo de cartas (lista simple) ---");
        for (var card : engine.catalog()) System.out.println("> " + card);
    }

    public void showFinalTurnOrder(GameEngine engine) {
        String order = engine.turnOrder().stream().map(player -> player.name()).reduce((left, right) -> left + " -> " + right).orElse("sin jugadores");
        System.out.println("Orden circular final de iniciativa: " + order + " -> (vuelve al inicio)");
    }

    public void showTurn(GameEngine.TurnResult result, GameEngine engine) {
        System.out.println("\n============================================================");
        System.out.println("RIFTFORGE TCG | MOTOR DE DUELOS POR TURNOS");
        System.out.printf("[GRIETA ACTIVA] %s | bono %s +%d ATK%n", result.rift().name(), result.rift().bonusElement(), result.rift().attackBonus());
        System.out.printf("[TURNO %d] %s -> siguiente: %s%n", result.number(), result.active().name(), engine.nextPlayer().name());
        System.out.println("> " + result.action());
        System.out.printf("Vida: %s=%d | %s=%d%n", result.active().name(), result.active().life(), result.target().name(), result.target().life());
        System.out.printf("Maná: %s=%d | %s=%d%n", result.active().name(), result.active().mana(), result.target().name(), result.target().mana());
    }
}
