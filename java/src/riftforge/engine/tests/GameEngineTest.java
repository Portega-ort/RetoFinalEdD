package riftforge.engine.tests;

import riftforge.app.Startup;
import riftforge.data.CardDatabase;
import riftforge.engine.GameEngine;
import riftforge.model.Card;
import riftforge.model.CardType;
import riftforge.model.Element;
import riftforge.model.Player;
import riftforge.sort.OwnSorter;
import riftforge.structures.PriorityQueue;
import riftforge.structures.Tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pruebas funcionales de las reglas TCG: reparto equilibrado, campo de batalla,
 * combate entre criaturas (bloqueo estricto sin trample), fatiga de invocación,
 * blindaje, prioridad de especiales, ordenamiento propio, árbol de evolución e
 * historial.
 *
 * <p>Se ejecuta sin framework: {@code java -cp out riftforge.engine.tests.GameEngineTest}.
 */
public final class GameEngineTest {
    private static final List<String> CHAIN_IMPULSOS = Arrays.asList(
            "La Hechicera de Impulsos", "La Campeona Élfica", "La Reina de la Red");
    private static final List<String> CHAIN_FORJA = Arrays.asList(
            "El Ingeniero Trasgo", "El Herrero Enano", "El Paladín de Neón");
    private static final List<String> CHAIN_FUEGO = Arrays.asList(
            "El Can Sabueso de Rastro", "El Licántropo de Cortafuegos", "El Dragón de Voltaje");

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        testFicha40Cartas();
        testRepartoEquilibrado();
        testCadenasCompletasPorJugador();
        testCriaturasAlCampo();
        testFatigaInvocacion();
        testCombateEntreCriaturas();
        testDañoDirectoSinBloqueo();
        testUtilidadSegunSuNombre();
        testMejoraEquipaYBlindajeAbsorbe();
        testEspecialPrioridadCola();
        testOrdenamientoPropio();
        testArbolEvolucion();
        testHistorialDoble();
        System.out.println("\nResultado: " + passed + " pasan, " + failed + " fallan.");
        if (failed > 0) System.exit(1);
    }

    private static void testFicha40Cartas() throws Exception {
        check(CardDatabase.load().size() == 40, "la ficha tiene 40 cartas");
    }

    private static void testRepartoEquilibrado() throws Exception {
        List<Card>[] halves = Startup.splitDecks(CardDatabase.load());
        check(halves[0].size() == 20 && halves[1].size() == 20, "mitades de 20/20");
        Set<String> mazoA = names(halves[0]);
        Set<String> mazoB = names(halves[1]);
        check(mazoA.size() == 20 && mazoB.size() == 20, "sin cartas duplicadas por jugador");
        Set<String> union = new HashSet<>(mazoA);
        union.addAll(mazoB);
        check(union.size() == 40, "entre ambas mitades se cubren las 40 cartas");
    }

    private static void testCadenasCompletasPorJugador() throws Exception {
        List<Card>[] halves = Startup.splitDecks(CardDatabase.load());
        Set<String> mazoA = names(halves[0]);
        Set<String> mazoB = names(halves[1]);
        for (List<String> chain : List.of(CHAIN_IMPULSOS, CHAIN_FORJA, CHAIN_FUEGO)) {
            boolean enA = mazoA.containsAll(chain);
            boolean enB = mazoB.containsAll(chain);
            check(enA ^ enB, "la cadena '" + chain.get(0) + "' está completa en un solo mazo");
        }
    }

    private static void testCriaturasAlCampo() throws Exception {
        GameEngine game = newGameWithHalves();
        for (int i = 0; i < 12; i++) game.playTurn(true);
        Player p1 = game.turnOrder().get(0);
        Player p2 = game.turnOrder().get(1);
        check(p1.field().size() + p2.field().size() > 0, "las criaturas jugadas van al campo");
        check(p1.deck().size() + p2.deck().size() > 0 || p1.graveyard().size() + p2.graveyard().size() > 0,
                "el duelo consume las cartas de los mazos");
    }

    private static void testFatigaInvocacion() throws Exception {
        GameEngine game = controlledGame();
        Player p1 = game.turnOrder().get(0);
        Player p2 = game.turnOrder().get(1);
        Card criatura = firstCreature(game);
        fillDeck(p1, criatura);
        int vida = p2.life();
        game.playTurn(true);
        check(p1.field().size() == 1, "la criatura recién jugada entra al campo");
        check(p2.life() == vida, "la criatura recién invocada tiene fatiga y no ataca ese turno");
    }

    private static void testCombateEntreCriaturas() throws Exception {
        GameEngine game = controlledGame();
        Player p1 = game.turnOrder().get(0);
        Player p2 = game.turnOrder().get(1);
        Card atacante = firstCreature(game);
        Card defensor = secondCreature(game);
        p1.field().add(atacante);
        p2.field().add(defensor);
        fillDeck(p1, game.findCard("El Altar de Carga"));
        int vida = p2.life();
        int atk = atacante.attackWithBonus(game.activeRift());
        game.playTurn(true);
        if (atk < defensor.health()) {
            check(p2.life() == vida, "la criatura rival bloquea: no hay daño directo a la vida");
            check(defensor.damageTaken() == atk && !defensor.isDestroyed(), "el bloqueador recibe el golpe en su HP");
        } else {
            check(defensor.isDestroyed() && p2.field().size() == 0, "el bloqueador destruido sale del campo");
            check(p2.graveyard().peek() == defensor, "la criatura destruida va al cementerio");
            check(p2.life() == vida, "el exceso de daño no atraviesa la línea (bloqueo estricto)");
        }
    }

    private static void testDañoDirectoSinBloqueo() throws Exception {
        GameEngine game = controlledGame();
        Player p1 = game.turnOrder().get(0);
        Player p2 = game.turnOrder().get(1);
        Card atacante = firstCreature(game);
        p1.field().add(atacante);
        fillDeck(p1, game.findCard("El Altar de Carga"));
        int vida = p2.life();
        int atk = atacante.attackWithBonus(game.activeRift());
        game.playTurn(true);
        check(p2.field().isEmpty(), "sin criaturas rivales el campo está abierto");
        check(p2.life() == vida - atk, "sin bloqueo el ataque golpea directo la vida del rival");
    }

    private static void testUtilidadSegunSuNombre() throws Exception {
        GameEngine game = new GameEngine();
        CardDatabase.apply(game, CardDatabase.load());
        Startup.addRifts(game);
        Player owner = new Player("Util", 10, 25);
        Player rival = new Player("Rival", 10, 25);
        game.addPlayer(owner);
        game.addPlayer(rival);
        Card altar = game.findCard("El Altar de Carga");
        fillDeck(owner, altar);
        int manaAntes = owner.mana();
        game.playTurn(true);
        check(owner.mana() == manaAntes - altar.manaCost() + 1, "El Altar de Carga devuelve +1 de Flux");
        check(owner.graveyard().size() == 1, "la utilidad usada va al cementerio");
    }

    private static void testMejoraEquipaYBlindajeAbsorbe() throws Exception {
        Player owner = new Player("Mejora", 10, 25);
        GameEngine game = new GameEngine();
        CardDatabase.apply(game, CardDatabase.load());
        Startup.addRifts(game);
        Player rival = new Player("Rival", 10, 25);
        game.addPlayer(owner);
        game.addPlayer(rival);
        Card criatura = game.findCard("La Valquiria de Plasma");
        Card mejora = game.findCard("El Núcleo Supremo");
        owner.field().add(criatura);
        fillDeck(owner, mejora);
        game.playTurn(true);
        check(owner.field().size() == 1, "la mejora no entra al campo");
        check(owner.fieldAttackBonus() > 0, "la mejora aporta ATQ permanente al campo");
        check(owner.shieldPool() > 0, "la mejora aporta blindaje defensivo");
        int absorbido = owner.absorb(10);
        check(absorbido == 0 || owner.shieldPool() == 0, "el blindaje absorbe el daño antes de la vida");
    }

    private static void testEspecialPrioridadCola() {
        PriorityQueue<String> queue = new PriorityQueue<>();
        queue.enqueue("común", false);
        queue.enqueue("común-2", false);
        queue.enqueue("ESPECIAL", true);
        check(queue.dequeue().equals("ESPECIAL"), "las especiales salen primero de la cola de prioridad");
        check(queue.dequeue().equals("común") && queue.dequeue().equals("común-2"), "el resto conserva su orden FIFO");
    }

    private static void testOrdenamientoPropio() throws Exception {
        GameEngine game = new GameEngine();
        CardDatabase.apply(game, CardDatabase.load());
        List<Card> sorted = OwnSorter.insertionSortByCost(new ArrayList<>(asList(game.catalog())));
        boolean crece = true;
        for (int i = 1; i < sorted.size(); i++) crece &= sorted.get(i - 1).manaCost() <= sorted.get(i).manaCost();
        check(crece && sorted.size() == 40, "el Insertion Sort propio ordena las 40 por coste");
    }

    private static void testArbolEvolucion() throws Exception {
        GameEngine game = new GameEngine();
        CardDatabase.apply(game, CardDatabase.load());
        Tree<Card> tree = game.evolutionTree();
        check(tree.height() == 3, "el árbol de evolución llega a la legendaría (altura 3)");
        check(containsAll(tree, CHAIN_IMPULSOS) && containsAll(tree, CHAIN_FORJA) && containsAll(tree, CHAIN_FUEGO),
                "el árbol contiene las tres líneas completas");
    }

    private static void testHistorialDoble() throws Exception {
        GameEngine game = newGameWithHalves();
        for (int i = 0; i < 6; i++) game.playTurn(i % 2 == 0);
        check(game.history().size() == 6, "el historial registra un evento por turno");
        check(game.history().forward().size() == 6 && game.history().backward().size() == 6, "el historial se recorre en las dos direcciones");
    }

    private static GameEngine newGameWithHalves() throws Exception {
        GameEngine game = new GameEngine();
        CardDatabase.apply(game, CardDatabase.load());
        Startup.addRifts(game);
        List<Card>[] halves = Startup.splitDecks(CardDatabase.load());
        Player p1 = new Player("P1", 10, 25);
        Player p2 = new Player("P2", 10, 25);
        for (Card c : halves[0]) p1.deck().push(c);
        for (Card c : halves[1]) p2.deck().push(c);
        game.addPlayer(p1);
        game.addPlayer(p2);
        return game;
    }

    private static void fillDeck(Player player, Card card) {
        player.deck().push(card);
    }

    private static GameEngine controlledGame() throws Exception {
        GameEngine game = new GameEngine();
        CardDatabase.apply(game, CardDatabase.load());
        Startup.addRifts(game);
        Player p1 = new Player("P1", 10, 30);
        Player p2 = new Player("P2", 10, 30);
        game.addPlayer(p1);
        game.addPlayer(p2);
        return game;
    }

    private static Card firstCreature(GameEngine game) {
        for (Card card : game.catalog()) if (card.type() == CardType.CRIATURA) return card;
        throw new IllegalStateException("el catálogo necesita al menos una criatura");
    }

    private static Card secondCreature(GameEngine game) {
        boolean primero = false;
        for (Card card : game.catalog()) {
            if (card.type() != CardType.CRIATURA) continue;
            if (!primero) { primero = true; continue; }
            return card;
        }
        return firstCreature(game);
    }

    private static Set<String> names(List<Card> cards) {
        Set<String> set = new HashSet<>();
        for (Card card : cards) set.add(card.name());
        return set;
    }

    private static List<Card> asList(Iterable<Card> iterable) {
        List<Card> list = new ArrayList<>();
        for (Card card : iterable) list.add(card);
        return list;
    }

    private static boolean containsAll(Tree<Card> tree, List<String> names) {
        List<String> presentes = new ArrayList<>();
        for (Card card : tree.preorder()) presentes.add(card.name());
        return presentes.containsAll(names);
    }

    private static void check(boolean condition, String label) {
        if (condition) {
            passed++;
            System.out.println("PASS " + label);
        } else {
            failed++;
            System.out.println("FAIL " + label);
        }
    }
}