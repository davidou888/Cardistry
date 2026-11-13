package cardistry.items;

import static cardistry.items.Rarity.COMMON;
import static cardistry.items.Rarity.LEGENDARY;
import static cardistry.items.Rarity.RARE;
import static cardistry.items.Rarity.UNCOMMON;

import cardistry.core.EffectContext;
import cardistry.core.TemporaryEffect;
import cardistry.core.event.GameEvent;
import cardistry.core.event.GameHooks;
import cardistry.core.stats.StatModifier;
import cardistry.core.stats.StatType;
import cardistry.model.Element;
import cardistry.model.Enemy;
import cardistry.model.card.Card;
import cardistry.model.card.TerrainCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Registre de tous les objets et trinkets. Pour ajouter du contenu : une entrée ici, un test,
 * et une ligne dans GAME_DESIGN.md. Les noms sont <b>exactement</b> ceux du game design.
 * Chaque appel crée de nouvelles instances.
 */
public final class ItemRegistry {

    private ItemRegistry() {
    }

    public static List<Trinket> trinkets() {
        return List.of(
                // --- Communs ---
                Trinket.builder("redistribution-des-briquets", "Redistribution des briquets", COMMON)
                        .description("+25 % dégâts feu")
                        .tags(Tag.FIRE, Tag.PARTY)
                        .percent(StatType.FIRE_DAMAGE, 0.25)
                        .build(),
                Trinket.builder("pichet-de-sat", "Pichet de Sat", COMMON)
                        .description("+25 % dégâts eau")
                        .tags(Tag.WATER, Tag.EPFL)
                        .percent(StatType.WATER_DAMAGE, 0.25)
                        .build(),
                Trinket.builder("plante-du-rolex", "Plante du Rolex", COMMON)
                        .description("+25 % dégâts nature")
                        .tags(Tag.NATURE, Tag.EPFL)
                        .percent(StatType.NATURE_DAMAGE, 0.25)
                        .build(),
                Trinket.builder("polycopie-d-analyse-i", "Polycopié d'Analyse I", COMMON)
                        .description("−2 dégâts à chaque attaque subie")
                        .tags(Tag.DEFENSE, Tag.EPFL)
                        .flat(StatType.DAMAGE_REDUCTION, 2)
                        .build(),
                Trinket.builder("latte-macchiato-vanille", "Latte macchiato vanille", COMMON)
                        .description("+1 carte au premier tour de chaque combat")
                        .tags(Tag.DRAW, Tag.EPFL)
                        .hooks(new GameHooks() {
                            @Override
                            public void onTurnStart(GameEvent.TurnStarted event, EffectContext ctx) {
                                if (ctx.state().combatTurn() == 1) {
                                    ctx.drawCards(1);
                                }
                            }
                        })
                        .build(),

                // --- Peu communs ---
                Trinket.builder("pax", "Pax", UNCOMMON)
                        .description("+1 taille de main")
                        .tags(Tag.DRAW, Tag.PARTY)
                        .flat(StatType.HAND_SIZE, 1)
                        .build(),
                Trinket.builder("m1", "M1", UNCOMMON)
                        .description("Changer de terrain rend 5 PV")
                        .tags(Tag.TERRAIN, Tag.EPFL)
                        .hooks(new GameHooks() {
                            @Override
                            public void onTerrainChanged(GameEvent.TerrainChanged event, EffectContext ctx) {
                                ctx.healPlayer(5);
                            }
                        })
                        .build(),
                Trinket.builder("monde-snack", "Monde Snack", UNCOMMON)
                        .description("+10 PV à chaque adversaire vaincu")
                        .tags(Tag.HEAL, Tag.PARTY)
                        .hooks(new GameHooks() {
                            @Override
                            public void onEnemyKilled(GameEvent.EnemyKilled event, EffectContext ctx) {
                                ctx.healPlayer(10);
                            }
                        })
                        .build(),
                Trinket.builder("plan-du-campus", "Plan du campus", UNCOMMON)
                        .description("Jouer un terrain inflige aussi 5 dégâts")
                        .tags(Tag.TERRAIN, Tag.EPFL)
                        .hooks(new GameHooks() {
                            @Override
                            public void onCardPlayed(GameEvent.CardPlayed event, EffectContext ctx) {
                                if (event.card() instanceof TerrainCard) {
                                    ctx.damageEnemy(5);
                                }
                            }
                        })
                        .build(),
                Trinket.builder("grinder", "Grinder", UNCOMMON)
                        .description("Soins de type feu +50 % ; Petit pétou deux fois plus fréquent dans la pioche")
                        .tags(Tag.HEAL, Tag.PARTY)
                        .percent(StatType.FIRE_HEAL, 0.5)
                        .cardWeight("Petit pétou", 2)
                        .build(),

                // --- Rares ---
                Trinket.builder("photocopieuse", "Photocopieuse", RARE)
                        .description("Piocher donne 2 cartes")
                        .tags(Tag.DRAW, Tag.EPFL)
                        .flat(StatType.CARDS_PER_DRAW, 1)
                        .build(),
                Trinket.builder("comprehension-algebrique", "Compréhension algébrique", RARE)
                        .description("20 % des dégâts de sorts te soignent")
                        .tags(Tag.HEAL, Tag.NATURE)
                        .hooks(new GameHooks() {
                            @Override
                            public void onDamageDealt(GameEvent.DamageDealt event, EffectContext ctx) {
                                if (event.source() == GameEvent.DamageDealt.Source.SPELL && event.target() instanceof Enemy) {
                                    ctx.healPlayer(0.2 * event.amount());
                                }
                            }
                        })
                        .build(),
                // Provisoire (question ouverte) : +50 % sur les dégâts élémentaires (sorts, objets à élément).
                Trinket.builder("cms", "CMS", RARE)
                        .description("+50 % dégâts, mais l'adversaire aussi +25 %")
                        .tags(Tag.EPFL)
                        .percent(StatType.FIRE_DAMAGE, 0.5)
                        .percent(StatType.WATER_DAMAGE, 0.5)
                        .percent(StatType.NATURE_DAMAGE, 0.5)
                        .percent(StatType.ENEMY_DAMAGE, 0.25)
                        .build(),
                Trinket.builder("foie-d-ingenieur", "Foie d'ingénieur", RARE)
                        .description("Gueule de bois à 5 Ivresse au lieu de 3")
                        .tags(Tag.PARTY)
                        .flat(StatType.HANGOVER_THRESHOLD, 2)
                        .build(),

                // --- Légendaires ---
                Trinket.builder("petou-ancestral", "Pétou ancestral", LEGENDARY)
                        .description("+10 PV par tour, mais tu ne joues qu'un tour sur deux (l'adversaire attaque normalement)")
                        .tags(Tag.PARTY, Tag.FIRE)
                        .effect(() -> TemporaryEffect.builder("petou-ancestral", "Pétou ancestral")
                                .summary("un tour sur deux, +10 PV par tour")
                                .keepAcrossCombats()
                                .skips(TemporaryEffect.SkipRule.EVEN_COMBAT_TURNS)
                                .onTurnStart(ctx -> ctx.healPlayer(10))
                                .build())
                        .build(),
                // Comme CMS (provisoire) : s'applique aux dégâts élémentaires.
                Trinket.builder("ptit-sat", "Ptit Sat ?", LEGENDARY)
                        .description("+10 % dégâts par point d'Ivresse")
                        .tags(Tag.PARTY)
                        .hooks(new GameHooks() {
                            @Override
                            public void onDrunkennessChanged(GameEvent.DrunkennessChanged event, EffectContext ctx) {
                                String source = Trinket.sourceOf("ptit-sat");
                                ctx.state().playerStats().removeBySource(source);
                                double bonus = 0.1 * event.drunkenness();
                                if (bonus > 0) {
                                    for (Element element : Element.values()) {
                                        ctx.state().playerStats().add(
                                                StatModifier.percent(StatType.damageMultiplier(element), bonus, source));
                                    }
                                }
                            }
                        })
                        .build(),
                Trinket.builder("george", "George", LEGENDARY)
                        .description("À chaque carte piochée, une autre carte de ta main (tirée au hasard) est"
                                + " remplacée par une carte aléatoire du pool")
                        .flavor("À l'origine de la redistribution des biens communs.")
                        .tags(Tag.PARTY)
                        .hooks(new GameHooks() {
                            @Override
                            public void onCardDrawn(GameEvent.CardDrawn event, EffectContext ctx) {
                                List<Card> hand = ctx.state().hand();
                                List<Integer> others = new ArrayList<>();
                                for (int i = 0; i < hand.size(); i++) {
                                    if (hand.get(i) != event.card()) {
                                        others.add(i);
                                    }
                                }
                                if (!others.isEmpty()) {
                                    ctx.replaceCardInHand(ctx.rng().pick(others));
                                }
                            }
                        })
                        .build());
    }

    public static List<Item> items() {
        return List.of(
                new Item("mate", "Maté", "+10 PV", "", COMMON, Set.of(Tag.HEAL),
                        ctx -> ctx.healPlayer(10)),
                new Item("pichet-vide", "Pichet vide", "15 dégâts à l'adversaire, tu perds 5 PV, +1 Ivresse", "",
                        COMMON, Set.of(Tag.PARTY, Tag.DRINK),
                        ctx -> {
                            ctx.damageEnemy(15);
                            ctx.damagePlayer(5);
                        }),
                new Item("caipirinha", "Caïpirinha", "+20 PV, +1 Ivresse", "", COMMON, Set.of(Tag.PARTY, Tag.DRINK),
                        ctx -> ctx.healPlayer(20)),
                new Item("petit-petou", "Petit pétou", "+15 PV (soin feu), −1 Ivresse", "", COMMON, Set.of(Tag.PARTY),
                        ctx -> {
                            ctx.elementalHeal(15, Element.FIRE);
                            ctx.addDrunkenness(-1);
                        }),
                new Item("spritz", "Spritz", "+30 PV, +1 Ivresse", "", UNCOMMON, Set.of(Tag.PARTY, Tag.DRINK),
                        ctx -> ctx.healPlayer(30)),
                new Item("shot-de-jager", "Shot de Jäger", "Prochain sort ×2, +1 Ivresse", "", UNCOMMON,
                        Set.of(Tag.PARTY, Tag.DRINK),
                        ctx -> ctx.addEffect(TemporaryEffect.builder("shot-de-jager", "Shot de Jäger")
                                .summary("prochain sort ×2")
                                .charges(1)
                                .multipliesNextSpell(2)
                                .build())),
                new Item("moscow-mule", "Moscow Mule", "Annule la prochaine attaque, +1 Ivresse", "", UNCOMMON,
                        Set.of(Tag.PARTY, Tag.DRINK),
                        ctx -> ctx.addEffect(TemporaryEffect.builder("moscow-mule", "Moscow Mule")
                                .summary("annule la prochaine attaque")
                                .charges(1)
                                .cancelsNextEnemyAttack()
                                .build())),
                new Item("raclette", "Raclette", "+50 PV, mais tu sautes ton prochain tour", "", UNCOMMON,
                        Set.of(Tag.EPFL),
                        ctx -> {
                            ctx.healPlayer(50);
                            ctx.addEffect(TemporaryEffect.builder("raclette", "Raclette")
                                    .summary("tu sautes ton prochain tour")
                                    .charges(1)
                                    .skips(TemporaryEffect.SkipRule.NEXT_TURN)
                                    .build());
                        }),
                // +50 % dégâts : même portée que CMS (dégâts élémentaires).
                new Item("space-cake", "Space cake", "Au bout de 3 tours : +40 PV et +50 % dégâts pendant 2 tours",
                        "", RARE, Set.of(Tag.PARTY),
                        ctx -> ctx.addEffect(TemporaryEffect.builder("space-cake", "Space cake")
                                .summary("+50 % dégâts")
                                .delay(3)
                                .lasts(2)
                                .keepAcrossCombats()
                                .percent(StatType.FIRE_DAMAGE, 0.5)
                                .percent(StatType.WATER_DAMAGE, 0.5)
                                .percent(StatType.NATURE_DAMAGE, 0.5)
                                .onActivate(c -> c.healPlayer(40))
                                .build())),
                new Item("cafffeeeeinnnn", "Cafffeeeeinnnn", "Pioche 2 cartes", "", COMMON, Set.of(Tag.EPFL),
                        ctx -> ctx.drawCards(2)),
                new Item("gaz-du-bat-inf", "Gaz du bat Inf", "20 dégâts d'un élément aléatoire", "", COMMON,
                        Set.of(Tag.EPFL),
                        ctx -> ctx.elementalDamage(20, ctx.rng().pick(List.of(Element.values())))));
    }

    /** Tous les objets et trinkets. */
    public static List<Collectible> all() {
        return Stream.concat(trinkets().stream(), items().stream())
                .map(Collectible.class::cast)
                .toList();
    }

    /** Nouvelle instance du trinket {@code id}. */
    public static Trinket trinket(String id) {
        return trinkets().stream().filter(t -> t.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Trinket inconnu : " + id));
    }

    /** Nouvelle instance de l'objet {@code id}. */
    public static Item item(String id) {
        return items().stream().filter(i -> i.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Objet inconnu : " + id));
    }
}
