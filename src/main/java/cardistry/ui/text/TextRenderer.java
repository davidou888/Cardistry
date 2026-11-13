package cardistry.ui.text;

import static cardistry.ui.Format.capitalize;
import static cardistry.ui.Format.mult;
import static cardistry.ui.Format.num;

import cardistry.core.GameState;
import cardistry.core.event.GameEvent;
import cardistry.items.Collectible;
import cardistry.items.Trinket;
import cardistry.ui.ItemKeys;
import cardistry.model.Element;
import cardistry.model.ElementRules;
import cardistry.model.Enemy;
import cardistry.model.card.Card;
import cardistry.model.card.HealCard;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import cardistry.ui.Renderer;
import java.util.List;

/** Rendu texte provisoire (avant Lanterna) : println + couleurs ANSI + pauses. */
public final class TextRenderer implements Renderer {

    private static final String SEPARATOR = "===================";

    private final Console console;
    private int handSize;

    public TextRenderer(Console console) {
        this.console = console;
    }

    @Override
    public void onEvent(GameEvent event) {
        switch (event) {
            case GameEvent.GameStarted e -> {
                console.println("Bienvenue dans " + Ansi.yellow("Cardistry") + " !");
                console.println("Seed : " + e.seed());
                console.println();
            }
            case GameEvent.PlayerTurnStarted e -> {
                // Rien de plus à afficher.
            }
            case GameEvent.TurnStarted e -> {
                if (e.turn() > 1) {
                    console.pause(1);
                }
            }
            case GameEvent.CardPlayed e -> console.println("Tu joues la carte " + cardName(e.card()));
            case GameEvent.ItemUsed e -> console.println("Tu utilises l'objet " + Ansi.yellow(e.item().name()));
            case GameEvent.CardReplaced e -> console.println("→ " + cardName(e.removed()) + " est remplacée par "
                    + cardName(e.replacement()));
            case GameEvent.CardDrawn e -> {
                console.println("Tu pioches : " + describe(e.card()));
                console.pause(1);
            }
            case GameEvent.DamageDealt e -> {
                if (e.target() instanceof Enemy enemy) {
                    console.println("→ " + enemy.name() + " subit " + num(e.amount()) + " dégâts" + mult(e.multiplier()));
                } else {
                    console.println("→ Tu subis " + num(e.amount()) + " dégâts" + mult(e.multiplier()));
                }
                console.pause(1);
            }
            case GameEvent.Healed e -> {
                console.println("→ Tu récupères " + num(e.amount()) + " PV" + mult(e.multiplier()));
                console.pause(1);
            }
            case GameEvent.TerrainChanged e -> {
                console.println("→ Tu es téléporté de " + cardName(e.previous()) + " à " + cardName(e.current()));
                console.pause(1);
            }
            case GameEvent.EnemyAttacked e -> console.println(enemyName(e.enemy()) + " t'attaque !");
            case GameEvent.EnemyKilled e -> {
                console.println("Tu as vaincu " + enemyName(e.enemy()) + " !");
                console.pause(1);
            }
            case GameEvent.CombatStarted e -> {
                console.println();
                console.println(Ansi.yellow("Combat " + e.number() + "/" + e.total()) + " : " + enemyName(e.enemy())
                        + " (" + Ansi.element(e.enemy().element()) + ", " + num(e.enemy().hp()) + " PV)");
                console.pause(1);
            }
            case GameEvent.HandDealt e -> console.println("Tu reçois une nouvelle main de " + e.cards().size() + " cartes.");
            case GameEvent.ActionRejected e -> console.println(switch (e.reason()) {
                case INVALID_CHOICE -> "Choisis un nombre entre 0 et " + handSize;
                case HAND_FULL -> "Ta main est pleine";
                case EMPTY_SLOT -> "Ce slot est vide, choisis une carte";
                case NO_ITEM -> "Pas d'objet à cet emplacement";
                case INVALID_REWARD -> "Choix de récompense invalide";
                case INVENTORY_FULL -> "Inventaire plein : choisis quoi remplacer";
                case NO_CIGARETTE -> "Plus de clope : roule-en une (g)";
                case PACK_FULL -> "Paquet plein : impossible de rouler";
            });
            case GameEvent.EffectAdded e -> console.println("→ " + e.effect().describe());
            case GameEvent.EffectActivated e -> console.println("→ " + Ansi.yellow(e.effect().name()) + " se déclenche !");
            case GameEvent.EffectEnded e -> console.println("→ " + e.effect().name() + " : terminé.");
            case GameEvent.PlayerTurnSkipped e -> {
                console.println(Ansi.RED + "Tu sautes ce tour" + Ansi.END_COLOR + " (" + String.join(", ", e.causes()) + ").");
                console.pause(1);
            }
            case GameEvent.AttackCancelled e -> console.println("→ " + e.cause() + " annule l'attaque de "
                    + enemyName(e.enemy()) + " !");
            case GameEvent.DrunkennessChanged e -> {
                if (e.drunkenness() > 0) {
                    console.println("→ Ivresse : " + e.drunkenness() + "/" + e.threshold());
                }
            }
            case GameEvent.CurseGained e -> console.println(Ansi.RED + "Gueule de bois en vue !" + Ansi.END_COLOR
                    + " " + e.curse().displayName() + " (" + e.curse().description(1) + ") au prochain combat.");
            case GameEvent.CurseStarted e -> console.println(Ansi.RED + e.curse().displayName() + Ansi.END_COLOR
                    + " : " + e.curse().description(lastState.activeIntensity(e.curse())) + " pendant ce combat.");
            case GameEvent.CurseReduced e -> console.println("→ " + e.curse().displayName()
                    + (e.intensity() == 0 ? " évitée !" : " réduite (" + e.curse().description(e.intensity()) + ")")
                    + (e.pending() && e.intensity() > 0 ? " au prochain combat" : ""));
            case GameEvent.CigarettesChanged e -> {
                // Affiché dans l'état.
            }
            case GameEvent.CigaretteSmoked e -> console.println(e.transformedIntoPetitPetou()
                    ? Ansi.yellow("La clope se transforme en Petit pétou !")
                    : "Tu fumes une clope.");
            case GameEvent.CigaretteRolled e -> console.println("Tu roules une clope.");
            case GameEvent.CurseEnded e -> console.println(e.curse().displayName() + " terminée.");
            case GameEvent.RewardOffered e -> {
                console.println();
                console.println(Ansi.yellow("Récompense !") + " Choisis-en une :");
                for (int i = 0; i < e.options().size(); i++) {
                    console.println("[" + (i + 1) + "] " + describe(e.options().get(i)));
                }
                console.println("[0] Refuser");
            }
            case GameEvent.RewardTaken e -> console.println("Tu prends " + Ansi.yellow(e.taken().name())
                    + (e.replaced() != null ? " à la place de " + e.replaced().name() : "") + ".");
            case GameEvent.RewardSkipped e -> console.println("Tu refuses la récompense.");
            case GameEvent.GameWon e -> {
                console.println();
                console.println(Ansi.yellow("VICTOIRE !") + " Tu as vaincu tous les adversaires.");
            }
            case GameEvent.GameLost e -> {
                console.println();
                console.println(Ansi.RED + "DÉFAITE..." + Ansi.END_COLOR + " Tu n'as plus de points de vie.");
            }
        }
    }

    /** Dernier état affiché (pour les événements qui ne portent pas l'état). */
    private GameState lastState = GameState.newGame();

    @Override
    public void showStatus(GameState state) {
        lastState = state;
        handSize = state.handSize();
        Enemy enemy = state.currentEnemy();
        console.println();
        console.println(SEPARATOR + " Tour " + state.turn());
        console.println("JOUEUR :");
        console.println("Tu as " + num(state.player().hp()) + "/" + num(state.maxHp()) + " points de vie");
        console.pause(1);
        console.println();
        console.println("ADVERSAIRE :");
        console.println(enemyName(enemy) + ", de type " + Ansi.element(enemy.element())
                + ", il lui reste " + num(enemy.hp()) + " points de vie");
        double reduction = state.playerStats().get(cardistry.core.stats.StatType.DAMAGE_REDUCTION);
        console.println("Attaque : " + num(enemy.attack()) + mult(state.enemyAttackMultiplier())
                + (reduction > 0 ? " − " + num(reduction) : "") + " = " + num(state.enemyAttackDamage()));
        console.pause(0.7);
        console.println();
        console.println("TERRAIN ACTUEL :");
        console.println(describe(state.terrain()));
        console.println();
        if (state.drunkenness() > 0) {
            console.println("Ivresse : " + state.drunkenness() + "/" + state.hangoverThreshold());
        }
        for (cardistry.core.Curse curse : state.activeCurses()) {
            console.println(Ansi.RED + curse.displayName() + Ansi.END_COLOR + " : "
                    + curse.description(state.activeIntensity(curse)));
        }
        for (cardistry.core.Curse curse : state.pendingCurses()) {
            console.println(curse.displayName() + " au prochain combat");
        }
        if (!state.effects().isEmpty()) {
            console.println("EFFETS EN COURS :");
            for (cardistry.core.TemporaryEffect effect : state.effects()) {
                console.println("- " + effect.describe());
            }
        }
        console.println("TRINKETS (" + state.trinkets().size() + "/" + state.trinketSlots() + ") :"
                + (state.trinkets().isEmpty() ? " aucun" : ""));
        for (Trinket trinket : state.trinkets()) {
            console.println("- " + describe(trinket));
        }
        console.println(SEPARATOR);
        console.pause(0.5);
    }

    @Override
    public void showHand(GameState state) {
        console.println();
        console.println();
        console.println(SEPARATOR);
        console.println("TES CARTES :");
        List<Card> hand = state.hand();
        for (int i = 0; i < state.handSize(); i++) {
            String line = i < hand.size() ? describe(hand.get(i)) : "(vide)";
            console.println("[" + (i + 1) + "] " + line);
        }
        console.println();
        console.println("[0] Piocher une carte");
        for (int i = 0; i < state.items().size(); i++) {
            console.println("[" + ItemKeys.key(i) + "] Utiliser l'" + describe(state.items().get(i)).replaceFirst("^Objet", "objet")
                    + " (gratuit)");
        }
        console.println("Tu as " + hand.size() + "/" + state.handSize() + " cartes");
        console.println(SEPARATOR);
        console.println();
        console.pause(1);
    }

    @Override
    public void showIntro(GameState state) {
        List<Enemy> enemies = state.enemies();
        console.println("Il y a " + enemies.size() + " ennemis :");
        console.pause(0.7);
        for (Enemy enemy : enemies) {
            console.println("- " + enemyName(enemy) + ", de type " + Ansi.element(enemy.element()));
            console.pause(0.7);
        }
        waitForEnter("(Appuie sur Entrée pour continuer)");

        console.println("À chaque tour,");
        console.pause(0.7);
        console.println("tu peux jouer une carte ou piocher.");
        console.pause(0.7);
        console.println("Une carte peut être un " + Ansi.yellow("sortilège") + ", qui fait des dégâts à la créature,");
        console.pause(0.9);
        console.println("un " + Ansi.yellow("soin") + ", qui te rend des PV,");
        console.pause(0.8);
        console.println("ou un " + Ansi.yellow("terrain") + ", qui te téléporte à un autre endroit.");
        console.pause(0.7);
        waitForEnter("(Appuie sur Entrée pour continuer)");

        console.println("Il y a des " + Ansi.yellow("éléments") + " !");
        console.pause(0.7);
        for (Element winner : Element.values()) {
            for (Element loser : Element.values()) {
                if (winner.beats(loser)) {
                    console.println(Ansi.element(winner, capitalize(winner.displayName()))
                            + " bat " + Ansi.element(loser, capitalize(loser.displayName())));
                    console.pause(0.7);
                }
            }
        }
        console.pause(0.3);
        console.println("Un sortilège fait ×" + num(ElementRules.ADVANTAGE) + " contre une créature qu'il bat,"
                + " et ×" + num(ElementRules.DISADVANTAGE) + " contre une créature qui le bat.");
        console.pause(0.8);
        console.println("Sur un terrain de son élément, la créature attaque plus fort (×"
                + num(ElementRules.TERRAIN_STRENGTHENS) + ") ;");
        console.println("sur un terrain qui la bat, elle attaque moins fort (×" + num(ElementRules.TERRAIN_WEAKENS) + ").");
        console.pause(0.8);
        console.println("Les soins sont plus efficaces sur un terrain de leur élément (×"
                + num(ElementRules.HEAL_SAME_TERRAIN) + ")");
        console.println("et moins sur un terrain qui les bat (×" + num(ElementRules.HEAL_HOSTILE_TERRAIN) + ").");
        console.pause(0.8);
        waitForEnter("(Appuie sur Entrée pour commencer la partie)");
    }

    private void waitForEnter(String prompt) {
        console.println(prompt);
        console.readLine();
    }

    private static String enemyName(Enemy enemy) {
        return Ansi.yellow(enemy.name());
    }

    private static String cardName(Card card) {
        return Ansi.element(card.element(), card.name());
    }

    static String describe(Collectible c) {
        return c.kindName() + " " + Ansi.yellow(c.name()) + " (" + c.rarity().displayName() + ") : " + c.description();
    }

    private static String describe(Card card) {
        String type = Ansi.element(card.element());
        return switch (card) {
            case SpellCard s -> "Sortilège " + cardName(s) + " (" + type + ") : " + num(s.damage()) + " dégâts";
            case HealCard h -> "Soin " + cardName(h) + " (" + type + ") : " + num(h.amount()) + " PV";
            case TerrainCard t -> "Terrain " + cardName(t) + " (" + type + ")";
        };
    }
}
