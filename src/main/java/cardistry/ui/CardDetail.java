package cardistry.ui;

import static cardistry.ui.Format.num;

import cardistry.core.GameState;
import cardistry.core.stats.StatType;
import cardistry.core.status.StatusTarget;
import cardistry.model.Combatant;
import cardistry.model.Element;
import cardistry.model.ElementRules;
import cardistry.model.Enemy;
import cardistry.model.card.Card;
import cardistry.model.card.DeadCard;
import cardistry.model.card.HealCard;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import java.util.ArrayList;
import java.util.List;

/**
 * Fiche détaillée d'une carte (touche V) : type, rareté, valeur de base, effet réel en combat avec
 * chaque multiplicateur, pouvoir ou effet de terrain, et ce que son élément bat. Partagée par la
 * fenêtre, le terminal et le mode texte.
 */
public final class CardDetail {

    /** Une ligne de la fiche : {@code kind} sert à la colorer. */
    public record Line(String text, Kind kind) {
    }

    public enum Kind { TITLE, SECTION, TEXT, TOTAL, POWER, DIM }

    private CardDetail() {
    }

    /**
     * Lignes de la fiche ; {@code inCombat} : effet réel contre l'adversaire actuel (sinon valeurs de
     * base seulement, comme au shop).
     */
    public static List<Line> lines(GameState state, Card card, boolean inCombat) {
        List<Line> lines = new ArrayList<>();
        lines.add(new Line(card.name(), Kind.TITLE));
        String rarity = card.rarity() == null ? "" : " · " + card.rarity().displayName();
        lines.add(new Line(capitalize(CardText.kind(card)) + rarity, Kind.DIM));
        String base = CardText.baseValue(card);
        if (!base.isEmpty()) {
            lines.add(new Line("Valeur de base : " + base, Kind.TEXT));
        }
        String extra = CardText.extra(card);
        if (!extra.isEmpty()) {
            lines.add(new Line(extra, Kind.POWER));
        }
        if (inCombat && state.hasEnemy() && !(card instanceof DeadCard)) {
            lines.add(new Line("", Kind.TEXT));
            lines.add(new Line("Si tu la joues maintenant", Kind.SECTION));
            lines.addAll(now(state, card));
        }
        if (card.element() != null) {
            lines.add(new Line("", Kind.TEXT));
            lines.add(new Line("Élément " + card.element().displayName(), Kind.SECTION));
            lines.add(new Line(matchups(card.element()), Kind.TEXT));
        }
        return lines;
    }

    private static List<Line> now(GameState state, Card card) {
        List<Line> lines = new ArrayList<>();
        Enemy enemy = state.currentEnemy();
        switch (card) {
            case SpellCard s when s.damage() <= 0 && !s.hasDamageRule() -> lines.add(new Line(
                    "Aucun dégât à " + enemy.name() + (s.power().isPresent() ? " : son pouvoir agit" : ""), Kind.TOTAL));
            case SpellCard s when s.isUnmultiplied() -> lines.add(new Line("Dégâts non multipliés : "
                    + s.damageLabel().orElse(num(s.damage())) + " à " + enemy.name(), Kind.TOTAL));
            case SpellCard s -> {
                double element = ElementRules.spellMultiplier(s.element(), enemy.element());
                lines.add(new Line("Contre " + enemy.name() + " (" + enemy.element().displayName() + ") : ×"
                        + num(element) + matchupNote(s.element(), enemy.element()), Kind.TEXT));
                double stats = state.playerStats().get(StatType.damageMultiplier(s.element()));
                if (stats != 1.0) {
                    lines.add(new Line("Tes bonus de dégâts " + s.element().displayName() + " : ×" + num(stats), Kind.TEXT));
                }
                double hypnosis = state.hypnosisMultiplier(StatusTarget.ENEMY);
                if (hypnosis != 1.0) {
                    lines.add(new Line("Adversaire hypnotisé : ×" + num(hypnosis), Kind.TEXT));
                }
                state.nextSpellBoost().ifPresent(b -> lines.add(new Line(b.name() + " : ×" + num(b.spellMultiplier()),
                        Kind.TEXT)));
                if (state.terrain().effect() == TerrainCard.Effect.CEILING) {
                    lines.add(new Line("Fractale : total arrondi à l'entier supérieur", Kind.TEXT));
                }
                lines.addAll(statLines(state, s, enemy));
                double mult = state.spellMultiplier(s);
                String total = s.hasDamageRule()
                        ? s.damageLabel().orElse("?") + " ×" + num(mult) + " dégâts à " + enemy.name()
                        : damageFormula(state, s, true) + " dégâts à " + enemy.name();
                lines.add(new Line("Total : " + total, Kind.TOTAL));
            }
            case HealCard h when h.amount() <= 0 -> lines.add(new Line("Aucun soin direct : son pouvoir agit",
                    Kind.TOTAL));
            case HealCard h -> {
                double terrain = ElementRules.healMultiplier(h.element(), state.terrain().element());
                lines.add(new Line("Terrain " + state.terrain().name() + " : ×" + num(terrain), Kind.TEXT));
                double efficiency = state.playerStats().get(StatType.HEAL_EFFICIENCY)
                        * state.playerStats().get(StatType.healMultiplier(h.element()));
                if (efficiency != 1.0) {
                    lines.add(new Line("Tes bonus de soin : ×" + num(efficiency), Kind.TEXT));
                }
                double mind = state.playerStats().get(StatType.MIND);
                if (mind != 0) {
                    lines.add(new Line("Ton ESP : " + (mind > 0 ? "+" : "−") + num(Math.abs(mind))
                            + " PV (avant les multiplicateurs)", Kind.TEXT));
                }
                double mult = state.healMultiplier(h);
                double raw = state.healAmount(h);
                double missing = Math.max(0, state.maxHp() - state.player().hp());
                double healed = state.healingUncapped() ? raw : Math.min(raw, missing);
                lines.add(new Line("Total : " + healBase(state, h) + " ×" + num(mult) + " → +" + num(Combatant.round(healed))
                        + " PV" + (healed < raw ? " (PV max)" : ""), Kind.TOTAL));
            }
            case TerrainCard t -> {
                double after = ElementRules.terrainAttackMultiplier(t.element(), enemy.element());
                lines.add(new Line("Attaque de " + enemy.name() + " : ×" + num(state.enemyAttackMultiplier())
                        + " → ×" + num(after) + " (selon l'élément du terrain)", Kind.TOTAL));
            }
            case DeadCard d -> {
            }
        }
        return lines;
    }

    /** Lignes des stats d'un sort : « + FOR », « − DEF » de l'adversaire, chance de critique. */
    private static List<Line> statLines(GameState state, SpellCard spell, Enemy enemy) {
        List<Line> lines = new ArrayList<>();
        cardistry.core.stats.StatType offenseStat = cardistry.core.CombatFormula.offensiveStat(spell.element());
        double offense = cardistry.core.CombatFormula.offense(spell.element(), state.playerStats());
        String statName = offenseStat != null ? offenseStat.abbreviation() : "meilleure stat (GLITCH)";
        if (!spell.addsOffenseStat()) {
            lines.add(new Line("Ta " + statName + " est déjà comptée dans ses dégâts", Kind.TEXT));
        } else if (offense != 0) {
            lines.add(new Line("+ " + statName + " : " + (offense > 0 ? "+" : "−") + num(Math.abs(offense))
                    + " dégâts (avant les multiplicateurs)", Kind.TEXT));
        }
        double defense = enemy.stat(StatType.DEFENSE);
        if (defense != 0) {
            double kept = cardistry.core.CombatFormula.effectiveDefense(defense, spell.defensePierce());
            String head = "− DEF de " + enemy.name() + " : ";
            if (kept == 0) {
                lines.add(new Line(head + num(defense) + ", ignorée par ce sort", Kind.TEXT));
            } else {
                String note = kept == defense ? "" : " (" + num(defense - kept) + " ignorée par ce sort)";
                lines.add(new Line(head + (kept > 0 ? "−" : "+") + num(Math.abs(kept)) + " dégâts" + note, Kind.TEXT));
            }
        }
        double luck = state.playerStats().get(StatType.LUCK);
        if (spell.sureCriticalLuck() > 0 && luck >= spell.sureCriticalLuck()) {
            lines.add(new Line("Coup critique garanti (CHA ≥ " + spell.sureCriticalLuck() + ") : ×"
                    + num(cardistry.core.CombatFormula.CRIT_MULTIPLIER), Kind.TEXT));
        } else if (luck > 0) {
            lines.add(new Line("Coup critique : " + num(Math.round(cardistry.core.CombatFormula.critChance(luck) * 100))
                    + " % de chance, ×" + num(cardistry.core.CombatFormula.CRIT_MULTIPLIER)
                    + (state.luckyRolls() ? " (tirage doublé)" : ""), Kind.TEXT));
        }
        return lines;
    }

    /**
     * Calcul d'un sort à dégâts fixes : « 10 ×1.5 = 15 », ou avec les stats « (10 + 3) ×1.5 − 2 = 18 »
     * (stat offensive du joueur, DEF de l'adversaire). {@code alwaysMultiplier} : affiche aussi ×1.
     */
    public static String damageFormula(GameState state, SpellCard spell, boolean alwaysMultiplier) {
        double mult = state.spellMultiplier(spell);
        double offense = spell.addsOffenseStat()
                ? cardistry.core.CombatFormula.offense(spell.element(), state.playerStats()) : 0;
        double defense = state.currentEnemy().stat(StatType.DEFENSE);
        String base = offense == 0 ? num(spell.damage())
                : "(" + num(spell.damage()) + (offense > 0 ? " + " : " − ") + num(Math.abs(offense)) + ")";
        String text = base + (alwaysMultiplier || mult != 1.0 ? " ×" + num(mult) : "");
        if (defense != 0) {
            text += (defense > 0 ? " − " : " + ") + num(Math.abs(defense));
        }
        return text + " = " + num(state.spellDamage(spell));
    }

    /** Base d'un soin : « 10 », ou « (10 + 2) » avec l'ESP du joueur. */
    public static String healBase(GameState state, HealCard heal) {
        double mind = state.playerStats().get(StatType.MIND);
        return mind == 0 ? num(heal.amount())
                : "(" + num(heal.amount()) + (mind > 0 ? " + " : " − ") + num(Math.abs(mind)) + ")";
    }

    /** « (feu bat plante) », « (eau bat feu) » ou vide. */
    private static String matchupNote(Element spell, Element enemy) {
        if (spell.beats(enemy)) {
            return " (" + spell.displayName() + " bat " + enemy.displayName() + ")";
        }
        if (enemy.beats(spell)) {
            return " (" + enemy.displayName() + " bat " + spell.displayName() + ")";
        }
        return "";
    }

    /** « Bat : plante · Battu par : eau, error: null ». */
    static String matchups(Element element) {
        if (element.beatsEverything()) {
            return "Bat tout";
        }
        List<String> beats = new ArrayList<>();
        List<String> beatenBy = new ArrayList<>();
        for (Element other : Element.values()) {
            if (element.beats(other)) {
                beats.add(other.displayName());
            }
            if (other.beats(element)) {
                beatenBy.add(other.displayName());
            }
        }
        return "Bat : " + (beats.isEmpty() ? "rien" : String.join(", ", beats))
                + " · Battu par : " + (beatenBy.isEmpty() ? "rien" : String.join(", ", beatenBy));
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
