package cardistry.model.card;

import cardistry.items.Rarity;
import cardistry.model.Element;

/** Une carte. Les sous-types sont fermés : sortilège, soin, terrain, et la carte morte (injouable). */
public abstract sealed class Card permits SpellCard, HealCard, TerrainCard, DeadCard {

    private final String name;
    private final Element element;
    private final Rarity rarity;
    /** {@code null} : aucun pouvoir. */
    private CardPower power;

    protected Card(String name, Element element, Rarity rarity) {
        this.name = name;
        this.element = element;
        this.rarity = rarity;
    }

    public String name() {
        return name;
    }

    /** Élément de la carte ; {@code null} pour la carte morte ({@link DeadCard}). */
    public Element element() {
        return element;
    }

    /** Identifiant qualifié, distinct de celui d'un objet ou trinket du même nom : « carte:petit-petou ». */
    public String qualifiedId() {
        return "carte:" + cardistry.model.Ids.slug(name);
    }

    /** Rareté de la carte. */
    public Rarity rarity() {
        return rarity;
    }

    /** Pouvoir de la carte (texte + effet après l'effet principal), s'il y en a un. */
    public java.util.Optional<CardPower> power() {
        return java.util.Optional.ofNullable(power);
    }

    /** Pose le pouvoir de {@code source} sur {@code copy} (à appeler dans chaque {@link #copy()}). */
    protected static <T extends Card> T samePower(Card source, T copy) {
        ((Card) copy).power = source.power;
        return copy;
    }

    /** Pose {@code power} sur {@code copy} (pour les {@code withPower} des sous-classes). */
    protected static <T extends Card> T withPower(T copy, CardPower power) {
        ((Card) copy).power = power;
        return copy;
    }

    /**
     * Nouvelle instance indépendante, pouvoir compris. Les cartes du registre sont des modèles : on ne
     * met en main que des copies, pour qu'une carte améliorée ou modifiée n'affecte pas les autres.
     */
    public abstract Card copy();
}
