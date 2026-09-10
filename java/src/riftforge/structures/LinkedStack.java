package riftforge.structures;

import java.util.NoSuchElementException;

/** Pila LIFO usada tanto por el deck como por el cementerio. */
public final class LinkedStack<T> {
    private Node<T> top;
    private int size;
    public void push(T value) { Node<T> node = new Node<>(value); node.next = top; top = node; size++; }
    public T pop() { if (top == null) throw new NoSuchElementException("Pila vacía"); T value = top.data; top = top.next; size--; return value; }
    public T peek() { if (top == null) throw new NoSuchElementException("Pila vacía"); return top.data; }
    public boolean isEmpty() { return top == null; }
    public int size() { return size; }
}
