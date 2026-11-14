package cardistry.items;

import static cardistry.model.Element.FIRE;
import static cardistry.model.Element.NATURE;
import static cardistry.model.Element.WATER;
import static org.junit.jupiter.api.Assertions.assertEquals;

import cardistry.core.Action;
import cardistry.core.CombatEngine;
import cardistry.core.GameState;
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
import org.junit.jupiter.api.Test;

/** Un test par effet chiffré des trinkets du registre. */
class TrinketContentTest {

    private static final double DELTA = 1e-9;

    final List<GameEvent> events = new ArrayList<>();
    GameState state;
    CombatEngine engine;
    Enemy enemy;

    /**
     * Combat contre un adversaire de 100 PV / 10 d'attaque, sur un terrain neutre pour lui
     * (attaque ×1), avec la main donnée.
     */
    void setUp(cardistry.model.Element enemyElement, List<Card> hand, CardPool pool) {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        enemy = new Enemy("Cible", enemyElement, 100, 10);
        TerrainCard neutral = switch (enemyElement) {
            case FIRE -> new TerrainCard("Forêt", NATURE);
            case WATER -> new TerrainCard("EPFL", FIRE);
            case NATURE -> new TerrainCard("Marécage", WATER);
        };
        state = new GameState(new Player(100), List.of(enemy, new Enemy("Suivant", FIRE, 100, 10)), neutral, hand,
                new Stats());
        engine = new CombatEngine(state, pool, new Rng(2), bus, new LootTable(List::of));
    }

    void setUp(cardistry.model.Element enemyElement, List<Card> hand) {
        setUp(enemyElement, hand, new CardPool());
    }

    void equip(String id) {
        engine.equip(ItemRegistry.trinket(id));
    }

    double lastDamageToEnemy() {
        for (int i = events.size() - 1; i >= 0; i--) {
            if (events.get(i) instanceof GameEvent.DamageDealt d && d.target() == enemy) {
                return d.amount();
            }
        }
        throw new AssertionError("Aucun dégât à l'adversaire");
    }

    private void spellBoost(String id, cardistry.model.Element element, double expectedDamage) {
        setUp(element, List.of(new SpellCard("Sort", element, 20)));
        equip(id);
        engine.submit(new Action.PlayCard(0));
        assertEquals(expectedDamage, lastDamageToEnemy(), DELTA);
    }

    @Test
    void redistributionDesBriquets() {
        spellBoost("redistribution-des-briquets", FIRE, 25);
    }

    @Test
    void pichetDeSat() {
        spellBoost("pichet-de-sat", WATER, 25);
    }

    @Test
    void planteDuRolex() {
        spellBoost("plante-du-rolex", NATURE, 25);
    }

    @Test
    void lesBonusDElementNeSAppliquentQuASonElement() {
        setUp(WATER, List.of(new SpellCard("Sort", WATER, 20)));
        equip("redistribution-des-briquets");
        engine.submit(new Action.PlayCard(0));
        assertEquals(20, lastDamageToEnemy(), DELTA);
    }

    @Test
    void polycopieDAnalyseI() {
        setUp(FIRE, List.of());
        equip("polycopie-d-analyse-i");
        engine.submit(new Action.DrawCard());
        assertEquals(100 - (10 - 2), state.player().hp(), DELTA);
    }

    @Test
    void pax() {
        setUp(FIRE, List.of());
        equip("pax");
        assertEquals(8, state.handSize());
    }

    @Test
    void grinderSoinsFeu() {
        setUp(FIRE, List.of(new HealCard("Petit pétou", FIRE, 10)));
        state.player().takeDamage(50);
        equip("grinder");
        engine.submit(new Action.PlayCard(0));
        // Terrain Forêt : le feu bat la nature → soin feu ×1 ; Grinder +50 %.
        assertEquals(new GameEvent.Healed(state.player(), 15, 1.5), events.get(1));
    }

    @Test
    void grinderPetitPetouDeuxFoisPlusFrequent() {
        setUp(FIRE, List.of());
        Card pétou = new HealCard("Petit pétou", FIRE, 15);
        Card other = new SpellCard("Boule de feu", FIRE, 10);
        equip("grinder");
        assertEquals(2.0, state.cardWeight(pétou), DELTA);
        assertEquals(1.0, state.cardWeight(other), DELTA);
    }

    @Test
    void photocopieuse() {
        setUp(FIRE, List.of());
        equip("photocopieuse");
        engine.submit(new Action.DrawCard());
        assertEquals(2, state.hand().size());
    }

    @Test
    void cms() {
        setUp(FIRE, List.of(new SpellCard("Sort", FIRE, 20)));
        equip("cms");
        engine.submit(new Action.PlayCard(0));
        assertEquals(30, lastDamageToEnemy(), DELTA);          // +50 %
        assertEquals(100 - 12.5, state.player().hp(), DELTA);  // adversaire +25 %
    }

    // --- Trinkets à hooks ---

    @Test
    void latteMacchiatoVanillePremierTourDeChaqueCombat() {
        setUp(FIRE, List.of());
        equip("latte-macchiato-vanille");
        engine.start();
        assertEquals(GameState.STARTING_HAND, state.hand().size());

        engine.startTurn();
        assertEquals(GameState.STARTING_HAND + 1, state.hand().size());

        engine.submit(new Action.PlayCard(0));
        engine.startTurn();
        assertEquals(GameState.STARTING_HAND, state.hand().size()); // 6 − 1 jouée, pas de bonus au tour 2
    }

    @Test
    void m1ChangerDeTerrainRendCinqPv() {
        setUp(FIRE, List.of(new TerrainCard("Marécage", WATER)));
        state.player().takeDamage(30);
        equip("m1");
        engine.submit(new Action.PlayCard(0));
        assertEquals(true, events.contains(new GameEvent.Healed(state.player(), 5, 1.0)));
        // Marécage (eau) bat l'adversaire feu : attaque ×0.75.
        assertEquals(70 + 5 - 7.5, state.player().hp(), DELTA);
    }

    @Test
    void mondeSnackSoigneAChaqueAdversaireVaincu() {
        setUp(FIRE, List.of(new SpellCard("Coup fatal", WATER, 100)));
        state.player().takeDamage(30);
        equip("monde-snack");
        engine.submit(new Action.PlayCard(0));
        assertEquals(80, state.player().hp(), DELTA);
    }

    @Test
    void planDuCampusUnTerrainInfligeCinqDegats() {
        setUp(FIRE, List.of(new TerrainCard("Marécage", WATER), new SpellCard("Sort", FIRE, 20)));
        equip("plan-du-campus");
        engine.submit(new Action.PlayCard(0));
        assertEquals(95, enemy.hp(), DELTA);

        engine.submit(new Action.PlayCard(0)); // un sort ne déclenche pas Plan du campus
        assertEquals(75, enemy.hp(), DELTA);
    }

    @Test
    void comprehensionAlgebriqueVingtPourcentDesSorts() {
        setUp(FIRE, List.of(new SpellCard("Sort", FIRE, 20), new TerrainCard("Marécage", WATER)));
        state.player().takeDamage(50);
        equip("comprehension-algebrique");
        equip("plan-du-campus");

        engine.submit(new Action.PlayCard(0));
        assertEquals(new GameEvent.Healed(state.player(), 4, 1.0), events.stream()
                .filter(GameEvent.Healed.class::isInstance).findFirst().orElseThrow());

        // Les dégâts d'effets (Plan du campus) ne soignent pas.
        events.clear();
        engine.submit(new Action.PlayCard(0));
        assertEquals(0, events.stream().filter(GameEvent.Healed.class::isInstance).count());
    }

    @Test
    void georgeRemplaceUneAutreCarteAChaquePioche() {
        Card keep = new TerrainCard("Gardée", FIRE);
        setUp(FIRE, List.of(keep), new CardPool(List.of(new SpellCard("Neuve", WATER, 10))));
        equip("george");

        engine.submit(new Action.DrawCard());

        // La carte piochée reste ; l'autre (la seule) est remplacée.
        assertEquals(List.of("Neuve", "Neuve"), state.hand().stream().map(Card::name).toList());
        GameEvent.CardReplaced replaced = (GameEvent.CardReplaced) events.stream()
                .filter(GameEvent.CardReplaced.class::isInstance).findFirst().orElseThrow();
        assertEquals(keep, replaced.removed());
    }

    @Test
    void georgeSansAutreCarteNeFaitRien() {
        setUp(FIRE, List.of());
        equip("george");
        engine.submit(new Action.DrawCard());
        assertEquals(1, state.hand().size());
        assertEquals(0, events.stream().filter(GameEvent.CardReplaced.class::isInstance).count());
    }

    @Test
    void georgeEtPhotocopieuseDeuxRemplacements() {
        setUp(FIRE, List.of(new TerrainCard("A", FIRE), new TerrainCard("B", FIRE)));
        equip("george");
        equip("photocopieuse");
        engine.submit(new Action.DrawCard());
        assertEquals(2, events.stream().filter(GameEvent.CardReplaced.class::isInstance).count());
        assertEquals(4, state.hand().size());
    }

    // --- Ivresse ---

    private void drink(int times) {
        for (int i = 0; i < times; i++) {
            engine.give(ItemRegistry.item("spritz"));
            engine.submit(new Action.UseItem(0));
        }
    }

    @Test
    void foieDIngenieurSeuilACinq() {
        setUp(FIRE, List.of());
        equip("foie-d-ingenieur");
        drink(4);
        assertEquals(true, state.pendingCurses().isEmpty());
        drink(1);
        assertEquals(java.util.Set.of(cardistry.core.Curse.HANGOVER), state.pendingCurses());
    }

    @Test
    void ptitSatDixPourcentParPointDIvresse() {
        setUp(FIRE, List.of(new SpellCard("Sort", FIRE, 20)));
        equip("ptit-sat");
        drink(2);
        engine.submit(new Action.PlayCard(0));
        assertEquals(20 * 1.2, lastDamageToEnemy(), DELTA);
    }

    @Test
    void ptitSatRetombeAvecLIvresseEtSAdditionneALaCms() {
        setUp(FIRE, List.of(new SpellCard("Coup fatal", WATER, 100), new SpellCard("Sort", FIRE, 20)));
        equip("ptit-sat");
        equip("cms");
        drink(3);
        assertEquals(1 + 0.5 + 0.3, state.playerStats().get(cardistry.core.stats.StatType.FIRE_DAMAGE), DELTA);

        engine.submit(new Action.PlayCard(0)); // tue l'adversaire : fin du combat, Ivresse à 0
        assertEquals(1.5, state.playerStats().get(cardistry.core.stats.StatType.FIRE_DAMAGE), DELTA);
    }

    @Test
    void ptitSatDesequipeRetireToutSesBonus() {
        setUp(FIRE, List.of());
        equip("ptit-sat");
        drink(2);
        assertEquals(1.2, state.playerStats().get(cardistry.core.stats.StatType.WATER_DAMAGE), DELTA);
        Trinket ptitSat = state.trinkets().get(0);
        state.playerStats().removeBySource(ptitSat.modifierSource());
        state.playerStats().removeBySource(ptitSat.dynamicSource());
        assertEquals(1.0, state.playerStats().get(cardistry.core.stats.StatType.WATER_DAMAGE), DELTA);
    }

    @Test
    void cmsEtBriquetsSAdditionnent() {
        setUp(FIRE, List.of(new SpellCard("Sort", FIRE, 20)));
        equip("cms");
        equip("redistribution-des-briquets");
        engine.submit(new Action.PlayCard(0));
        assertEquals(20 * 1.75, lastDamageToEnemy(), DELTA);
    }
}
