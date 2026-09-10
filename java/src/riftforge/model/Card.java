package riftforge.model;

import java.util.Objects;

/** Entidad de dominio. No conoce las estructuras ni las reglas del motor. */
public final class Card {
    private final String name;
    private final Element element;
    private final CardType type;
    private final int manaCost;
    private final int attack;
    private final int health;

    public Card(String name, Element element, CardType type, int manaCost, int attack, int health) {
        if (Objects.requireNonNull(name, "El nombre no puede ser nulo").isBlank()) throw new IllegalArgumentException("El nombre no puede estar vacío");
        Objects.requireNonNull(element, "El elemento no puede ser nulo");
        Objects.requireNonNull(type, "El tipo no puede ser nulo");
        if (manaCost < 0 || attack < 0 || health < 0) throw new IllegalArgumentException("Los valores no pueden ser negativos");
        this.name = name;
        this.element = element;
        this.type = type;
        this.manaCost = manaCost;
        this.attack = attack;
        this.health = health;
    }

    public String name() { return name; }
    public Element element() { return element; }
    public CardType type() { return type; }
    public int manaCost() { return manaCost; }
    public int attack() { return attack; }
    public int health() { return health; }
    public int attackWithBonus(Rift rift) { return attack + (element == rift.bonusElement() ? rift.attackBonus() : 0); }

    @Override public String toString() {
        return "%s | %s | coste %d | ATK %d | HP %d".formatted(name, element, manaCost, attack, health);
    }
}
