package cardistry.core;

import cardistry.core.stats.StatModifier;
import cardistry.core.stats.StatType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Effet temporaire générique (objet, trinket…), géré par le moteur comme les malédictions.
 *
 * <p>Cycle de vie :
 * <ol>
 *   <li><b>en attente</b> pendant {@code delayTurns} débuts de tour (0 = actif tout de suite) ;</li>
 *   <li><b>actif</b> : ses modificateurs de stats s'appliquent, {@code onActivate} est appelé une
 *       fois, {@code onTurnStart} à chaque début de tour ;</li>
 *   <li><b>terminé</b> quand ses tours ({@code activeTurns}) ou ses charges sont épuisés, ou à la
 *       fin du combat s'il n'est pas {@code keepAcrossCombats}.</li>
 * </ol>
 * Les comportements spéciaux (multiplier le prochain sort, annuler la prochaine attaque, sauter
 * des tours) consomment une charge à chaque fois qu'ils servent.
 *
 * <p>Chaque instance est unique (ses modificateurs ont leur propre source).
 */
public final class TemporaryEffect {

    /** Quand le joueur saute son tour à cause de cet effet. */
    public enum SkipRule {
        NONE,
        /** Le prochain tour (consomme une charge). */
        NEXT_TURN,
        /** Les tours pairs de chaque combat (2, 4…) ; ne consomme rien. */
        EVEN_COMBAT_TURNS
    }

    private static final AtomicLong SERIAL = new AtomicLong();

    private final String id;
    private final String name;
    private final String summary;
    private final int delayTurns;
    private final Integer activeTurns;
    private final boolean keepAcrossCombats;
    private final double spellMultiplier;
    private final boolean cancelsEnemyAttack;
    private final String enemySkipMessage;
    private final SkipRule skipRule;
    private final List<Mod> mods;
    private final Consumer<EffectContext> onActivate;
    private final Consumer<EffectContext> onTurnStart;
    private final String modifierSource;

    // --- État ---
    private boolean active;
    private int delayLeft;
    private Integer turnsLeft;
    private Integer chargesLeft;
    private String owner = "";

    private record Mod(StatType stat, StatModifier.Kind kind, double value) {
    }

    private TemporaryEffect(Builder b) {
        this.id = b.id;
        this.name = b.name;
        this.summary = b.summary;
        this.delayTurns = b.delayTurns;
        this.activeTurns = b.activeTurns;
        this.keepAcrossCombats = b.keepAcrossCombats;
        this.spellMultiplier = b.spellMultiplier;
        this.cancelsEnemyAttack = b.cancelsEnemyAttack;
        this.enemySkipMessage = b.enemySkipMessage;
        this.skipRule = b.skipRule;
        this.mods = List.copyOf(b.mods);
        this.onActivate = b.onActivate;
        this.onTurnStart = b.onTurnStart;
        this.modifierSource = "effet:" + id + "#" + SERIAL.incrementAndGet();
        this.delayLeft = b.delayTurns;
        this.chargesLeft = b.charges;
        this.active = false;
    }

    public static Builder builder(String id, String name) {
        return new Builder(id, name);
    }

    // --- Lecture ---

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    /** Ce que fait l'effet, en quelques mots : « prochain sort ×2 ». */
    public String summary() {
        return summary;
    }

    public boolean isActive() {
        return active;
    }

    /** Tours restants avant le déclenchement (0 si actif). */
    public int delayLeft() {
        return delayLeft;
    }

    /** Tours d'effet restants (null = illimité). */
    public Integer turnsLeft() {
        return turnsLeft;
    }

    /** Déclenchements restants (null = illimités). */
    public Integer chargesLeft() {
        return chargesLeft;
    }

    public boolean keepsAcrossCombats() {
        return keepAcrossCombats;
    }

    /** Multiplicateur appliqué au prochain sort (1 = aucun). */
    public double spellMultiplier() {
        return spellMultiplier;
    }

    public boolean cancelsEnemyAttack() {
        return cancelsEnemyAttack;
    }

    /** {@code true} si l'effet fait sauter le prochain tour de l'adversaire (consomme une charge). */
    public boolean skipsEnemyTurn() {
        return enemySkipMessage != null;
    }

    /** Message du tour sauté, avec le nom de l'adversaire. */
    public String enemySkipMessage(String enemyName) {
        return enemySkipMessage == null ? "" : enemySkipMessage.replace("{adversaire}", enemyName);
    }

    public SkipRule skipRule() {
        return skipRule;
    }

    /** Ce qui a posé l'effet (« trinket:… ») quand il doit être retiré avec ; vide sinon. */
    public String owner() {
        return owner;
    }

    /**
     * Texte d'affichage avec la durée restante : « Shot de Jäger : prochain sort ×2 »,
     * « Space cake : +50 % dégâts, 2 tours », « Space cake : dans 3 tours ».
     */
    public String describe() {
        if (!active) {
            return name + " : dans " + delayLeft + (delayLeft > 1 ? " tours" : " tour");
        }
        StringBuilder text = new StringBuilder(name).append(" : ").append(summary);
        if (turnsLeft != null) {
            text.append(", ").append(turnsLeft).append(turnsLeft > 1 ? " tours" : " tour");
        }
        if (chargesLeft != null && chargesLeft > 1) {
            text.append(" (×").append(chargesLeft).append(")");
        }
        return text.toString();
    }

    // --- Transitions (moteur uniquement) ---

    String modifierSource() {
        return modifierSource;
    }

    void assignOwner(String owner) {
        this.owner = owner;
    }

    List<StatModifier> modifiers() {
        List<StatModifier> list = new ArrayList<>();
        for (Mod mod : mods) {
            list.add(new StatModifier(mod.stat(), mod.kind(), mod.value(), modifierSource));
        }
        return list;
    }

    /** Passe à l'état actif ; à appeler une seule fois. */
    void activate() {
        active = true;
        delayLeft = 0;
        turnsLeft = activeTurns;
    }

    /** Un début de tour en attente ; {@code true} si l'effet doit se déclencher maintenant. */
    boolean tickDelay() {
        delayLeft--;
        return delayLeft <= 0;
    }

    /** Un début de tour actif ; {@code true} si la durée est épuisée. */
    boolean tickDuration() {
        if (turnsLeft == null) {
            return false;
        }
        turnsLeft--;
        return turnsLeft <= 0;
    }

    /** Consomme une charge ; {@code true} si l'effet est épuisé. */
    boolean consumeCharge() {
        if (chargesLeft == null) {
            return false;
        }
        chargesLeft--;
        return chargesLeft <= 0;
    }

    int delayTurns() {
        return delayTurns;
    }

    void runOnActivate(EffectContext ctx) {
        onActivate.accept(ctx);
    }

    void runOnTurnStart(EffectContext ctx) {
        onTurnStart.accept(ctx);
    }

    @Override
    public String toString() {
        return "TemporaryEffect[" + id + "]";
    }

    public static final class Builder {
        private final String id;
        private final String name;
        private String summary = "";
        private int delayTurns = 0;
        private Integer activeTurns = null;
        private Integer charges = null;
        private boolean keepAcrossCombats = false;
        private double spellMultiplier = 1.0;
        private boolean cancelsEnemyAttack = false;
        private String enemySkipMessage = null;
        private SkipRule skipRule = SkipRule.NONE;
        private final List<Mod> mods = new ArrayList<>();
        private Consumer<EffectContext> onActivate = ctx -> { };
        private Consumer<EffectContext> onTurnStart = ctx -> { };

        private Builder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        /** Description courte affichée avec la durée restante. */
        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        /** Se déclenche au bout de {@code turns} débuts de tour. */
        public Builder delay(int turns) {
            this.delayTurns = turns;
            return this;
        }

        /** Dure {@code turns} tours une fois déclenché (le tour du déclenchement compris). */
        public Builder lasts(int turns) {
            this.activeTurns = turns;
            return this;
        }

        /** Se termine après {@code count} déclenchements. */
        public Builder charges(int count) {
            this.charges = count;
            return this;
        }

        /** Reste d'un combat à l'autre (sinon : perdu à la fin du combat). */
        public Builder keepAcrossCombats() {
            this.keepAcrossCombats = true;
            return this;
        }

        public Builder multipliesNextSpell(double factor) {
            this.spellMultiplier = factor;
            return this;
        }

        public Builder cancelsNextEnemyAttack() {
            this.cancelsEnemyAttack = true;
            return this;
        }

        /**
         * L'adversaire saute son prochain tour : il n'attaque pas du tout (passe avant les
         * annulations d'attaque, qui restent pour la suivante). {@code message} peut contenir
         * « {adversaire} ».
         */
        public Builder skipsNextEnemyTurn(String message) {
            this.enemySkipMessage = message;
            return this;
        }

        public Builder skips(SkipRule rule) {
            this.skipRule = rule;
            return this;
        }

        /** Bonus en pourcentage pendant que l'effet est actif. */
        public Builder percent(StatType stat, double percent) {
            mods.add(new Mod(stat, StatModifier.Kind.PERCENT, percent));
            return this;
        }

        /** Bonus plat pendant que l'effet est actif. */
        public Builder flat(StatType stat, double value) {
            mods.add(new Mod(stat, StatModifier.Kind.FLAT, value));
            return this;
        }

        /** Action au déclenchement. */
        public Builder onActivate(Consumer<EffectContext> action) {
            this.onActivate = action;
            return this;
        }

        /** Action à chaque début de tour où l'effet est actif. */
        public Builder onTurnStart(Consumer<EffectContext> action) {
            this.onTurnStart = action;
            return this;
        }

        public TemporaryEffect build() {
            return new TemporaryEffect(this);
        }
    }
}
