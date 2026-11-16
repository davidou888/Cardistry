package cardistry.run.map;

/** Types de nœuds de la carte ; le symbole est de l'ASCII pur (affichage et repli). */
public enum NodeType {
    COMBAT('C', "Combat"),
    ELITE('E', "Élite"),
    SHOP('$', "Shop"),
    EVENT('?', "Événement"),
    REST('Z', "Sieste"),
    BOSS('B', "Boss"),
    ALTAR('A', "Autel");

    private final char symbol;
    private final String displayName;

    NodeType(char symbol, String displayName) {
        this.symbol = symbol;
        this.displayName = displayName;
    }

    public char symbol() {
        return symbol;
    }

    public String displayName() {
        return displayName;
    }

    /** Nœuds qui ne se suivent jamais deux fois sur un chemin. */
    public boolean noRepeat() {
        return this == SHOP || this == ELITE || this == REST || this == ALTAR;
    }
}
