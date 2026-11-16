package cardistry.model;

/**
 * Un familier qui combat aux côtés du joueur (l'alien de Sassy ; plus tard, des trinkets ou objets
 * pourront en invoquer). Il a ses propres PV, que la DEF du joueur ne protège pas ; les statuts ne le
 * touchent pas. Une attaque adverse peut le viser à la place du joueur (annoncé dans l'intent). À 0
 * PV, il est assommé jusqu'à la fin du combat et revient au combat suivant avec tous ses PV.
 */
public final class Familiar extends Combatant {

    /**
     * Ce qui définit un familier.
     *
     * @param id nom de ses images ({@code images/player/<id>.<animation>.<n>.png})
     * @param targetChance chance qu'une attaque adverse le vise à la place du joueur
     */
    public record Spec(String id, String name, double maxHp, double targetChance) {
    }

    private final Spec spec;
    /** La prochaine attaque annoncée le vise. */
    private boolean targeted;

    public Familiar(Spec spec) {
        super(spec.maxHp());
        this.spec = spec;
    }

    public Spec spec() {
        return spec;
    }

    public String id() {
        return spec.id();
    }

    public String name() {
        return spec.name();
    }

    public double maxHp() {
        return spec.maxHp();
    }

    /** Assommé (0 PV) : il n'agit plus et n'est plus visé jusqu'à la fin du combat. */
    public boolean isKnockedOut() {
        return !isAlive();
    }

    /** {@code true} si la prochaine attaque annoncée de l'adversaire le vise. */
    public boolean isTargeted() {
        return targeted;
    }

    public void setTargeted(boolean value) {
        targeted = value;
    }

    /** Nouveau combat : tous ses PV, plus visé. */
    public void restore() {
        heal(maxHp() - hp());
        targeted = false;
    }
}
