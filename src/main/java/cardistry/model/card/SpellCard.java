package cardistry.model.card;

import cardistry.core.EffectContext;
import cardistry.items.Rarity;
import cardistry.model.Element;
import java.util.function.Consumer;

/**
 * Sortilège : inflige des dégâts à l'adversaire. Ses dégâts de base peuvent être calculés au moment
 * du coup ({@link #withDamageRule}) et échapper aux multiplicateurs ({@link #unmultiplied()}).
 */
public final class SpellCard extends Card {

    private final double damage;
    /** {@code null} : dégâts fixes ({@link #damage()}). */
    private DamageRule rule;
    /** Texte des dégâts quand ils sont calculés (« 1-40 », « 10 / 20 »). */
    private String damageLabel;
    private boolean unmultiplied;

    public SpellCard(String name, Element element, double damage, Rarity rarity) {
        super(name, element, rarity);
        this.damage = damage;
    }

    /** Dégâts de base affichés (positifs ou nuls), avant multiplicateurs. */
    public double damage() {
        return damage;
    }

    /** Dégâts de base de ce coup : la règle s'il y en a une, sinon {@link #damage()}. */
    public double baseDamage(EffectContext ctx) {
        return rule != null ? rule.damage(ctx) : damage;
    }

    /** {@code true} si les dégâts sont calculés au moment du coup. */
    public boolean hasDamageRule() {
        return rule != null;
    }

    /** Texte des dégâts calculés (« 1-40 ») ; vide pour des dégâts fixes (l'UI affiche {@link #damage()}). */
    public java.util.Optional<String> damageLabel() {
        return java.util.Optional.ofNullable(damageLabel);
    }

    /** {@code true} si aucun multiplicateur ne s'applique (élément, stats, hypnose, Shot de Jäger). */
    public boolean isUnmultiplied() {
        return unmultiplied;
    }

    /** Copie avec un pouvoir (texte + effet après les dégâts). */
    public SpellCard withPower(String text, Consumer<EffectContext> effect) {
        return withPower(copy(), new CardPower(text, effect));
    }

    /** Copie dont les dégâts de base sont calculés au moment du coup ({@code label} : texte affiché). */
    public SpellCard withDamageRule(String label, DamageRule damageRule) {
        SpellCard copy = copy();
        copy.rule = damageRule;
        copy.damageLabel = label;
        return copy;
    }

    /** Copie dont les dégâts ne sont pas multipliés. */
    public SpellCard unmultiplied() {
        SpellCard copy = copy();
        copy.unmultiplied = true;
        return copy;
    }

    @Override
    public SpellCard copy() {
        SpellCard copy = samePower(this, new SpellCard(name(), element(), damage, rarity()));
        copy.rule = rule;
        copy.damageLabel = damageLabel;
        copy.unmultiplied = unmultiplied;
        return copy;
    }
}
