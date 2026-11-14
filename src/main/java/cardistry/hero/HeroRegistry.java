package cardistry.hero;

import cardistry.core.EffectContext;
import cardistry.core.TemporaryEffect;
import cardistry.core.event.GameEvent;
import cardistry.core.event.GameHooks;
import cardistry.core.stats.StatModifier;
import cardistry.core.stats.StatType;
import cardistry.model.Element;
import java.util.List;
import java.util.NoSuchElementException;

/** Registre des personnages jouables : tout leur contenu (chiffres compris) est ici. */
public final class HeroRegistry {

    // --- Louis : MAN ---
    /** Multiplicateur du premier sort de chaque combat. */
    public static final double MAN_FIRST_SPELL = 1.5;
    /** Multiplicateur du malus de Gueule de bois. */
    public static final double MAN_HANGOVER = 2;

    // --- Sassy : wadayatalkinabit ---
    /** PV max en plus. */
    public static final double SASSY_BONUS_HP = 30;
    /** Dégâts bruts de l'alien, à chaque tour. */
    public static final double ALIEN_DAMAGE = 4;
    /** Chance que l'alien rate son tour (il fume un joint). */
    public static final double ALIEN_MISS_CHANCE = 0.10;

    public static final Hero LOUIS = new Hero("louis", "Louis", "MAN",
            "La première attaque de chaque combat inflige +50 % de dégâts. Le malus de Gueule de bois est doublé.",
            List.of(StatModifier.multiply(StatType.HANGOVER_SEVERITY, MAN_HANGOVER, Hero.modifierSource("louis"))),
            new GameHooks() {
                @Override
                public void onCombatStart(GameEvent.CombatStarted event, EffectContext ctx) {
                    ctx.addEffect(TemporaryEffect.builder("man", "MAN")
                            .summary("première attaque ×" + MAN_FIRST_SPELL)
                            .charges(1)
                            .multipliesNextSpell(MAN_FIRST_SPELL)
                            .build());
                }
            });

    public static final Hero DAVE = new Hero("dave", "Dave", "Full skill Mr. Le Juge",
            "Un sort du même élément que le terrain et qui bat l'élément de l'adversaire te soigne des PV"
                    + " qu'il lui enlève.",
            List.of(),
            new GameHooks() {
                @Override
                public void onSpellHit(GameEvent.SpellHit event, EffectContext ctx) {
                    Element spell = event.spell().element();
                    if (spell == ctx.state().terrain().element() && spell.beats(event.enemy().element())) {
                        ctx.announce("Full skill Mr. Le Juge : le sort te soigne.");
                        ctx.healPlayer(event.hpLost());
                    }
                }
            });

    public static final Hero SASSY = new Hero("sassy", "Sassy", "wadayatalkinabit",
            "+30 PV max. À chaque tour, son alien inflige 4 dégâts à l'adversaire (10 % de chance qu'il fume un"
                    + " joint à la place).",
            List.of(StatModifier.flat(StatType.MAX_HP, SASSY_BONUS_HP, Hero.modifierSource("sassy"))),
            new GameHooks() {
                @Override
                public void onTurnStart(GameEvent.TurnStarted event, EffectContext ctx) {
                    if (!ctx.state().hasEnemy() || !ctx.state().currentEnemy().isAlive()) {
                        return;
                    }
                    // Tirage défavorable au joueur : pas un tirage de chance (CLAUDE.md ne le double pas).
                    if (ctx.rng().nextDouble() < ALIEN_MISS_CHANCE) {
                        ctx.announce("L'alien rate son tour : il fume un joint à la place.");
                    } else {
                        ctx.announce("L'alien de Sassy attaque.");
                        ctx.damageEnemy(ALIEN_DAMAGE);
                    }
                }
            });

    private static final List<Hero> ALL = List.of(LOUIS, DAVE, SASSY);

    private HeroRegistry() {
    }

    /** Personnages proposés au début d'une partie, dans l'ordre d'affichage. */
    public static List<Hero> all() {
        return ALL;
    }

    public static Hero hero(String id) {
        return ALL.stream().filter(h -> h.id().equals(id)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("Personnage inconnu : " + id));
    }
}
