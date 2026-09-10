package riftforge.structures;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Cola enlazada circular para la iniciativa.
 * {@code rear.next} siempre apunta al frente: así no hay un final de ronda.
 */
public final class CircularQueue<T> {
    private Node<T> rear;
    private int size;

    public void enqueue(T value) {
        Node<T> node = new Node<>(value);
        if (rear == null) {
            rear = node;
            node.next = node;
        } else {
            node.next = rear.next;
            rear.next = node;
            rear = node;
        }
        size++;
    }

    public T dequeue() {
        if (rear == null) throw new NoSuchElementException("Cola circular vacía");
        Node<T> front = rear.next;
        if (front == rear) rear = null;
        else rear.next = front.next;
        size--;
        return front.data;
    }

    public T peek() {
        if (rear == null) throw new NoSuchElementException("Cola circular vacía");
        return rear.next.data;
    }

    /** Devuelve una vista desde el frente sin alterar el ciclo. */
    public List<T> snapshot() {
        List<T> values = new ArrayList<>();
        if (rear == null) return values;
        Node<T> current = rear.next;
        do {
            values.add(current.data);
            current = current.next;
        } while (current != rear.next);
        return values;
    }

    public int size() { return size; }
}
