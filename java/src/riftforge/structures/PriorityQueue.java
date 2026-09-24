package riftforge.structures;

/**
 * Cola de prioridad enlazada propia. Las cartas con habilidad especial
 * (alta prioridad) se resuelven primero sin importar cuándo se encolaron;
 * dentro de cada nivel se conserva el orden FIFO.
 *
 * <p>Invariante: {@code head..antesDe(firstNormal)} son todos de alta
 * prioridad y {@code firstNormal..tail} de prioridad normal.
 */
public final class PriorityQueue<T> {
    private Node<T> head;
    private Node<T> tail;
    private Node<T> firstNormal;
    private int size;

    /** Encola con prioridad normal (FIFO entre los normales). */
    public void enqueue(T value) { enqueue(value, false); }

    /**
     * @param highPriority {@code true} para que el elemento salga antes que
     *                     cualquier normal, sin importar el orden de llegada.
     */
    public void enqueue(T value, boolean highPriority) {
        Node<T> node = new Node<>(value);
        if (head == null) {
            head = tail = node;
            firstNormal = node;
        } else if (highPriority) {
            if (firstNormal == null) {
                tail.next = node;
                tail = node;
            } else if (firstNormal == head) {
                node.next = head;
                head = node;
            } else {
                Node<T> before = head;
                while (before.next != firstNormal) before = before.next;
                node.next = firstNormal;
                before.next = node;
            }
        } else {
            tail.next = node;
            tail = node;
            if (firstNormal == null) firstNormal = node;
        }
        size++;
    }

    /** Saca el frente; devuelve {@code null} si está vacía. */
    public T dequeue() {
        if (head == null) return null;
        Node<T> removed = head;
        if (head == firstNormal) firstNormal = head.next;
        head = head.next;
        if (head == null) tail = null;
        size--;
        return removed.data;
    }

    /** Consulta el frente sin sacarlo; {@code null} si está vacía. */
    public T peek() {
        return head == null ? null : head.data;
    }

    public int size() { return size; }
    public boolean isEmpty() { return head == null; }
}
