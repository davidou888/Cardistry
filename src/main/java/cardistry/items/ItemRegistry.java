package cardistry.items;

import static cardistry.items.Rarity.COMMON;

import cardistry.core.stats.StatType;
import java.util.List;
import java.util.Set;

/**
 * Registre de tous les objets et trinkets. Pour ajouter du contenu : une entrée ici, un test,
 * et une ligne dans GAME_DESIGN.md. Chaque appel crée de nouvelles instances.
 */
public final class ItemRegistry {

    private ItemRegistry() {
    }

    public static List<Trinket> trinkets() {
        return List.of(
                Trinket.builder("redistribution-des-briquets", "Redistribution des briquets", COMMON)
                        .description("+25 % dégâts feu")
                        .tags(Tag.FIRE, Tag.PARTY)
                        .percent(StatType.FIRE_DAMAGE, 0.25)
                        .build());
    }

    public static List<Item> items() {
        return List.of(
                new Item("mate", "Maté", "+10 PV", "", COMMON, Set.of(Tag.HEAL),
                        ctx -> ctx.healPlayer(10)));
    }

    /** Tous les objets et trinkets. */
    public static List<Collectible> all() {
        return java.util.stream.Stream.concat(trinkets().stream(), items().stream())
                .map(Collectible.class::cast)
                .toList();
    }
}
