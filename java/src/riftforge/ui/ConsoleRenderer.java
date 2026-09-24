package riftforge.ui;

import riftforge.engine.GameEngine;
import riftforge.model.Card;
import riftforge.structures.Tree;

/** Vista de consola: concentra el formato, sin contaminar el motor con IO. */
public final class ConsoleRenderer {
    public void showWelcome() {
        System.out.println("============================================================");
        System.out.println("                 RIFTFORGE TCG - DUELO LOCAL");
        System.out.println("Cada turno recupera hasta 2 de maná (máximo 10).");
        System.out.println("============================================================");
    }

    public void showStatus(GameEngine engine) {
        System.out.printf("%nGrieta: %s (%s +%d ATK)%n", engine.activeRift().name(), engine.activeRift().bonusElement(), engine.activeRift().attackBonus());
        for (var player : engine.turnOrder()) {
            System.out.printf("%s | vida: %d | maná: %d | deck: %d | cementerio: %d | blindaje: %d%n",
                    player.name(), player.life(), player.mana(), player.deck().size(), player.graveyard().size(), player.shieldPool());
            System.out.printf("  campo: %s%n", describeField(player));
        }
    }

    private String describeField(riftforge.model.Player player) {
        StringBuilder sb = new StringBuilder();
        for (Card creature : player.field()) {
            if (creature == null) continue;
            sb.append(creature.name());
            if (creature.damageTaken() > 0) sb.append(" [HP ").append(creature.remainingHealth()).append("/").append(creature.health()).append("]");
            sb.append(", ");
        }
        String field = sb.length() == 0 ? "(vacío)" : sb.substring(0, sb.length() - 2);
        return field + (player.fieldAttackBonus() > 0 ? " | mejoras: +" + player.fieldAttackBonus() + " ATQ" : "");
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
        for (var card : engine.catalog()) if (card != null) System.out.println("> " + card);
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

    /** Muestra la línea de evolución (básica → mejorada → legendaria) con recorrido recursivo en preorden. */
    public void showEvolutionTree(GameEngine engine) {
        System.out.println("\n--- Línea de evolución de cartas (árbol, preorden recursivo) ---");
        var tree = engine.evolutionTree();
        if (tree.root() == null) { System.out.println("Aún no hay líneas de evolución registradas."); return; }
        showNode(tree.root(), 0);
        System.out.println("Altura del árbol: " + tree.height() + " | cartas en la línea: " + tree.size());
    }

    private void showNode(Tree.TreeNode<Card> node, int depth) {
        System.out.println("  ".repeat(depth) + "> " + node.data());
        for (Tree.TreeNode<Card> child : node.children().forward()) showNode(child, depth + 1);
    }
}
