package cardistry.items;

import java.util.Set;

/** Ce que le joueur peut obtenir et garder dans son inventaire : objet ou trinket. */
public sealed interface Collectible permits Item, Trinket {

    /** Identifiant stable (sans accents), utilisé comme clé. */
    String id();

    /** Nom affiché, exactement comme dans GAME_DESIGN.md. */
    String name();

    /** Effet, en une phrase. */
    String description();

    /** Texte d'ambiance optionnel (vide si aucun). */
    String flavor();

    Rarity rarity();

    Set<Tag> tags();

    /** « Objet » ou « Trinket ». */
    String kindName();

    /**
     * Identifiant qualifié, unique parmi les cartes, objets et trinkets :
     * « objet:petit-petou », « trinket:pax » (une carte : « carte:petit-petou »).
     */
    default String qualifiedId() {
        return (this instanceof Item ? "objet:" : "trinket:") + id();
    }
}
