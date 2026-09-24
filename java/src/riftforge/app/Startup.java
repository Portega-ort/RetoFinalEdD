package riftforge.app;

import riftforge.data.CardDatabase;
import riftforge.engine.GameEngine;
import riftforge.model.Card;
import riftforge.model.Element;
import riftforge.model.Rift;
import riftforge.sort.OwnSorter;

import java.util.ArrayList;
import java.util.List;

/**
 * Arranque de una partida: carga la ficha (cards2.csv), arma el motor con las
 * 6 grietas (una por elemento) y reparte el catálogo completo en dos mitades
 * equilibradas para que cada jugador reciba un mazo propio de 20 cartas.
 *
 * <p>El reparto garantiza que cada jugador conserve <b>sus líneas de evolución
 * completas</b> (básica → mejorada → legendaria): las cadenas se asignan
 * enteras y alternadas; el resto del catálogo se distribuye por coste de maná
 * (con el ordenamiento propio) para que ambas mitades tengan una curva de
 * coste parecida.
 */
public final class Startup {
    private Startup() {
    }

    /** Maná con el que empieza cada duelista (suficiente para invocar una unidad el primer turno). */
    public static final int STARTING_MANA = 6;
    /** Vida inicial de cada duelista (se subió a 30 para dar margen con el bloqueo). */
    public static final int STARTING_LIFE = 30;

    /** Carga la ficha y devuelve un motor configurado con grietas y catálogo (sin jugadores). */
    public static GameEngine newEngine() throws java.io.IOException {
        List<CardDatabase.CardRow> rows = CardDatabase.load();
        GameEngine game = new GameEngine();
        CardDatabase.apply(game, rows);
        addRifts(game);
        return game;
    }

    public static void addRifts(GameEngine game) {
        game.addRift(new Rift("Fuego Ígneo", Element.FUEGO, 2));
        game.addRift(new Rift("Tormenta Eléctrica", Element.ELECTRICO, 3));
        game.addRift(new Rift("Vacío Abisal", Element.VACIO, 1));
        game.addRift(new Rift("Marea Cristalina", Element.AGUA, 2));
        game.addRift(new Rift("Sabiduría del Dragón", Element.DRAGON, 2));
        game.addRift(new Rift("Sombras del Ciberespacio", Element.SOMBRA, 3));
    }

    /**
     * Copia permutada con Fisher–Yates (barajado propio, no usa
     * {@code Collections.shuffle}). Con ella los mazos siempre salen en un
     * orden distinto en cada partida.
     */
    public static <T> java.util.List<T> shuffled(java.util.List<T> source) {
        java.util.List<T> copy = new java.util.ArrayList<>(source);
        java.util.Random random = new java.util.Random();
        for (int i = copy.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            T tmp = copy.get(i);
            copy.set(i, copy.get(j));
            copy.set(j, tmp);
        }
        return copy;
    }

    /**
     * Divide el catálogo (40 cartas) en dos mazos de 20.
     *
     * @return {@code [mazoA, mazoB]}; la cola (orden de robo) es la pila, por
     *         eso las cartas del mazo A se apilan de abajo hacia arriba.
     */
    public static List<Card>[] splitDecks(List<CardDatabase.CardRow> rows) {
        List<Card> a = new ArrayList<>();
        List<Card> b = new ArrayList<>();

        List<List<Card>> chains = evolutionChains(rows);
        int owner = 0;
        for (List<Card> chain : chains) {
            (owner++ % 2 == 0 ? a : b).addAll(chain);
        }

        List<Card> chainCards = new ArrayList<>();
        for (List<Card> chain : chains) chainCards.addAll(chain);

        List<Card> rest = new ArrayList<>();
        for (CardDatabase.CardRow row : rows) {
            Card card = row.toCard();
            if (containsName(chainCards, card)) continue;
            rest.add(card);
        }
        List<Card> sortedRest = OwnSorter.insertionSortByCost(rest);
        for (int i = 0; i < sortedRest.size(); i++) (i % 2 == 0 ? a : b).add(sortedRest.get(i));

        while (a.size() > 20 && b.size() < 20) b.add(a.remove(a.size() - 1));
        while (b.size() > 20 && a.size() < 20) a.add(b.remove(b.size() - 1));

        @SuppressWarnings("unchecked")
        List<Card>[] halves = new List[]{a, b};
        return halves;
    }

    /** Detecta las líneas de evolución (base con hijos y sus descendientes) presentes en la ficha. */
    private static List<List<Card>> evolutionChains(List<CardDatabase.CardRow> rows) {
        List<List<Card>> chains = new ArrayList<>();
        for (CardDatabase.CardRow row : rows) {
            if (!row.isBase()) continue;
            List<Card> chain = new ArrayList<>();
            CardDatabase.CardRow current = row;
            while (current != null) {
                chain.add(current.toCard());
                CardDatabase.CardRow next = null;
                for (CardDatabase.CardRow r : rows) {
                    if (r.evolvesFromId() != null && r.evolvesFromId().equals(current.id())) { next = r; break; }
                }
                current = next;
            }
            if (chain.size() >= 2) chains.add(chain);
        }
        return chains;
    }

    private static boolean containsName(List<Card> cards, Card target) {
        for (Card card : cards) if (card.equals(target)) return true;
        return false;
    }
}