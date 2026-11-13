package cardistry.items;

import cardistry.core.EffectContext;
import cardistry.core.event.GameEvent;
import cardistry.core.event.GameHooks;
import cardistry.core.stats.StatModifier;
import cardistry.core.stats.StatType;
import java.util.List;

/** Registre des bonus de set (voir GAME_DESIGN.md). Noms provisoires. */
public final class SetBonuses {

    private SetBonuses() {
    }

    private static StatModifier percent(StatType stat, double value) {
        return StatModifier.percent(stat, value, "");
    }

    private static StatModifier flat(StatType stat, double value) {
        return StatModifier.flat(stat, value, "");
    }

    /** +{@code value} sur les trois stats de dégâts élémentaires. */
    private static List<StatModifier> elementalDamage(double value) {
        return List.of(percent(StatType.FIRE_DAMAGE, value), percent(StatType.WATER_DAMAGE, value),
                percent(StatType.NATURE_DAMAGE, value));
    }

    private static List<StatModifier> with(StatModifier first, List<StatModifier> rest) {
        List<StatModifier> all = new java.util.ArrayList<>(rest);
        all.add(0, first);
        return all;
    }

    public static List<SetBonus> all() {
        return List.of(
                new SetBonus(Tag.EPFL, "Cursus", List.of(
                        new SetBonus.Tier(2, "+10 % dégâts élémentaires", elementalDamage(0.10)),
                        new SetBonus.Tier(3, "+1 taille de main", List.of(flat(StatType.HAND_SIZE, 1))),
                        new SetBonus.Tier(4, "+25 PV max et +15 % dégâts élémentaires",
                                with(flat(StatType.MAX_HP, 25), elementalDamage(0.15))))),
                new SetBonus(Tag.PARTY, "Before → Afterparty", List.of(
                        new SetBonus.Tier(2, "+1 clope au début de chaque combat", List.of(), new GameHooks() {
                            @Override
                            public void onCombatStart(GameEvent.CombatStarted event, EffectContext ctx) {
                                ctx.addCigarettes(1);
                            }
                        }),
                        new SetBonus.Tier(3, "Seuil de Gueule de bois +1", List.of(flat(StatType.HANGOVER_THRESHOLD, 1))),
                        new SetBonus.Tier(4, "Chaque boisson soigne aussi 5 PV", List.of(), new GameHooks() {
                            @Override
                            public void onItemUsed(GameEvent.ItemUsed event, EffectContext ctx) {
                                if (event.item().tags().contains(Tag.DRINK)) {
                                    ctx.healPlayer(5);
                                }
                            }
                        }))),
                new SetBonus(Tag.HEAL, "", List.of(
                        new SetBonus.Tier(2, "+15 % soins", List.of(percent(StatType.HEAL_EFFICIENCY, 0.15))))),
                new SetBonus(Tag.DRAW, "", List.of(
                        new SetBonus.Tier(2, "+1 carte dans la main de départ", List.of(flat(StatType.STARTING_HAND, 1))))));
    }
}
