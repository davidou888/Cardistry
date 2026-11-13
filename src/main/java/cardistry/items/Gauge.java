package cardistry.items;

/** Jauge d'un trinket (ex. Honoré 3/5), affichée par l'interface. Persiste tant que le trinket est équipé. */
public final class Gauge {

    private final String label;
    private final int max;
    private int value = 0;

    public Gauge(String label, int max) {
        this.label = label;
        this.max = max;
    }

    public String label() {
        return label;
    }

    public int value() {
        return value;
    }

    public int max() {
        return max;
    }

    public boolean isFull() {
        return value >= max;
    }

    /** Ajoute {@code amount} sans dépasser le maximum. */
    public void add(int amount) {
        value = Math.min(max, value + amount);
    }

    public void reset() {
        value = 0;
    }
}
