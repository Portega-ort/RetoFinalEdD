package riftforge.structures;

import java.util.NoSuchElementException;

/** Lista circular: current.next vuelve al primer terreno sin usar null como final. */
public final class CircularLinkedList<T> {
    private Node<T> current;
    private int size;
    /** Añade al final lógico para que la rotación respete el orden de configuración. */
    public void add(T value) {
        Node<T> node = new Node<>(value);
        if (current == null) { current = node; node.next = node; }
        else {
            Node<T> tail = current;
            while (tail.next != current) tail = tail.next;
            node.next = current;
            tail.next = node;
        }
        size++;
    }
    public T current() { if (current == null) throw new NoSuchElementException("Lista circular vacía"); return current.data; }
    public T rotate() { current = current.next; return current.data; }
    public int size() { return size; }
}
