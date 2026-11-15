package cardistry.items;

import static cardistry.model.Element.FIRE;
import static cardistry.model.Element.NATURE;
import static cardistry.model.Element.WATER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.Action;
import cardistry.core.CombatEngine;
import cardistry.core.GameState;
import cardistry.core.PlayerDecisions;
import cardistry.core.Rng;
import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.stats.Stats;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.HealCard;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import org.junit.jupiter.api.Test;

class PaquetDeCartesTest {

    private final List<GameEvent> events = new ArrayList<>();
    private final List<List<Card>> offers = new ArrayList<>();
    private GameState state;
    private CombatEngine engine;

    /** Moteur avec la main donnée ; {@code choice} décide quelle carte garder parmi les propositions. */
    private void setUp(List<Card> hand, CardPool pool, long seed, ToIntFunction<List<Card>> choice) {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        state = new GameState(new Player(100), List.of(new Enemy("A", FIRE, 100, 10), new Enemy("B", FIRE, 100, 10)),
                new TerrainCard("Forêt", NATURE, cardistry.items.Rarity.COMMON), hand, new Stats());
        engine = new CombatEngine(state, pool, new Rng(seed), bus, new LootTable(List::of));
        engine.setDecisions(new PlayerDecisions() {
            @Override
            public int chooseDrawnCard(GameState s, List<Card> options) {
                offers.add(options);
                return choice.applyAsInt(options);
            }

            @Override
            public int chooseTrinketToDiscard(GameState s) {
                return 0;
            }
        });
    }

    private void setUp(ToIntFunction<List<Card>> choice) {
        setUp(List.of(), new CardPool(), 1, choice);
    }

    private void equip(String id) {
        engine.equip(ItemRegistry.trinket(id));
    }

    @Test
    void choixSimpleTroisCartesUneGardee() {
        setUp(options -> 2);
        equip("paquet-de-cartes");

        engine.submit(new Action.DrawCard());

        assertEquals(1, offers.size());
        assertEquals(3, offers.get(0).size());
        assertSame(offers.get(0).get(2), state.hand().get(0));
        GameEvent.CardChosen chosen = (GameEvent.CardChosen) events.stream()
                .filter(GameEvent.CardChosen.class::isInstance).findFirst().orElseThrow();
        assertEquals(List.of(offers.get(0).get(0), offers.get(0).get(1)), chosen.discarded());
        assertTrue(events.indexOf(chosen) < events.indexOf(new GameEvent.CardDrawn(state.hand().get(0))));
    }

    @Test
    void sansPaquetPasDeChoix() {
        setUp(options -> 0);
        engine.submit(new Action.DrawCard());
        assertTrue(offers.isEmpty());
        assertEquals(1, state.hand().size());
    }

    @Test
    void piocheMultipleUnChoixParCarte() {
        setUp(options -> 1);
        equip("paquet-de-cartes");
        equip("photocopieuse");

        engine.submit(new Action.DrawCard());

        assertEquals(2, offers.size());
        assertEquals(List.of(offers.get(0).get(1), offers.get(1).get(1)), state.hand());
    }

    @Test
    void lesEffetsQuiPiochentDonnentAussiUnChoix() {
        setUp(options -> 0);
        equip("paquet-de-cartes");
        engine.give(ItemRegistry.item("cafffeeeeinnnn"));
        engine.submit(new Action.UseItem(0));
        assertEquals(2, offers.size());

        equip("latte-macchiato-vanille"); // déclenche au premier tour du combat suivant
        state.currentEnemy().takeDamage(1000);
        engine.submit(new Action.DrawCard());   // fin du combat 1 (une pioche de plus : 3e choix)
        offers.clear();
        engine.startTurn();                     // combat 2, tour 1
        assertEquals(1, offers.size());
    }

    @Test
    void laMainDeDepartNEstPasConcernee() {
        EventBus bus = new EventBus();
        state = GameState.newGame();
        engine = new CombatEngine(state, new CardPool(), new Rng(3), bus, new LootTable(List::of));
        engine.setDecisions(new PlayerDecisions() {
            @Override
            public int chooseDrawnCard(GameState s, List<Card> options) {
                offers.add(options);
                return 0;
            }

            @Override
            public int chooseTrinketToDiscard(GameState s) {
                return 0;
            }
        });
        engine.equip(ItemRegistry.trinket("paquet-de-cartes"));
        engine.start();
        engine.startCombat(cardistry.model.enemy.EnemyRegistry.create(cardistry.model.enemy.EnemyRegistry.CADDIE, 1),
                false);
        assertEquals(GameState.STARTING_HAND, state.hand().size());
        assertTrue(offers.isEmpty());
    }

    @Test
    void georgeSeDeclencheApresLeChoixEtProtegeLaCarteChoisie() {
        Card keep = new TerrainCard("Gardée", FIRE, cardistry.items.Rarity.COMMON);
        setUp(List.of(keep), new CardPool(List.of(new SpellCard("Neuve", WATER, 10, cardistry.items.Rarity.COMMON))), 1, options -> 1);
        equip("paquet-de-cartes");
        equip("george");

        engine.submit(new Action.DrawCard());

        Card chosen = offers.get(0).get(1);
        assertSame(chosen, state.hand().get(1), "la carte choisie est protégée");
        assertNotSame(keep, state.hand().get(0), "l'autre carte est remplacée");
        int chosenAt = events.indexOf(events.stream().filter(GameEvent.CardChosen.class::isInstance).findFirst().orElseThrow());
        int replacedAt = events.indexOf(events.stream().filter(GameEvent.CardReplaced.class::isInstance).findFirst().orElseThrow());
        assertTrue(chosenAt < replacedAt);
    }

    @Test
    void grinderSAppliqueAuxPropositions() {
        Card common = new SpellCard("Boule", FIRE, 10, cardistry.items.Rarity.COMMON);
        Card petou = new HealCard("Petite taffe", FIRE, 15, cardistry.items.Rarity.COMMON);
        setUp(List.of(), new CardPool(List.of(common, common, common, petou)), 5, options -> 0);
        equip("paquet-de-cartes");
        equip("grinder");

        long petous = 0;
        long total = 0;
        for (int i = 0; i < 2000; i++) {
            engine.submit(new Action.DrawCard());
            engine.submit(new Action.PlayCard(0)); // garde la main presque vide
            state.player().heal(1000);              // seules les propositions comptent ici :
            state.currentEnemy().heal(1000);        // personne ne meurt
        }
        for (List<Card> offer : offers) {
            total += offer.size();
            petous += offer.stream().filter(c -> c.name().equals("Petite taffe")).count();
        }
        // Sans Grinder : 1/4 ; avec (poids 2) : 2/5.
        assertEquals(0.4, petous / (double) total, 0.02);
    }

    @Test
    void deterministeAvecLaSeed() {
        assertEquals(playWithSeed(42), playWithSeed(42));
    }

    private List<String> playWithSeed(long seed) {
        events.clear();
        offers.clear();
        setUp(List.of(), new CardPool(), seed, options -> options.size() - 1);
        equip("paquet-de-cartes");
        for (int i = 0; i < 5; i++) {
            engine.submit(new Action.DrawCard());
        }
        List<String> result = new ArrayList<>(state.hand().stream().map(Card::name).toList());
        offers.forEach(o -> result.add(o.stream().map(Card::name).toList().toString()));
        return result;
    }
}
