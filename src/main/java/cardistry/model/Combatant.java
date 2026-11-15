package cardistry.model;

/**
 * Tout ce qui a des points de vie. Les PV sont toujours des <b>entiers</b> : chaque changement est
 * arrondi à l'entier le plus proche au moment où il s'applique (voir {@link #round(double)}).
 */
public abstract class Combatant {

    private int hp;

    protected Combatant(double hp) {
        this.hp = round(hp);
    }

    /** Arrondi des PV : entier le plus proche, x,5 vers le haut. */
    public static int round(double value) {
        return (int) Math.round(value);
    }

    public int hp() {
        return hp;
    }

    /** Retire {@code amount} PV (arrondi) ; renvoie les PV retirés. */
    public int takeDamage(double amount) {
        int lost = round(amount);
        hp -= lost;
        return lost;
    }

    /** Rend {@code amount} PV (arrondi) ; renvoie les PV rendus. */
    public int heal(double amount) {
        int gained = round(amount);
        hp += gained;
        return gained;
    }

    public boolean isAlive() {
        return hp > 0;
    }
}
