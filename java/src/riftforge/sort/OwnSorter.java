package riftforge.sort;

import riftforge.model.Card;
import riftforge.model.CardType;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordenamiento propio exigido para la Entrega Final: sin usar
 * {@code java.util.Arrays.sort} ni {@code java.util.Collections.sort}, se
 * implementa Insertion Sort a mano sobre una copia de la colección y se usa
 * para ordenar el catálogo por coste de maná y por tipo de carta (vista de
 * colección).
 *
 * <p><b>Requisito de estabilidad y motivación:</b> con mazos de unas decenas de
 * cartas, Insertion Sort es estable (no permuta cartas de igual coste o de igual
 * tipo), es eficiente en conjuntos casi ordenados y deja un trazado O(n²) fácil
 * de explicar y documentar, a diferencia de un sort de biblioteca que oculta el
 * algoritmo.
 */
public final class OwnSorter {
    private OwnSorter() {
    }

    /**
     * Ordena de menor a mayor coste de maná (estable) y devuelve una nueva
     * lista; la colección recibida no se modifica.
     */
    public static List<Card> insertionSortByCost(List<Card> input) {
        return insertionSort(input, (left, right) -> Integer.compare(left.manaCost(), right.manaCost()));
    }

    /**
     * Agrupa las cartas por tipo (estable) y devuelve una nueva lista; la
     * colección recibida no se modifica.
     *
     * <p>El criterio es el ordinal de {@link CardType}, así que los bloques
     * salen en el orden de declaración del enum (criaturas, hechizos, equipos)
     * y dentro de cada bloque se conserva el orden de entrada, que es el que
     * traía la lista de catálogo.
     */
    public static List<Card> insertionSortByType(List<Card> input) {
        return insertionSort(input, (left, right) -> Integer.compare(left.type().ordinal(), right.type().ordinal()));
    }

    /**
     * Insertion Sort genérico: la comparación es estricta ({@code >}) y por eso
     * el algoritmo es estable — dos cartas que la comparación considera
     * equivalentes no se intercambian entre sí y conservan su orden relativo.
     *
     * @param input       lista de origen; no se modifica
     * @param comparison  orden natural: negativo si {@code left} va antes
     * @return copia de {@code input} ordenada
     */
    private static List<Card> insertionSort(List<Card> input, java.util.Comparator<Card> comparison) {
        List<Card> cards = new ArrayList<>(input);
        for (int i = 1; i < cards.size(); i++) {
            Card key = cards.get(i);
            int j = i - 1;
            while (j >= 0 && comparison.compare(cards.get(j), key) > 0) {
                cards.set(j + 1, cards.get(j));
                j--;
            }
            cards.set(j + 1, key);
        }
        return cards;
    }
}