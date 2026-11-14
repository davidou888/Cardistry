package cardistry.ui.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.Action;
import cardistry.core.CombatEngine;
import cardistry.core.GameState;
import cardistry.core.Rng;
import cardistry.core.event.EventBus;
import cardistry.items.ItemRegistry;
import cardistry.run.Secret;
import cardistry.ui.TitleChoice;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Mode texte : code secret sur l'écran titre, annulation. */
class ConsoleInputTest {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    private ConsoleInput input(String typed) {
        Console console = new Console(new ByteArrayInputStream(typed.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(out, true, StandardCharsets.UTF_8), true, false, false);
        return new ConsoleInput(console);
    }

    @Test
    void codeClaudeSurLEcranTitre() {
        TitleChoice choice = input("claude\ns\n").title(12);
        assertEquals(List.of(Secret.CLAUDE_MD), choice.codes());
        assertFalse(choice.showRules());
        String text = out.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("Seed : 12"));
        assertTrue(text.contains("Code accepté : CLAUDE.md"));
    }

    @Test
    void sansCode() {
        TitleChoice choice = input("\n").title(1);
        assertTrue(choice.codes().isEmpty());
        assertTrue(choice.showRules());
    }

    @Test
    void zAnnuleAvecCtrlZ() {
        GameState state = GameState.newGame();
        CombatEngine engine = new CombatEngine(state, new cardistry.model.card.CardPool(), new Rng(1), new EventBus());
        assertEquals(new Action.DrawCard(), input("z\n0\n").nextAction(state), "sans Ctrl+Z, z n'annule rien");
        engine.equip(ItemRegistry.trinket(ItemRegistry.CTRL_Z));
        assertEquals(new Action.Undo(), input("z\n").nextAction(state));
    }

    @Test
    void carteEnModeTexte() {
        cardistry.run.Run run = new cardistry.run.Run(4, java.util.Set.of(), new EventBus());
        run.start();
        cardistry.run.map.MapView view = new cardistry.run.map.MapView(run.state(), run.map().orElseThrow(),
                run.visited(), run.position(), run.choices());
        Console console = new Console(new ByteArrayInputStream("9\n2\n".getBytes(StandardCharsets.UTF_8)),
                new PrintStream(out, true, StandardCharsets.UTF_8), true, false, false);
        new TextRenderer(console).showMap(view);
        assertEquals(1, new ConsoleInput(console).chooseNode(view));
        String text = out.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("Carte · Niveau 1/3"), text);
        assertTrue(text.contains("[1] Étage 1 : Combat"));
        assertTrue(text.contains("[2] Étage 1 : Combat"));
        assertTrue(text.contains("B"));
        assertTrue(text.contains("Entre un nombre entre 1 et"), "9 est refusé");
    }

    @Test
    void shopEnModeTexte() {
        GameState state = GameState.newGame();
        CombatEngine engine = new CombatEngine(state, new cardistry.model.card.CardPool(), new Rng(3), new EventBus());
        engine.start();
        for (int i = 0; i < 3; i++) {
            engine.give(ItemRegistry.item("mate"));
        }
        engine.gainChoomies(985); // 1000
        engine.openShop();
        cardistry.core.Shop shop = engine.shop().orElseThrow();
        // « 6 » : premier objet, inventaire plein → remplacer le 2e ; puis r, t + carte 3, 0.
        ConsoleInput input = input("x\n6\n2\nr\nt\n3\n0\n");
        assertEquals(new cardistry.core.shop.ShopAction.Buy(5, 1), input.chooseShopAction(state, shop, 75));
        assertEquals(new cardistry.core.shop.ShopAction.Reroll(), input.chooseShopAction(state, shop, 75));
        assertEquals(new cardistry.core.shop.ShopAction.RemoveCard(2), input.chooseShopAction(state, shop, 75));
        assertEquals(new cardistry.core.shop.ShopAction.Leave(), input.chooseShopAction(state, shop, 75));
        String text = out.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("SHOP · 1000 Choomies"), text);
        assertTrue(text.contains("Cartes :") && text.contains("Objets :") && text.contains("Trinkets :"));
        assertTrue(text.contains("r) Reroll : 20"));
        assertTrue(text.contains("t) Retirer une carte : 75"));
        assertTrue(text.contains("Inventaire plein"));
        assertTrue(text.contains("Entre un numéro d'article"), "« x » est refusé");
    }

    @Test
    void shopTropCherEnModeTexte() {
        GameState state = GameState.newGame();
        CombatEngine engine = new CombatEngine(state, new cardistry.model.card.CardPool(), new Rng(3), new EventBus());
        engine.start();
        engine.openShop();
        input("0\n").chooseShopAction(state, engine.shop().orElseThrow(), 75);
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("(trop cher)"));
    }
}
