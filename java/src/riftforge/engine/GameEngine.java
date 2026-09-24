package riftforge.engine;

import riftforge.model.*;
import riftforge.structures.*;

import java.util.List;

/**
 * Orquesta las reglas; las clases de estructuras sólo administran enlaces.
 *
 * <p>Desde el Avance 2 suma: un {@code HashMap} que indexa el catálogo por
 * nombre (búsqueda instantánea), una {@code PriorityQueue} que resuelve primero
 * las habilidades especiales sin importar cuándo se jugaron, y un {@code Tree}
 * con la línea de evolución de cada carta (básica → mejorada → legendaria).
 */
public final class GameEngine {
    /** Número de turnos consecutivos durante los que permanece activo cada efecto. */
    public static final int TURNS_PER_RIFT = 2;
    /** Raíz sintética del árbol de evolución; agrupa todas las líneas por carta. */
    private static final Card EVOLUTION_ROOT = new Card("Árbol de Evolución", Element.VACIO, CardType.EQUIPO, 0, 0, 0);

    private final SinglyLinkedList<Card> catalog = new SinglyLinkedList<>();
    /** Índice del catálogo por nombre normalizado: búsqueda O(1). */
    private final java.util.HashMap<String, Card> catalogIndex = new java.util.HashMap<>();
    private final CircularLinkedList<Rift> rifts = new CircularLinkedList<>();
    private final CircularQueue<Player> turnQueue = new CircularQueue<>();
    private final DoublyLinkedList<BattleEvent> history = new DoublyLinkedList<>();
    /** Cola de resolución: las cartas especiales salen antes que las normales. */
    private final PriorityQueue<Card> pendingEffects = new PriorityQueue<>();
    /** Línea de evolución de las cartas: básica → mejorada → legendaria. */
    private final Tree<Card> evolutions = new Tree<>();
    private int turn;

    /** Registra en la lista simple y lo indexa por nombre. Acepta null resolviendo a no-op. */
    public void registerCard(Card card) {
        if (card == null) return;
        catalog.add(card);
        catalogIndex.put(normalize(card.name()), card);
    }
    /** Búsqueda instantánea por nombre en el índice hash, con recaída en la lista simple. */
    public Card findCard(String name) {
        Card hit = catalogIndex.get(normalize(name));
        if (hit != null) return hit;
        for (Card card : catalog) if (card != null && card.name().equalsIgnoreCase(name)) return card;
        return null;
    }
    public boolean removeCard(String name) {
        boolean removed = catalog.remove(card -> card != null && card.name().equalsIgnoreCase(name));
        catalogIndex.remove(normalize(name));
        return removed;
    }
    /** Permite recorrer el catálogo sin exponer sus nodos ni operaciones de enlace. */
    public Iterable<Card> catalog() { return catalog; }

    /**
     * Copia del catálogo ordenada por coste de maná con el Insertion Sort
     * propio (nunca con {@code Arrays.sort}); la lista simple original se
     * conserva intacta. Se usa en la vista de colección de la interfaz.
     */
    public java.util.List<Card> catalogSortedByCost() {
        java.util.List<Card> list = new java.util.ArrayList<>();
        for (Card card : catalog) if (card != null) list.add(card);
        return riftforge.sort.OwnSorter.insertionSortByCost(list);
    }

    /** Registra una carta base como nivel 1 de su línea de evolución bajo la raíz sintética. */
    public void registerBaseEvolution(Card base) {
        if (evolutions.size() == 0) evolutions.setRoot(EVOLUTION_ROOT);
        evolutions.addChild(EVOLUTION_ROOT, base);
    }
    /** Agrega un nivel (básica → mejorada → legendaria) y da de alta la nueva carta en el catálogo. */
    public void registerEvolution(Card parent, Card child) {
        if (evolutions.size() == 0) evolutions.setRoot(EVOLUTION_ROOT);
        evolutions.addChild(parent, child);
        registerCard(child);
    }
    public Tree<Card> evolutionTree() { return evolutions; }

    private static String normalize(String name) { return name == null ? "" : name.toLowerCase(java.util.Locale.ROOT); }
    public void addRift(Rift rift) { rifts.add(rift); }
    public void addPlayer(Player player) { turnQueue.enqueue(player); }
    public Rift activeRift() { return rifts.current(); }
    public int turn() { return turn; }
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
        Rift rift = activeRift();
        StringBuilder action = new StringBuilder();
        if (card == null) {
            action.append(active.name()).append(" no puede robar: deck y cementerio vacíos.");
        } else if (!wantsToPlay) {
            active.graveyard().push(card);
            action.append(active.name()).append(" descarta ").append(card.name()).append(" al cementerio.");
        } else if (!active.canPay(card)) {
            active.graveyard().push(card);
            action.append(active.name()).append(" roba ").append(card.name())
                    .append(" pero no tiene maná; la descarta.");
        } else {
            active.pay(card);
            pendingEffects.enqueue(card, card.special());
            action.append(resolvePendingEffects(active));
        }
        action.append(attackPhase(active, target, card));
        String message = action.toString().trim();
        history.addLast(new BattleEvent(turn, message));
        turnQueue.enqueue(active);
        if (turn % TURNS_PER_RIFT == 0) rifts.rotate();
        return new TurnResult(turn, active, target, card, rift, message);
    }

    private Card draw(Player player) {
        recycleDeckIfNeeded(player);
        return player.deck().isEmpty() ? null : player.deck().pop();
    }

    /**
     * Drena la cola de resolución en orden de prioridad: las cartas especiales
     * (alta prioridad) resuelven su efecto antes que las normales, sin importar
     * cuándo se encolaron. Internamente es FIFO dentro de cada nivel.
     *
     * <p>Solo las cartas de tipo {@code CRIATURA} entran al campo de batalla;
     * las de tipo {@code HECHIZO} (Utiles) ejecutan su efecto y las de tipo
     * {@code EQUIPO} (Mejoras) entregan su bono a la criatura.
     */
    private String resolvePendingEffects(Player owner) {
        StringBuilder sb = new StringBuilder();
        while (!pendingEffects.isEmpty()) {
            Card card = pendingEffects.dequeue();
            sb.append(owner.name()).append(" ").append(resolveCard(card, owner)).append(".\n");
        }
        return sb.toString();
    }

    private String resolveCard(Card card, Player owner) {
        String special = card.special() ? " [especial: su efecto resuelve antes que el de cartas normales]" : "";
        return switch (card.type()) {
            case CRIATURA -> {
                owner.field().add(card);
                yield "juega la criatura " + card.name() + " (coste " + card.manaCost()
                        + ") y la coloca en su campo" + special;
            }
            case HECHIZO -> {
                owner.graveyard().push(card);
                yield "juega la Utilidad " + card.name() + special + " → " + resolveUtility(card, owner);
            }
            case EQUIPO -> {
                if (owner.field().isEmpty()) {
                    owner.graveyard().push(card);
                    yield "intenta jugar la Mejora " + card.name()
                            + " pero no hay criatura en su campo que equipar; la descarta";
                }
                owner.graveyard().push(card);
                owner.buffFieldAttack(card.attack());
                owner.addShield(card.health());
                yield "equipa la Mejora " + card.name() + " en su campo: +" + card.attack()
                        + " ATQ permanente y +" + card.health() + " de blindaje" + special;
            }
        };
    }

    /**
     * Efecto concreto de cada Utilidad: hace lo que su nombre dice sobre el
     * estado que sí existe en el simulador (maná, vida, blindaje y bono de
     * ataque del campo). Si una carta nueva no está mapeada, se resuelve con
     * un efecto de respaldo (restaurar maná) y se deja claro en el historial.
     */
    private String resolveUtility(Card card, Player owner) {
        String special = card.special() ? " [especial]" : "";
        switch (card.name()) {
            case "El Altar de Carga":
                owner.restoreMana(1);
                return "genera +1 de Flux" + special;
            case "El Condensador de Energía":
                owner.restoreMana(3);
                return "almacena +3 de Flux para el siguiente turno" + special;
            case "El Ojo de Escaneo":
            case "La Brújula de Flujo":
            case "El Mapa de Enlaces":
            case "El Reloj de Arena de Datos":
                owner.restoreMana(2);
                return "agiliza tu flujo de recursos (+2 maná)" + special;
            case "El Portal de Enlace":
                owner.heal(2);
                return "reorganiza tu campo y recupera 2 de vida" + special;
            case "El Revelador de Runas":
                owner.addShield(3);
                return "cancela 3 puntos de daño entrante (blindaje)" + special;
            case "El Tomo de Análisis":
                owner.buffFieldAttack(1);
                return "otorga +1 ATQ permanente a tu campo" + special;
            case "El Orbe de Transmisión":
                owner.buffFieldAttack(1);
                owner.heal(1);
                return "transfiere +1 ATQ a tu campo y recupera 1 de vida" + special;
            default:
                owner.restoreMana(1);
                return "efecto de respaldo: restaura 1 de Flux" + special;
        }
    }

    /**
     * Fase de ataque (bloqueo estricto): cada criatura golpea a la criatura
     * frontal del campo enemigo y, si la derrota, el exceso de daño se pierde
     * contra la línea. El jugador rival solo recibe daño directo cuando su
     * campo queda vacío (no le quedan criaturas que bloqueen).
     *
     * <p>Reglas:
     * <ul>
     *   <li>Una criatura recién jugada en este turno sufre fatiga de invocación
     *       y no ataca (pero sí puede recibir daño en el turno del rival).</li>
     *   <li>La criatura destruida va al cementerio de su dueño.</li>
     * </ul>
     */
    private String attackPhase(Player owner, Player target, Card summoned) {
        if (owner.field().isEmpty()) return "";
        List<Card> line = new java.util.ArrayList<>();
        for (Card defender : target.field()) line.add(defender);
        StringBuilder out = new StringBuilder("\nEl campo de " + owner.name() + " ataca: ");
        int position = 0;
        int face = 0;
        boolean bonusGastado = false;
        for (Card creature : owner.field()) {
            if (creature == null) continue;
            if (creature == summoned) {
                out.append(creature.name()).append(" (recién invocada, no ataca), ");
                continue;
            }
            int atk = creature.attackWithBonus(activeRift());
            if (!bonusGastado) { atk += owner.fieldAttackBonus(); bonusGastado = true; }
            if (position >= line.size()) {
                face += atk;
                out.append(creature.name()).append(" golpea a ").append(target.name()).append(" por ").append(atk).append(", ");
                continue;
            }
            Card front = line.get(position);
            int soak = Math.min(atk, front.remainingHealth());
            front.takeDamage(soak);
            if (front.isDestroyed()) {
                target.graveyard().push(front);
                target.field().remove(c -> c == front);
                position++;
                out.append(creature.name()).append(" destruye a ").append(front.name());
                if (soak < atk) out.append(" (sobraron ").append(atk - soak).append(", se pierden)");
                out.append(", ");
            } else {
                out.append(creature.name()).append(" golpea a ").append(front.name())
                        .append(" (").append(soak).append(" → ").append(front.remainingHealth()).append(" HP), ");
            }
        }
        if (face > 0) {
            int dealt = target.absorb(face);
            String shield = dealt < face ? " (el blindaje de " + target.name() + " absorbió " + (face - dealt) + ")" : "";
            target.receiveDamage(dealt);
            out.append("daño directo a ").append(target.name()).append(": ").append(dealt).append(shield).append(".");
        } else {
            if (out.toString().endsWith(", ")) out.setLength(out.length() - 2);
            out.append(".");
        }
        return out.toString().trim();
    }

    private void recycleDeckIfNeeded(Player player) {
        if (player.deck().isEmpty()) while (!player.graveyard().isEmpty()) {
            Card card = player.graveyard().pop();
            card.clearDamage();
            player.deck().push(card);
        }
    }

    public record TurnResult(int number, Player active, Player target, Card drawn, Rift rift, String action) { }
}
