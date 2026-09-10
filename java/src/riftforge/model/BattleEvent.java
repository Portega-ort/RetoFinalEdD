package riftforge.model;

/** Registro inmutable: cada nodo del historial conserva una jugada auditable. */
public record BattleEvent(int turn, String description) {
    @Override public String toString() { return "Turno " + turn + ": " + description; }
}
