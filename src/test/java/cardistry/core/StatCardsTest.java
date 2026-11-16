package cardistry.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
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
import cardistry.model.enemy.EnemyBehavior;
import cardistry.model.enemy.EnemyMove;
import cardistry.model.enemy.EnemyRank;
import cardistry.model.enemy.MoveEffect;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Cartes modifiées par le plan V3 (stats) : un test par effet chiffré. */
class StatCardsTest {

    private final List<GameEvent> events = new ArrayList<>();
    private GameState state;
    private CombatEngine engine;
    private Enemy enemy;

    /**
     * Adversaire neutre de 200 PV, sans attaque, avec {@code defense} DEF et {@code resistance} RES ;
     * terrain neutre sans effet ; main = {@code hand}.
     */
    private void setUp(int defense, int resistance, long seed, Card... hand) {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        enemy = Enemy.builder("cible", "Cible", Element.NEUTRAL, EnemyRank.COMMON).hp(200)
                .stats(0, 0, 0, defense, resistance, 0)
                .behavior(EnemyBehavior.always(EnemyMove.of("rien", "Rien", "Rien", MoveEffect.Hit.of(0))))
                .build();
        state = new GameState(new Player(100), List.of(enemy, new Enemy("Suivant", Element.NEUTRAL, 10, 0)),
                new TerrainCard("Neutre", Element.NEUTRAL, Rarity.COMMON), new ArrayList<>(List.of(hand)), new Stats());
        engine = new CombatEngine(state, new CardPool(), new Rng(seed), bus, new LootTable(List::of));
        engine.startTurn();
    }

    private void setUp(int defense, Card... hand) {
        setUp(defense, 0, 1, hand);
    }

    private void player(StatType stat, double value) {
        state.playerStats().add(StatModifier.flat(stat, value, "test"));
    }

    private void play() {
        engine.submit(new Action.PlayCard(0));
        if (!engine.isOver()) {
            engine.startTurn();
        }
    }

    // --- Sorts ---

    @Test
    void jetDEauUseLaDefDeLAdversaire() {
        setUp(3, CardPool.jetDEau(), CardPool.jetDEau());
        play(); // 10 − 3 = 7 ; DEF 3 → 2
        assertEquals(193, enemy.hp());
        assertEquals(2, enemy.stat(StatType.DEFENSE), 1e-9);
        play(); // 10 − 2 = 8
        assertEquals(185, enemy.hp());
        assertEquals(1, enemy.stat(StatType.DEFENSE), 1e-9);
    }

    @Test
    void tsunamiEtPainPitaIgnorentLaDef() {
        setUp(4, CardPool.tsunami(), CardPool.painPita());
        play();
        assertEquals(185, enemy.hp());
        play();
        assertEquals(175, enemy.hp());
        assertEquals(15, state.spellDamage(CardPool.tsunami()), "aperçu sans la DEF");
    }

    @Test
    void cayouuuIgnoreDeuxPointsDeDef() {
        setUp(3, CardPool.cayouuu());
        play(); // 12 − (3 − 2)
        assertEquals(189, enemy.hp());
    }

    @Test
    void pluieDeFlammesEtTempeteDEpinesVingtPourcent() {
        int burns = 0;
        int bondages = 0;
        int runs = 600;
        for (long seed = 0; seed < runs; seed++) {
            setUp(0, 0, seed, CardPool.pluieDeFlammes(), CardPool.tempeteDEpines());
            engine.submit(new Action.PlayCard(0));
            burns += state.statuses(StatusTarget.ENEMY).has(StatusType.BURN) ? 1 : 0;
            engine.startTurn();
            engine.submit(new Action.PlayCard(0));
            bondages += state.statuses(StatusTarget.ENEMY).has(StatusType.BONDAGE) ? 1 : 0;
        }
        assertTrue(burns > 0.15 * runs && burns < 0.25 * runs, "Brûlure : " + burns);
        assertTrue(bondages > 0.15 * runs && bondages < 0.25 * runs, "Bondage : " + bondages);
        assertEquals(15, CardPool.pluieDeFlammes().damage(), 1e-9);
        assertEquals(15, CardPool.tempeteDEpines().damage(), 1e-9);
    }

    @Test
    void lancerDeManetteCritiqueGarantiATroisCha() {
        setUp(0, CardPool.lancerDeManette());
        player(StatType.LUCK, 3);
        play(); // 12 × 1,5
        assertEquals(182, enemy.hp());
        assertTrue(events.contains(new GameEvent.CriticalHit(enemy)));
    }

    @Test
    void lancerDeManetteSansCritiqueSousTroisCha() {
        setUp(0, CardPool.lancerDeManette());
        play(); // CHA 0 : ni critique garanti, ni chance de critique
        assertEquals(188, enemy.hp());
    }

    @Test
    void bigLezShotgunReculeDeDeuxForPourLeCombat() {
        setUp(0, CardPool.bigLezShotgun(), new cardistry.model.card.SpellCard("Coup", Element.NEUTRAL, 10, Rarity.COMMON));
        player(StatType.STRENGTH, 3);
        play(); // 80 + 3
        assertEquals(117, enemy.hp());
        assertEquals(1, state.playerStats().get(StatType.STRENGTH), 1e-9);
        play(); // 10 + 1
        assertEquals(106, enemy.hp());
        assertEquals(Rarity.LEGENDARY, CardPool.bigLezShotgun().rarity());
    }

    @Test
    void carteBasiquePasOufSixPlusDeuxFor() {
        setUp(0, CardPool.carteBasiquePasOuf(), CardPool.carteBasiquePasOuf());
        play(); // FOR 0 : 6
        assertEquals(194, enemy.hp());
        player(StatType.STRENGTH, 3);
        play(); // 6 + 2 × 3, la FOR n'est pas ajoutée une troisième fois
        assertEquals(182, enemy.hp());
    }

    @Test
    void contreExempleAnnuleUnPlusIntSurTroisStatuts() {
        setUp(0, CardPool.contreExemple());
        player(StatType.INTELLIGENCE, 7); // 1 + 2
        play();
        TemporaryEffect ward = state.effects().stream().filter(e -> e.cancelsNextStatus()).findFirst().orElseThrow();
        assertEquals(3, (int) ward.chargesLeft());
    }

    // --- Soins ---

    @Test
    void sparadrapDouzePvEtUneDefPourLeCombat() {
        setUp(0, CardPool.sparadrap());
        state.player().takeDamage(50);
        play(); // 12 × 1,5 (soin neutre sur terrain neutre)
        assertEquals(68, state.player().hp());
        assertEquals(1, state.playerStats().get(StatType.DEFENSE), 1e-9);
    }

    @Test
    void soupeDeMamieUneResPourLeCombat() {
        setUp(0, CardPool.soupeDeMamie());
        state.player().takeDamage(50);
        play(); // 10 × 1,5
        assertEquals(65, state.player().hp());
        assertEquals(1, state.playerStats().get(StatType.RESISTANCE), 1e-9);
    }

    @Test
    void berceuseInconnueSoigneDeDeuxFoisLEsprit() {
        setUp(0, CardPool.berceuseInconnue(), CardPool.berceuseInconnue());
        state.player().takeDamage(50);
        play(); // ESP 0 : aucun soin
        assertEquals(50, state.player().hp());
        player(StatType.MIND, 3);
        play(); // 2 × 3, terrain neutre : ×1, pas d'ESP en plus
        assertEquals(56, state.player().hp());
    }

    @Test
    void lesCartesDuDeckDeDepartSontLesMemes() {
        List<String> powers = CardPool.startingDeck().stream()
                .filter(c -> c.name().equals("Jet d'eau") || c.name().equals("Lancer de manette"))
                .map(c -> c.power().orElseThrow().text()).toList();
        assertEquals(List.of("l'adversaire perd 1 DEF pour le combat", "critique garanti si CHA ≥ 3"), powers);
        assertFalse(new CardPool().cards().stream().anyMatch(c -> c.name().equals("Tsunami") && c.power().isEmpty()));
    }
}
