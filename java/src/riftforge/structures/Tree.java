package riftforge.structures;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Árbol n-ario con nodos propios, pensado para la línea de evolución de una
 * carta: básica → mejorada → legendaria. Cada nodo guarda su dato y una lista
 * doble enlazada propia con sus hijos.
 *
 * <p>La búsqueda de un padre ({@link #addChild}) y los recorridos
 * {@link #preorder()}/{@link #postorder()} y la {@link #height()} se
 * implementan de forma recursiva: cada llamada resuelve una versión más
 * pequeña del mismo problema (subárboles) sin usar ciclos.
 */
public final class Tree<T> {
    /** Nodo del árbol: dato + referencia a sus nodos hijo. */
    public static final class TreeNode<T> {
        private final T data;
        private final DoublyLinkedList<TreeNode<T>> children = new DoublyLinkedList<>();

        TreeNode(T data) { this.data = data; }
        public T data() { return data; }
        public DoublyLinkedList<TreeNode<T>> children() { return children; }
    }

    private TreeNode<T> root;
    private int size;

    public void setRoot(T value) {
        Objects.requireNonNull(value, "El valor raíz no puede ser nulo");
        root = new TreeNode<>(value);
        size = 1;
    }

    public TreeNode<T> root() { return root; }

    /**
     * Agrega {@code child} como hijo del primer nodo cuyo dato sea igual a
     * {@code parent}. La búsqueda del padre recorre todo el árbol de forma
     * recursiva.
     *
     * @return {@code true} si el padre existía y se agregó el hijo
     */
    public boolean addChild(T parent, T child) {
        TreeNode<T> target = find(root, parent);
        if (target == null || child == null) return false;
        target.children.addLast(new TreeNode<>(child));
        size++;
        return true;
    }

    public boolean contains(T value) { return find(root, value) != null; }
    public int size() { return size; }

    /** Recorrido recursivo en preorden: raíz antes que sus hijos. */
    public List<T> preorder() {
        List<T> result = new ArrayList<>();
        if (root != null) preorder(root, result);
        return result;
    }

    /** Recorrido recursivo en postorden: hijos antes que la raíz. */
    public List<T> postorder() {
        List<T> result = new ArrayList<>();
        if (root != null) postorder(root, result);
        return result;
    }

    /** Altura en aristas: un árbol con un solo nodo tiene altura 0. */
    public int height() { return root == null ? -1 : height(root); }

    private TreeNode<T> find(TreeNode<T> node, T value) {
        if (node == null) return null;
        if (Objects.equals(node.data, value)) return node;
        for (TreeNode<T> child : node.children.forward()) {
            TreeNode<T> found = find(child, value);
            if (found != null) return found;
        }
        return null;
    }

    private void preorder(TreeNode<T> node, List<T> out) {
        out.add(node.data);
        for (TreeNode<T> child : node.children.forward()) preorder(child, out);
    }

    private void postorder(TreeNode<T> node, List<T> out) {
        for (TreeNode<T> child : node.children.forward()) postorder(child, out);
        out.add(node.data);
    }

    private int height(TreeNode<T> node) {
        int max = -1;
        for (TreeNode<T> child : node.children.forward()) max = Math.max(max, height(child));
        return max + 1;
    }
}