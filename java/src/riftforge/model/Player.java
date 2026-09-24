package riftforge.model;

import riftforge.structures.LinkedStack;
import riftforge.structures.SinglyLinkedList;

import java.util.Objects;

/** Estado que pertenece a cada duelista. */
public final class Player {
    private static final int MAX_MANA = 10;
    private final String name;
    private final LinkedStack<Card> deck = new LinkedStack<>();
    private final LinkedStack<Card> graveyard = new LinkedStack<>();
    /** Criaturas en juego: solo ellas atacan (Personajes y Criaturas del catálogo). */
    private final SinglyLinkedList<Card> field = new SinglyLinkedList<>();
    /** Bonos permanentes aportados por las Mejoras equipadas (+ATQ al campo). */
    private int fieldAttackBonus;
    /** Blindaje absorbente (defensa de las Mejoras equipadas) antes del daño. */
    private int shieldPool;
    private int mana;
    private int life;

    public Player(String name, int mana, int life) {
        if (Objects.requireNonNull(name, "El nombre no puede ser nulo").isBlank()) throw new IllegalArgumentException("El nombre no puede estar vacío");
        if (mana < 0 || life < 0) throw new IllegalArgumentException("El maná y la vida no pueden ser negativos");
        this.name = name;
        this.mana = Math.min(mana, MAX_MANA);
        this.life = life;
    }

    public String name() { return name; }
    public int mana() { return mana; }
    public int life() { return life; }
    public LinkedStack<Card> deck() { return deck; }
    public LinkedStack<Card> graveyard() { return graveyard; }
    /** Campo de batalla (lista simple de criaturas propias). */
    public SinglyLinkedList<Card> field() { return field; }
    public int fieldAttackBonus() { return fieldAttackBonus; }
    public int shieldPool() { return shieldPool; }
    public boolean canPay(Card card) { return mana >= card.manaCost(); }
    public void pay(Card card) { if (!canPay(card)) throw new IllegalStateException("Mana insuficiente"); mana -= card.manaCost(); }
    public void receiveDamage(int damage) { life = Math.max(0, life - damage); }
    public void heal(int amount) { life += Math.max(0, amount); }
    public boolean isDefeated() { return life == 0; }
    public void restoreMana(int amount) { mana = Math.min(MAX_MANA, mana + amount); }

    /** Aplica un bono de ataque permanente al campo (efecto de las Mejoras). */
    public void buffFieldAttack(int amount) { fieldAttackBonus += amount; }
    /** Suma blindaje absorbente (defensa de las Mejoras equipadas). */
    public void addShield(int amount) { shieldPool += Math.max(0, amount); }
    /** Reduce el blindaje al recibir daño; devuelve el daño que debe golpear la vida. */
    public int absorb(int damage) {
        int absorbed = Math.min(shieldPool, damage);
        shieldPool -= absorbed;
        return damage - absorbed;
    }
}
