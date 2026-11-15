package cardistry.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.event.EventBus;
import cardistry.core.stats.StatModifier;
import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.core.status.StatusTarget;
import cardistry.core.status.StatusType;
import cardistry.items.LootTable;
import cardistry.items.Rarity;
import cardistry.model.Element;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.TerrainCard;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** Cartes modifiées par le plan V2 (§7). */
class ModifiedCardsTest {

    private GameState state;
    private CombatEngine engine;
    private Enemy enemy;

    private void setUp(Card card, long seed, boolean claude) {
        Stats stats = new Stats();
        if (claude) {
            stats.add(StatModifier.flat(StatType.LUCKY_ROLLS, 1, "claude"));
        }
        enemy = new Enemy("Cible", Element.NEUTRAL, 1000, 0);
        state = new GameState(new Player(100), List.of(enemy), new TerrainCard("Neutre", Element.NEUTRAL,
                Rarity.COMMON), List.of(card), stats);
        engine = new CombatEngine(state, new CardPool(), new Rng(seed), new EventBus(), new LootTable(List::of));
        engine.startTurn();
    }

    /** Sur 1000 parties, combien de fois {@code card} pose {@code status} sur {@code target}. */
    private int count(Supplier<Card> card, StatusTarget target, StatusType status, boolean claude) {
        int hits = 0;
        for (long seed = 0; seed < 1000; seed++) {
            setUp(card.get(), seed, claude);
            engine.submit(new Action.PlayCard(0));
            hits += state.statuses(target).has(status) ? 1 : 0;
        }
        return hits;
    }

    @Test
    void bouleDeFeuDixPourcentDeBrulure() {
        assertEquals(10, card(CardPool::bouleDeFeu).damage(), 1e-9);
        int burned = count(CardPool::bouleDeFeu, StatusTarget.ENEMY, StatusType.BURN, false);
        assertTrue(burned > 70 && burned < 130, "≈ 10 % : " + burned);
        int lucky = count(CardPool::bouleDeFeu, StatusTarget.ENEMY, StatusType.BURN, true);
        assertTrue(lucky > 155 && lucky < 225, "CLAUDE.md : ≈ 19 % : " + lucky);
    }

    @Test
    void fouetDeLianesDixPourcentDeBondage() {
        int bound = count(CardPool::fouetDeLianes, StatusTarget.ENEMY, StatusType.BONDAGE, false);
        assertTrue(bound > 70 && bound < 130, "≈ 10 % : " + bound);
    }

    @Test
    void petDeMamouzAucunDegatEtIntoxAcoupSur() {
        assertEquals(0, card(CardPool::petDeMamouz).damage(), 1e-9);
        setUp(CardPool.petDeMamouz(), 1, false);
        engine.submit(new Action.PlayCard(0));
        assertTrue(state.statuses(StatusTarget.ENEMY).has(StatusType.INTOX));
        assertEquals(1000 - 5, enemy.hp(), "seulement l'Intox, au début de son tour");
    }

    @Test
    void petiteTaffeSoinQuinzeEtQuinzePourcentDeFonsNonDouble() {
        setUp(CardPool.petiteTaffe(), 1, false);
        state.player().takeDamage(50);
        engine.submit(new Action.PlayCard(0));
        assertEquals(65, state.player().hp());
        int asleep = count(CardPool::petiteTaffe, StatusTarget.PLAYER, StatusType.FONS, false);
        assertTrue(asleep > 115 && asleep < 185, "≈ 15 % : " + asleep);
        int lucky = count(CardPool::petiteTaffe, StatusTarget.PLAYER, StatusType.FONS, true);
        assertTrue(lucky > 115 && lucky < 185, "pas doublé par CLAUDE.md : " + lucky);
    }

    @Test
    void petiteTaffePendantLaFonsCinqPourcentDesPvMaxParTour() {
        for (long seed = 0; seed < 200; seed++) {
            setUp(CardPool.petiteTaffe(), seed, false);
            state.player().takeDamage(50);
            engine.submit(new Action.PlayCard(0)); // 50 + 15
            if (state.statuses(StatusTarget.PLAYER).has(StatusType.FONS)) {
                engine.startTurn(); // tour sauté : +5 % de 100
                assertEquals(70, state.player().hp());
                return;
            }
        }
        throw new AssertionError("aucune Fons en 200 parties");
    }

    @Test
    void raretesDesTerrainsEtDeLaPetiteTaffe() {
        CardPool pool = new CardPool();
        assertEquals(Rarity.RARE, rarity(pool, "Désert kaléidoscopique"));
        assertEquals(Rarity.RARE, rarity(pool, "Espace de dimension n"));
        assertEquals(Rarity.RARE, rarity(pool, "Fractale"));
        assertEquals(Rarity.UNCOMMON, rarity(pool, "Enfer"));
        assertEquals(Rarity.UNCOMMON, rarity(pool, "Atlantis"));
        assertEquals(Rarity.COMMON, rarity(pool, "Petite taffe"));
        assertTrue(pool.cards().stream().noneMatch(c -> c.name().equals("Petit pétou")));
    }

    @Test
    void leDeckDeDepartALesPouvoirs() {
        for (String name : List.of("Boule de feu", "Fouet de lianes", "Pet de Mamouz")) {
            assertTrue(CardPool.startingDeck().stream().filter(c -> c.name().equals(name)).findFirst()
                    .orElseThrow().power().isPresent(), name);
        }
    }

    private static cardistry.model.card.SpellCard card(Supplier<cardistry.model.card.SpellCard> card) {
        return card.get();
    }

    private static Rarity rarity(CardPool pool, String name) {
        return pool.cards().stream().filter(c -> c.name().equals(name)).findFirst().orElseThrow().rarity();
    }
}
