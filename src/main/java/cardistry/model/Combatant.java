package cardistry.model;

/** Tout ce qui a des points de vie. */
public abstract class Combatant {

    private double hp;

    protected Combatant(double hp) {
        this.hp = hp;
    }

    public double hp() {
        return hp;
    }

    public void takeDamage(double amount) {
        hp -= amount;
    }

    public void heal(double amount) {
        hp += amount;
    }

    public boolean isAlive() {
        return hp > 0;
    }
}
