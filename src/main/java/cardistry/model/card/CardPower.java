package cardistry.model.card;

import cardistry.core.EffectContext;
import java.util.function.Consumer;

/**
 * Pouvoir d'une carte : un texte affiché (sur la carte et dans sa description) et un effet,
 * déclenché <b>après</b> l'effet principal de la carte (dégâts, soin, terrain).
 */
public record CardPower(String text, Consumer<EffectContext> effect) {
}
