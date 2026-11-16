package cardistry.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.GameState;
import cardistry.core.stats.StatModifier;
import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.items.Rarity;
import cardistry.model.Element;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.HealCard;
import cardistry.model.card.TerrainCard;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Fiche détaillée d'une carte (touche V). */
class CardDetailTest {

    private static GameState combat(Element enemyElement, Stats stats) {
        return new GameState(new Player(100), List.of(new Enemy("Mannequin", enemyElement, 50, 5)),
                new TerrainCard("Forêt", Element.NATURE, Rarity.COMMON), List.of(), stats);
    }

    private static String text(GameState state, Card card, boolean inCombat) {
        return String.join("\n", CardDetail.lines(state, card, inCombat).stream().map(CardDetail.Line::text).toList());
    }

    @Test
    void sortEnCombatAvecChaqueMultiplicateur() {
        Stats stats = new Stats();
        stats.add(StatModifier.percent(StatType.FIRE_DAMAGE, 0.25, "test"));
        String text = text(combat(Element.NATURE, stats), CardPool.bouleDeFeu(), true);
        assertTrue(text.startsWith("Boule de feu\nSort feu · Commun\nValeur de base : 10 dégâts\n★ 10 % d'infliger Brûlure"), text);
        assertTrue(text.contains("Contre Mannequin (plante) : ×1.5 (feu bat plante)"), text);
        assertTrue(text.contains("Tes bonus de dégâts feu : ×1.25"), text);
        assertTrue(text.contains("Total : 10 ×1.875 = 19 dégâts à Mannequin"), text);
        assertTrue(text.contains("Bat : plante · Battu par : eau, error: null"), text);
    }

    @Test
    void sortAvecForDefEtCritique() {
        Stats stats = new Stats();
        stats.add(StatModifier.flat(StatType.STRENGTH, 3, "test"));
        stats.add(StatModifier.flat(StatType.LUCK, 1, "test"));
        Enemy armored = cardistry.model.Enemy.builder("m", "Mannequin", Element.NATURE,
                cardistry.model.enemy.EnemyRank.COMMON).hp(50).stats(0, 0, 0, 2, 0, 0)
                .behavior(cardistry.model.enemy.EnemyBehavior.always(cardistry.model.enemy.EnemyMove.basicAttack(5)))
                .build();
        GameState state = new GameState(new Player(100), List.of(armored),
                new TerrainCard("Forêt", Element.NATURE, Rarity.COMMON), List.of(), stats);
        String text = text(state, CardPool.bouleDeFeu(), true);
        assertTrue(text.contains("+ FOR : +3 dégâts (avant les multiplicateurs)"), text);
        assertTrue(text.contains("− DEF de Mannequin : −2 dégâts"), text);
        assertTrue(text.contains("Coup critique : 3 % de chance, ×1.5"), text);
        assertTrue(text.contains("Total : (10 + 3) ×1.5 − 2 = 18 dégâts à Mannequin"), text);
        String tsunami = text(state, CardPool.tsunami(), true);
        assertTrue(tsunami.contains("− DEF de Mannequin : 2, ignorée par ce sort"), tsunami);
        assertTrue(text(state, CardPool.cayouuu(), true).contains("− DEF de Mannequin : 2, ignorée par ce sort"));
        stats.add(StatModifier.flat(StatType.LUCK, 2, "test"));
        assertTrue(text(state, CardPool.lancerDeManette(), true).contains("Coup critique garanti (CHA ≥ 3)"));
        assertTrue(text(state, CardPool.carteBasiquePasOuf(), true).contains("Ta FOR est déjà comptée"));
    }

    @Test
    void auShopSeulementLesValeursDeBase() {
        String text = text(combat(Element.NATURE, new Stats()), CardPool.bouleDeFeu(), false);
        assertTrue(!text.contains("Si tu la joues") && !text.contains("Mannequin"), text);
    }

    @Test
    void soinNonMultiplieEtElementsParticuliers() {
        GameState state = combat(Element.WATER, new Stats());
        state.player().takeDamage(50);
        String heal = text(state, new HealCard("Potion", Element.NATURE, 10, Rarity.COMMON), true);
        assertTrue(heal.contains("Terrain Forêt : ×1.5") && heal.contains("Total : 10 ×1.5 → +15 PV"), heal);
        String prof = text(state, CardPool.professeurDOr(), true);
        assertTrue(prof.contains("Dégâts non multipliés : 35 à Mannequin"), prof);
        String pet = text(state, CardPool.petDeMamouz(), true);
        assertTrue(pet.contains("Aucun dégât à Mannequin : son pouvoir agit") && !pet.contains("Total"), pet);
        assertEquals("Bat tout", CardDetail.matchups(Element.GLITCH));
        assertEquals("Bat : rien · Battu par : error: null", CardDetail.matchups(Element.NEUTRAL));
        String terrain = text(state, CardPool.dojo(), true);
        assertTrue(terrain.contains("+1 FOR") && terrain.contains("Attaque de Mannequin"), terrain);
    }
}
