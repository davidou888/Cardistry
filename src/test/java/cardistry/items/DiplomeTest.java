package cardistry.items;

import static cardistry.model.Element.FIRE;
import static cardistry.model.Element.NATURE;
import static cardistry.model.Element.WATER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.Action;
import cardistry.core.CombatEngine;
import cardistry.core.GameState;
import cardistry.core.RewardChoice;
import cardistry.core.Rng;
import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiplomeTest {

    private static final double DELTA = 1e-9;
    private static int serial = 0;

    private final List<GameEvent> events = new ArrayList<>();
    private GameState state;
    private CombatEngine engine;
    private Enemy first;

    private void setUp(List<Card> hand, List<Collectible> loot) {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        first = new Enemy("A", FIRE, 100, 10);
        state = new GameState(new Player(100), List.of(first, new Enemy("B", FIRE, 100, 10)),
                new TerrainCard("Forêt", NATURE, cardistry.items.Rarity.COMMON), hand, new Stats());
        engine = new CombatEngine(state, new CardPool(), new Rng(1), bus, new LootTable(() -> loot));
    }

    private void setUp(List<Card> hand) {
        setUp(hand, List.of());
    }

    private void equip(String id) {
        engine.equip(ItemRegistry.trinket(id));
    }

    private static Trinket tagged(Tag... tags) {
        return Trinket.builder("d" + (++serial), "D" + serial, Rarity.COMMON).tags(tags).build();
    }

    private double stat(StatType stat) {
        return state.playerStats().get(stat);
    }

    @Test
    void unEmplacementDeTrinketEnPlus() {
        setUp(List.of());
        equip("diplome-epfl");
        assertEquals(6, state.trinketSlots());
    }

    @Test
    void doubleLesTrinketsEpflPasLesAutres() {
        setUp(List.of());
        equip("diplome-epfl");
        equip("redistribution-des-briquets"); // Feu, Soirée : pas EPFL
        equip("polycopie-d-analyse-i");        // EPFL : −2 → −4
        // Briquets non doublés (+25 %) ; Diplôme + Polycopié = set EPFL 2, doublé (+20 %)
        assertEquals(1.45, stat(StatType.FIRE_DAMAGE), DELTA);
        assertEquals(4, stat(StatType.DAMAGE_REDUCTION), DELTA);
    }

    @Test
    void lOrdreDEquipementNeComptePas() {
        setUp(List.of());
        equip("pichet-de-sat");
        equip("diplome-epfl");
        // Pichet de Sat +25 % ×2 = +50 % ; set EPFL 2 (Diplôme compte) +10 % ×2 = +20 %
        assertEquals(1.70, stat(StatType.WATER_DAMAGE), DELTA);
    }

    @Test
    void leDiplomeCompteDansLeSetEtDoubleLeSet() {
        setUp(List.of());
        equip("diplome-epfl");
        for (int i = 0; i < 3; i++) {
            engine.equip(tagged(Tag.EPFL));
        }
        assertEquals(4, state.tagCount(Tag.EPFL));
        // Palier 2 : +20 %, palier 4 : +30 % (doublés) ; palier 3 : +2 taille de main ; palier 4 : +50 PV max
        assertEquals(1.50, stat(StatType.NATURE_DAMAGE), DELTA);
        assertEquals(9, state.handSize());
        assertEquals(150, state.maxHp(), DELTA);
    }

    @Test
    void hooksDoublesM1PlanDuCampusLatte() {
        setUp(List.of(new TerrainCard("Marécage", WATER, cardistry.items.Rarity.COMMON), new TerrainCard("EPFL", FIRE, cardistry.items.Rarity.COMMON)));
        state.player().takeDamage(50);
        equip("diplome-epfl");
        equip("m1");
        equip("plan-du-campus");
        engine.submit(new Action.PlayCard(0));
        assertTrue(events.contains(new GameEvent.Healed(state.player(), 10, 1.0)), "M1 : 5 × 2");
        assertEquals(90, first.hp(), DELTA);                                    // Plan du campus : 5 × 2
    }

    @Test
    void latteDeuxCartesAuPremierTour() {
        setUp(List.of());
        equip("diplome-epfl");
        equip("latte-macchiato-vanille");
        engine.startTurn();
        assertEquals(2, state.hand().size());
    }

    @Test
    void photocopieuseTroisCartes() {
        setUp(List.of());
        equip("diplome-epfl");
        equip("photocopieuse");
        engine.submit(new Action.DrawCard());
        assertEquals(3, state.hand().size());
    }

    @Test
    void cmsContrepartieDoubleeAussi() {
        setUp(List.of(new SpellCard("Sort", FIRE, 20, cardistry.items.Rarity.COMMON)));
        equip("diplome-epfl");
        equip("cms");
        engine.submit(new Action.PlayCard(0));
        // CMS doublé : +100 % ; set EPFL 2 doublé : +20 % → ×2.2 ; adversaire +50 %
        assertEquals(100 - 44, first.hp(), DELTA);
        assertEquals(100 - 15, state.player().hp(), DELTA);
    }

    /** Combat avec un sort fatal, le Diplôme + 5 trinkets, et une récompense trinket. */
    private Trinket setUpFullWithDiplome(java.util.function.IntSupplier discardChoice, List<Integer> asked) {
        Trinket incoming = tagged(Tag.META);
        setUp(List.of(new SpellCard("Fatal", WATER, 1000, cardistry.items.Rarity.COMMON)), List.of(incoming));
        engine.setDecisions(new cardistry.core.PlayerDecisions() {
            @Override
            public int chooseDrawnCard(GameState s, List<Card> options) {
                return 0;
            }

            @Override
            public int chooseTrinketToDiscard(GameState s) {
                asked.add(s.trinkets().size());
                return discardChoice.getAsInt();
            }
        });
        equip("diplome-epfl");
        equip("pichet-de-sat");
        for (int i = 0; i < 4; i++) {
            engine.equip(tagged(Tag.META));
        }
        assertEquals(6, state.trinkets().size());
        engine.submit(new Action.PlayCard(0));
        return incoming;
    }

    @Test
    void perdreLeDiplomeAvecSixTrinketsObligeAEnJeterUn() {
        List<Integer> asked = new ArrayList<>();
        Trinket incoming = setUpFullWithDiplome(() -> 0, asked); // jette Pichet de Sat (1er après le Diplôme)

        engine.resolveReward(new RewardChoice.Take(0, 0)); // le Diplôme est remplacé

        assertEquals(List.of(6), asked, "une seule question, avec 6 trinkets pour 5 emplacements");
        assertEquals(5, state.trinketSlots());
        assertEquals(5, state.trinkets().size());
        assertTrue(state.trinkets().contains(incoming));
        assertTrue(events.stream().anyMatch(e -> e instanceof GameEvent.TrinketDiscarded d
                && d.trinket().id().equals("pichet-de-sat")));
        assertEquals(1.0, stat(StatType.WATER_DAMAGE), DELTA); // plus de Pichet, plus de set
        // Le combat suivant ne commence qu'après la défausse.
        int discarded = indexOf(GameEvent.TrinketDiscarded.class);
        assertTrue(discarded < indexOf(GameEvent.CombatStarted.class));
    }

    @Test
    void reponseInvalideRedemandee() {
        List<Integer> asked = new ArrayList<>();
        int[] answers = {9, -1, 2};
        int[] next = {0};
        setUpFullWithDiplome(() -> answers[next[0]++], asked);
        engine.resolveReward(new RewardChoice.Take(0, 0));
        assertEquals(3, asked.size());
        assertEquals(5, state.trinkets().size());
    }

    @Test
    void pasDeDefausseSansDebordement() {
        List<Integer> asked = new ArrayList<>();
        setUpFullWithDiplome(() -> 0, asked);
        engine.resolveReward(new RewardChoice.Take(0, 3)); // remplace un trinket quelconque, pas le Diplôme
        assertTrue(asked.isEmpty());
        assertEquals(6, state.trinkets().size());
    }

    private int indexOf(Class<? extends GameEvent> type) {
        for (int i = 0; i < events.size(); i++) {
            if (type.isInstance(events.get(i))) {
                return i;
            }
        }
        return -1;
    }
}
