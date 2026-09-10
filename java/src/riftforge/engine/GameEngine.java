package riftforge.engine;

import riftforge.model.*;
import riftforge.structures.*;

/** Orquesta las reglas; las clases de estructuras sólo administran enlaces. */
public final class GameEngine {
    /** Número de turnos consecutivos durante los que permanece activo cada efecto. */
    public static final int TURNS_PER_RIFT = 2;
    private final SinglyLinkedList<Card> catalog = new SinglyLinkedList<>();
    private final CircularLinkedList<Rift> rifts = new CircularLinkedList<>();
    private final CircularQueue<Player> turnQueue = new CircularQueue<>();
    private final DoublyLinkedList<BattleEvent> history = new DoublyLinkedList<>();
    private int turn;

    public void registerCard(Card card) { catalog.add(card); }
    public Card findCard(String name) { return catalog.find(card -> card.name().equalsIgnoreCase(name)); }
    public boolean removeCard(String name) { return catalog.remove(card -> card.name().equalsIgnoreCase(name)); }
    /** Permite recorrer el catálogo sin exponer sus nodos ni operaciones de enlace. */
    public Iterable<Card> catalog() { return catalog; }
    public void addRift(Rift rift) { rifts.add(rift); }
    public void addPlayer(Player player) { turnQueue.enqueue(player); }
    public Rift activeRift() { return rifts.current(); }
    public DoublyLinkedList<BattleEvent> history() { return history; }
    public Player nextPlayer() { return turnQueue.peek(); }
    public java.util.List<Player> turnOrder() { return turnQueue.snapshot(); }

    /** Muestra la próxima carta. Si el deck terminó, primero recicla el cementerio. */
    public Card previewNextCard() {
        Player player = turnQueue.peek();
        recycleDeckIfNeeded(player);
        return player.deck().isEmpty() ? null : player.deck().peek();
    }

    /**
     * Ejecuta una ronda completa. La decisión de jugar o descartar la carta viene de la interfaz.
     *
     * La secuencia concentra casi toda la lógica del juego, en este orden:
     *
     * <ol>
     *   <li><b>Robar:</b> se desencola al jugador activo (que pasa a ser objetivo de quien lo
     *       sigue en la cola), se restaura su maná y se roba la carta del tope de su deck.</li>
     *   <li><b>Reciclar si hace falta:</b> si el deck está vacío, {@link #recycleDeckIfNeeded}
     *       mueve el cementerio al deck ({@code pop}/{@code push}) para volver a tener cartas
     *       que robar, sin inanición de jugadores.</li>
     *   <li><b>Decidir:</b> si no hay carta se registra que el jugador no pudo robar; si no quiere
     *       jugar o no tiene maná suficiente, se descarta la carta al cementerio; en cualquier otro
     *       caso se juega.</li>
     *   <li><b>Aplicar bono:</b> al jugar se paga el coste, se calcula el ataque con el bono del
     *       efecto de campo activo y se inflige ese daño al objetivo; la carta usada va al cementerio.</li>
     *   <li><b>Registrar:</b> se agrega un {@link BattleEvent} con la acción descriptiva al final
     *       de la lista doble que sirve de historial.</li>
     *   <li><b>Rotar:</b> el jugador activo se reencola al final de la cola circular para conservar
     *       la iniciativa, y cada {@link #TURNS_PER_RIFT} turnos se rota la lista circular de grietas.</li>
     * </ol>
     *
     * @param wantsToPlay {@code true} si el jugador activo intenta jugar la carta robada
     * @return resultado del turno con el jugador, objetivo, carta y efecto involucrados
     */
    public TurnResult playTurn(boolean wantsToPlay) {
        if (turnQueue.size() < 2) throw new IllegalStateException("Se requieren al menos dos jugadores");
        Player active = turnQueue.dequeue();
        Player target = turnQueue.peek();
        turn++;
        active.restoreMana(2);
        Card card = draw(active);
        String action;
        if (card == null) action = active.name() + " no puede robar: deck y cementerio vacíos.";
        else if (!wantsToPlay) { active.graveyard().push(card); action = active.name() + " descarta " + card.name() + " al cementerio."; }
        else if (!active.canPay(card)) { active.graveyard().push(card); action = active.name() + " roba " + card.name() + " pero no tiene mana; la descarta."; }
        else {
            active.pay(card);
            int damage = card.attackWithBonus(activeRift());
            target.receiveDamage(damage);
            active.graveyard().push(card);
            action = "%s invoca %s e inflige %d a %s.".formatted(active.name(), card.name(), damage, target.name());
        }
        history.addLast(new BattleEvent(turn, action));
        turnQueue.enqueue(active);
        Rift usedRift = activeRift();
        if (turn % TURNS_PER_RIFT == 0) rifts.rotate();
        return new TurnResult(turn, active, target, card, usedRift, action);
    }

    private Card draw(Player player) {
        recycleDeckIfNeeded(player);
        return player.deck().isEmpty() ? null : player.deck().pop();
    }

    private void recycleDeckIfNeeded(Player player) {
        if (player.deck().isEmpty()) while (!player.graveyard().isEmpty()) player.deck().push(player.graveyard().pop());
    }

    public record TurnResult(int number, Player active, Player target, Card drawn, Rift rift, String action) { }
}
