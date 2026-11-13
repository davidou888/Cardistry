package cardistry.items;

import static cardistry.items.Rarity.COMMON;
import static cardistry.items.Rarity.RARE;
import static cardistry.items.Rarity.UNCOMMON;

import cardistry.core.stats.StatType;
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

                // --- Peu communs ---
                Trinket.builder("pax", "Pax", UNCOMMON)
                        .description("+1 taille de main")
                        .tags(Tag.DRAW, Tag.PARTY)
                        .flat(StatType.HAND_SIZE, 1)
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
                // Provisoire (question ouverte) : +50 % sur les dégâts élémentaires (sorts, objets à élément).
                Trinket.builder("cms", "CMS", RARE)
                        .description("+50 % dégâts, mais l'adversaire aussi +25 %")
                        .tags(Tag.EPFL)
                        .percent(StatType.FIRE_DAMAGE, 0.5)
                        .percent(StatType.WATER_DAMAGE, 0.5)
                        .percent(StatType.NATURE_DAMAGE, 0.5)
                        .percent(StatType.ENEMY_DAMAGE, 0.25)
                        .build());
    }

    public static List<Item> items() {
        return List.of(
                new Item("mate", "Maté", "+10 PV", "", COMMON, Set.of(Tag.HEAL),
                        ctx -> ctx.healPlayer(10)));
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
