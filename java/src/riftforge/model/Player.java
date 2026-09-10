package riftforge.model;

import riftforge.structures.LinkedStack;

import java.util.Objects;

/** Estado que pertenece a cada duelista. */
public final class Player {
    private static final int MAX_MANA = 10;
    private final String name;
    private final LinkedStack<Card> deck = new LinkedStack<>();
    private final LinkedStack<Card> graveyard = new LinkedStack<>();
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
    public boolean canPay(Card card) { return mana >= card.manaCost(); }
    public void pay(Card card) { if (!canPay(card)) throw new IllegalStateException("Mana insuficiente"); mana -= card.manaCost(); }
    public void receiveDamage(int damage) { life = Math.max(0, life - damage); }
    public boolean isDefeated() { return life == 0; }
    public void restoreMana(int amount) { mana = Math.min(MAX_MANA, mana + amount); }
}
