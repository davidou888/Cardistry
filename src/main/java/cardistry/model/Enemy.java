package cardistry.model;

public class Enemy extends Combatant {

    private final String name;
    private final Element element;
    private final double attack;

    public Enemy(String name, Element element, double hp, double attack) {
        super(hp);
        this.name = name;
        this.element = element;
        this.attack = attack;
    }

    public String name() {
        return name;
    }

    public Element element() {
        return element;
    }

    /** Attaque de base, avant multiplicateur de terrain. */
    public double attack() {
        return attack;
    }
}
