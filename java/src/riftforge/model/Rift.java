package riftforge.model;

/** Estado temporal de la arena. */
public record Rift(String name, Element bonusElement, int attackBonus) {
    public Rift {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("El nombre no puede estar vacío");
        if (bonusElement == null) throw new IllegalArgumentException("El elemento no puede ser nulo");
        if (attackBonus < 0) throw new IllegalArgumentException("El bono no puede ser negativo");
    }
}
