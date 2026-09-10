package riftforge.structures;

import java.util.ArrayList;
import java.util.List;

/** Lista doble: permite reproducir el historial hacia delante y hacia atrás. */
public final class DoublyLinkedList<T> {
    private Node<T> head;
    private Node<T> tail;
    private int size;
    public void addLast(T value) { Node<T> node = new Node<>(value); if (tail == null) head = tail = node; else { tail.next = node; node.previous = tail; tail = node; } size++; }
    public List<T> forward() { return traverse(head, true); }
    public List<T> backward() { return traverse(tail, false); }
    private List<T> traverse(Node<T> start, boolean forward) { List<T> result = new ArrayList<>(); for (Node<T> current = start; current != null; current = forward ? current.next : current.previous) result.add(current.data); return result; }
    public int size() { return size; }
}
