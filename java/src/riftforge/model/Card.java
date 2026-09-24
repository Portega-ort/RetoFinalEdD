package riftforge.model;

import java.util.Objects;

/** Entidad de dominio. No conoce las estructuras ni las reglas del motor. */
public final class Card {
    private final String uid;
    private final String name;
    private final Element element;
    private final CardType type;
    private final int manaCost;
    private final int attack;
    private final int health;
    private final boolean special;
    private final String specialAbility;
    /** Daño acumulado en combate; solo cambia el HP actual, nunca los valores base. */
    private int damageTaken;

    public Card(String name, Element element, CardType type, int manaCost, int attack, int health) {
        this(name, name, element, type, manaCost, attack, health, false, "");
    }

    public Card(String name, Element element, CardType type, int manaCost, int attack, int health,
                boolean special, String specialAbility) {
        this(name, name, element, type, manaCost, attack, health, special, specialAbility);
    }

    public Card(String uid, String name, Element element, CardType type, int manaCost, int attack, int health,
                boolean special, String specialAbility) {
        if (Objects.requireNonNull(name, "El nombre no puede ser nulo").isBlank()) throw new IllegalArgumentException("El nombre no puede estar vacío");
        Objects.requireNonNull(element, "El elemento no puede ser nulo");
        Objects.requireNonNull(type, "El tipo no puede ser nulo");
        if (manaCost < 0 || attack < 0 || health < 0) throw new IllegalArgumentException("Los valores no pueden ser negativos");
        if (special && (specialAbility == null || specialAbility.isBlank())) throw new IllegalArgumentException("Una carta especial necesita describir su habilidad");
        this.uid = uid == null || uid.isBlank() ? name : uid;
        this.name = name;
        this.element = element;
        this.type = type;
        this.manaCost = manaCost;
        this.attack = attack;
        this.health = health;
        this.special = special;
        this.specialAbility = specialAbility == null ? "" : specialAbility;
    }

    /** Identificador breve de la carta; coincide con el nombre de su imagen ({@code uid}.jpeg). */
    public String id() { return uid; }
    public String name() { return name; }
    public Element element() { return element; }
    public CardType type() { return type; }
    public int manaCost() { return manaCost; }
    public int attack() { return attack; }
    public int health() { return health; }
    /** @return true si la carta tiene habilidad especial que resuelve antes que las normales. */
    public boolean special() { return special; }
    /** Texto del efecto de la carta; se muestra en la interfaz y se resume en consola si es especial. */
    public String specialAbility() { return specialAbility; }
    public int attackWithBonus(Rift rift) { return attack + (element == rift.bonusElement() ? rift.attackBonus() : 0); }
    /** Daño acumulado en combate (para que las criaturas se ataquen entre sí). */
    public int damageTaken() { return damageTaken; }
    /** Vida actual de combate: HP base menos el daño recibido. */
    public int remainingHealth() { return health - damageTaken; }
    public boolean isDestroyed() { return remainingHealth() <= 0; }
    /** Reduce la vida de combate; al llegar a 0 la criatura se destruye. */
    public void takeDamage(int amount) { damageTaken += Math.max(0, amount); }
    /** Reinicia el daño (se usa cuando la carta regresa al deck tras reciclar el cementerio). */
    public void clearDamage() { damageTaken = 0; }

    /** La identidad de la carta es su nombre: permite indexarla en el HashMap y encontrarla en el árbol. */
    @Override public boolean equals(Object other) { return this == other || (other instanceof Card c && name.equals(c.name)); }
    @Override public int hashCode() { return name.hashCode(); }

    @Override public String toString() {
        String base = "%s | %s | coste %d | ATK %d | HP %d".formatted(name, element, manaCost, attack, health);
        return special ? base + " [ESPECIAL: " + specialAbility + "]" : base;
    }
}
