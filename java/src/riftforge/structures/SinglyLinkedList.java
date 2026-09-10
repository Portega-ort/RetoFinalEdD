package riftforge.structures;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/** Lista simple: catálogo/binder. Inserción al inicio O(1), búsqueda y borrado O(n). */
public final class SinglyLinkedList<T> implements Iterable<T> {
    private Node<T> head;
    private int size;
    public void add(T value) { Node<T> node = new Node<>(value); node.next = head; head = node; size++; }
    public T find(Predicate<T> condition) { for (T value : this) if (condition.test(value)) return value; return null; }
    public boolean remove(Predicate<T> condition) {
        Node<T> previous = null, current = head;
        while (current != null) { if (condition.test(current.data)) { if (previous == null) head = current.next; else previous.next = current.next; size--; return true; } previous = current; current = current.next; }
        return false;
    }
    public int size() { return size; }
    @Override public Iterator<T> iterator() { return new Iterator<>() { Node<T> current = head; public boolean hasNext() { return current != null; } public T next() { if (!hasNext()) throw new NoSuchElementException(); T value = current.data; current = current.next; return value; } }; }
}
