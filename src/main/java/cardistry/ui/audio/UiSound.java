package cardistry.ui.audio;

/** Sons d'interface (touches), déclenchés par l'interface elle-même. */
public enum UiSound {
    CURSOR("sfx.ui.cursor"),
    CONFIRM("sfx.ui.confirm"),
    BACK("sfx.ui.back");

    private final String tableName;

    UiSound(String tableName) {
        this.tableName = tableName;
    }

    public String tableName() {
        return tableName;
    }
}
