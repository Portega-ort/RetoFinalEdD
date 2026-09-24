package riftforge.sort;

import riftforge.model.Card;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordenamiento propio exigido para la Entrega Final: sin usar
 * {@code java.util.Arrays.sort} ni {@code java.util.Collections.sort}, se
 * implementa Insertion Sort a mano sobre una copia de la colección y se usa
 * para ordenar el catálogo por coste de maná (vista de colección).
 *
 * <p><b>Requisito de estabilidad y motivación:</b> con mazos de unas decenas de
 * cartas, Insertion Sort es estable (no permuta cartas de igual coste), es
 * eficiente en conjuntos casi ordenados y deja un trazado O(n²) fácil de
 * explicar y documentar, a diferencia de un sort de biblioteca que oculta el
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
        List<Card> cards = new ArrayList<>(input);
        for (int i = 1; i < cards.size(); i++) {
            Card key = cards.get(i);
            int j = i - 1;
            while (j >= 0 && cards.get(j).manaCost() > key.manaCost()) {
                cards.set(j + 1, cards.get(j));
                j--;
            }
            cards.set(j + 1, key);
        }
        return cards;
    }
}