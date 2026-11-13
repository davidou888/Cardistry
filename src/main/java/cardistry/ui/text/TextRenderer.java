package cardistry.ui.text;

import cardistry.core.GameState;
import cardistry.core.event.GameEvent;
import cardistry.model.Element;
import cardistry.model.Enemy;
import cardistry.model.card.Card;
import cardistry.model.card.HealCard;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import java.util.List;

/** Rendu texte provisoire (avant Lanterna) : println + couleurs ANSI + pauses. */
public final class TextRenderer implements cardistry.ui.Renderer {

    private final Console console;

    public TextRenderer(Console console) {
        this.console = console;
    }

    @Override
    public void onEvent(GameEvent event) {
        switch (event) {
            case GameEvent.GameStarted e -> {
                console.println("Welcome to GAME NAME");
                console.println("Seed : " + e.seed());
                console.println();
            }
            case GameEvent.TurnStarted e -> {
                if (e.turn() > 1) {
                    console.pause(1);
                }
            }
            case GameEvent.CardPlayed e -> console.println("Tu joues " + e.card().name());
            case GameEvent.CardDrawn e -> {
                console.println("Tu as reçu : " + describe(e.card()));
                console.pause(1);
            }
            case GameEvent.DamageDealt e -> {
                if (e.target() instanceof Enemy enemy) {
                    console.println("→ " + enemy.name() + " subit " + e.amount() + " dégâts (×" + e.multiplier() + ")");
                } else {
                    console.println("→ Tu subis " + e.amount() + " dégâts (×" + e.multiplier() + ")");
                }
                console.pause(1);
            }
            case GameEvent.Healed e -> {
                console.println("→ Tu récupères " + e.amount() + " PV (×" + e.multiplier() + ")");
                console.pause(1);
            }
            case GameEvent.TerrainChanged e -> {
                console.println("→ Tu es téléporté de " + e.previous().name() + " à " + e.current().name());
                console.pause(1);
            }
            case GameEvent.EnemyAttacked e -> console.println(e.enemy().name() + " t'attaque");
            case GameEvent.EnemyKilled e -> {
            }
            case GameEvent.EnemyArrived e -> {
                console.println("change adv");
                console.println("NEW CHALLENGER");
                console.println(describe(e.enemy()));
            }
            case GameEvent.ActionRejected e -> console.println(switch (e.reason()) {
                case INVALID_CHOICE -> "Veuillez choisir un nombre entre 0 et 7";
                case HAND_FULL -> "Votre main est pleine";
                case EMPTY_SLOT -> "Veuillez choisir un slot qui contient une carte";
            });
            case GameEvent.GameWon e -> console.println("WINNNNNEEERRRRRRR");
            case GameEvent.GameLost e -> {
            }
        }
    }

    @Override
    public void showStatus(GameState state) {
        console.println();
        console.println("===================");
        console.println("PLAYER:");
        console.println("Vous avez " + state.player().hp() + " points de vie");
        console.pause(1);
        console.println();
        console.println("ADVERSARY:");
        console.println(describe(state.currentEnemy()));
        console.println("Multiplicateur de l'adversaire: " + state.enemyAttackMultiplier());
        console.pause(0.7);
        console.println();
        console.println("TERRAIN ACTUEL:");
        console.println(describe(state.terrain()));
        console.println("===================");
        console.pause(0.5);
    }

    @Override
    public void showHand(GameState state) {
        console.println();
        console.println();
        console.println("===================");
        console.println("VOS CARTES:");
        List<Card> hand = state.hand();
        for (int i = 0; i < state.handSize(); i++) {
            String line = i < hand.size() ? describe(hand.get(i)) : "NO CARDS";
            console.println("[" + (i + 1) + "] " + line);
        }
        console.println();
        console.println("[0] GET A NEW CARD");
        console.println("Vous avez " + hand.size() + "/" + state.handSize() + " cartes");
        console.println("===================");
        console.println();
        console.pause(1);
    }

    @Override
    public void showIntro(GameState state) {
        console.println("Il y 3 ennemis");
        console.pause(0.7);
        console.println("Le " + Ansi.yellow("Dragon") + " de type " + Ansi.element(Element.FIRE));
        console.pause(0.7);
        console.println("La " + Ansi.yellow("Sirène") + " de type " + Ansi.element(Element.WATER));
        console.pause(0.7);
        console.println("et le " + Ansi.yellow("Goblin") + " de type " + Ansi.element(Element.NATURE));
        console.pause(0.7);
        waitForEnter("(Appuie sur Entrée pour continuer)");

        console.println("à chaque tour");
        console.pause(0.7);
        console.println("Vous pouvez jouer une carte ou piocher");
        console.pause(0.7);
        console.println("Une carte peut être un " + Ansi.yellow("sortilège") + " qui fait des dégats à la créature");
        console.pause(0.9);
        console.println("Un " + Ansi.yellow("heal") + " qui vous rends des PV");
        console.pause(0.8);
        console.println("Ou un " + Ansi.yellow("terrain") + " qui vous téléporte à un autre endroit");
        console.pause(0.7);
        waitForEnter("(Appuie sur Entrée pour continuer)");

        console.println("Il y a des " + Ansi.yellow("élélments") + " !");
        console.pause(0.7);
        console.println(Ansi.element(Element.WATER, "Eau") + " bat " + Ansi.element(Element.FIRE, "Feu"));
        console.pause(0.7);
        console.println(Ansi.element(Element.FIRE, "Feu") + " bat " + Ansi.element(Element.NATURE, "Nature"));
        console.pause(0.7);
        console.println(Ansi.element(Element.NATURE, "Nature") + " bat " + Ansi.element(Element.WATER, "Eau"));
        console.pause(1);
        console.println("Un élément contraire donne un malus d'attaque à la créature");
        console.pause(0.8);
        console.println("Et vous donne un bonus contre une créature");
        console.pause(0.8);
        console.println("Les heals sont plus éfficaces dans leurs terrains et moins dans le mauvais");
        console.pause(0.8);
        waitForEnter("(Appuie sur Entrée pour comemncer la partie)");
    }

    private void waitForEnter(String prompt) {
        console.println(prompt);
        console.readLine();
    }

    private static String describe(Enemy enemy) {
        return "Adversaire actuel: " + enemy.name() + " de type " + Ansi.element(enemy.element())
                + ", il lui reste " + enemy.hp() + " points de vie";
    }

    private static String describe(Card card) {
        String type = Ansi.element(card.element());
        return switch (card) {
            case SpellCard s -> "Sortilege, nom: " + s.name() + ", type: " + type + ", dégats: " + s.damage();
            case HealCard h -> "Heal, nom: " + h.name() + ", type: " + type + ", heal: " + h.amount();
            case TerrainCard t -> "Terrain, " + t.name() + " de type " + type;
        };
    }
}
