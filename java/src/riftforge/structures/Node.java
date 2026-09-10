package riftforge.structures;

/** Nodo reutilizado por las estructuras enlazadas; no se expone fuera de este package. */
final class Node<T> {
    T data;
    Node<T> next;
    Node<T> previous;
    Node(T data) { this.data = data; }
}
