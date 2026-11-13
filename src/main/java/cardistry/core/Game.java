package cardistry.core;

import cardistry.model.Element;
import cardistry.model.ElementRules;
import cardistry.model.Enemy;
import cardistry.model.EnemyRoster;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.HealCard;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import cardistry.ui.text.Ansi;
import java.util.List;
import java.util.Scanner;

// TEMPORAIRE : portage direct de l'ancien Jeu. L'affichage en sera retiré (événements + renderer).
public class Game {

    private final Scanner scanner = new Scanner(System.in);

    public int turn = 0;

    private final Enemy[] enemies;
    private Enemy currentEnemy;
    private TerrainCard terrain = new TerrainCard("EPFL", Element.FIRE);

    private final CardPool pool = new CardPool();
    private final Card[] hand = new Card[7];

    private final Player player = new Player();

    public Game() {
        deal();
        List<Enemy> roster = EnemyRoster.createDefault();
        enemies = roster.toArray(new Enemy[0]);
        currentEnemy = enemies[0];
    }

    public Player player() {
        return player;
    }

    private void deal() {
        for (int i = 0; i < hand.length; i++) {
            hand[i] = pool.draw();
        }
    }

    public void playerPlay() {
        int choice = -1;
        while (!checkInputPlay(choice)) {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Que veux-tu jouer ? ");
            choice = scanner.nextInt();
        }

        if (choice == 0) {
            getNewCard();
        } else {
            play(hand[choice - 1]);
            hand[choice - 1] = null;
            reStackHand(choice - 1);
            pause(1);
        }

        if (checkAdversaryAndReplace()) {
            double mult = terrainMultiplier();
            System.out.println(currentEnemy.name() + " t'attaque et fait " + mult + "*" + currentEnemy.attack() + " dégats");
            player.takeDamage(currentEnemy.attack() * mult);
            pause(1);
        }
    }

    private double terrainMultiplier() {
        return ElementRules.terrainAttackMultiplier(terrain.element(), currentEnemy.element());
    }

    private void play(Card card) {
        switch (card) {
            case SpellCard spell -> {
                double mult = ElementRules.spellMultiplier(spell.element(), currentEnemy.element());
                System.out.println("Tu joues " + spell.name() + " qui fait " + (mult * -spell.damage()));
                currentEnemy.takeDamage(spell.damage() * mult);
            }
            case HealCard heal -> {
                double mult = ElementRules.healMultiplier(heal.element(), terrain.element());
                System.out.println("Tu joues " + heal.name() + " qui te redonne " + (mult * heal.amount()));
                player.heal(heal.amount() * mult);
            }
            case TerrainCard newTerrain -> {
                System.out.println("Tu joues " + newTerrain.name() + " qui te téléporte de " + terrain.name() + " à " + newTerrain.name());
                terrain = newTerrain;
            }
        }
    }

    private void reStackHand(int indexPlayed) {
        for (int i = indexPlayed; i < hand.length - 1; i++) {
            hand[i] = hand[i + 1];
            hand[i + 1] = null;
        }
    }

    public boolean isWon() {
        for (Enemy enemy : enemies) {
            if (enemy != null && enemy.isAlive()) {
                return false;
            }
        }
        System.out.println("WINNNNNEEERRRRRRR");
        return true;
    }

    public void showGameState() {
        System.out.println("");
        System.out.println("===================");
        System.out.println("PLAYER:");
        System.out.println("Vous avez " + player.hp() + " points de vie");
        pause(1);
        System.out.println("");
        System.out.println("ADVERSARY:");
        System.out.println(describe(currentEnemy));
        System.out.println("Multiplicateur de l'adversaire: " + terrainMultiplier());
        pause(0.7);
        System.out.println("");
        System.out.println("TERRAIN ACTUEL:");
        System.out.println(describe(terrain));
        System.out.println("===================");
        pause(0.5);
        System.out.println("(Appuie sur Entrée)");
        scanner.nextLine();

        System.out.println("");
        System.out.println("");

        System.out.println("===================");
        System.out.println("VOS CARTES:");
        showCards();
        System.out.println("");
        System.out.println("[0] GET A NEW CARD");
        System.out.println("Vous avez " + numberOfCardsInHand() + "/7 cartes");
        System.out.println("===================");
        System.out.println("");
    }

    private static String describe(Enemy enemy) {
        return "Adversaire actuel: " + enemy.name() + " de type " + Ansi.element(enemy.element()) + ", il lui reste " + enemy.hp() + " points de vie";
    }

    private static String describe(Card card) {
        String type = Ansi.element(card.element());
        return switch (card) {
            case SpellCard s -> "Sortilege, nom: " + s.name() + ", type: " + type + ", dégats: " + (-s.damage());
            case HealCard h -> "Heal, nom: " + h.name() + ", type: " + type + ", heal: " + h.amount();
            case TerrainCard t -> "Terrain, " + t.name() + " de type " + type;
        };
    }

    public void playIntro() {
        System.out.println("Welcome to GAME NAME");
        System.out.println("");

        System.out.println("(Appuie sur Entrée pour continuer or s to skip)");

        String skip = scanner.nextLine();
        if (!skip.equals("s")) {
            System.out.println("Il y 3 ennemis");
            pause(0.7);
            System.out.println("Le " + Ansi.yellow("Dragon") + " de type " + Ansi.element(Element.FIRE));
            pause(0.7);
            System.out.println("La " + Ansi.yellow("Sirène") + " de type " + Ansi.element(Element.WATER));
            pause(0.7);
            System.out.println("et le " + Ansi.yellow("Goblin") + " de type " + Ansi.element(Element.NATURE));
            pause(0.7);

            System.out.println("(Appuie sur Entrée pour continuer)");
            scanner.nextLine();

            System.out.println("à chaque tour");
            pause(0.7);
            System.out.println("Vous pouvez jouer une carte ou piocher");
            pause(0.7);
            System.out.println("Une carte peut être un " + Ansi.yellow("sortilège") + " qui fait des dégats à la créature");
            pause(0.9);
            System.out.println("Un " + Ansi.yellow("heal") + " qui vous rends des PV");
            pause(0.8);
            System.out.println("Ou un " + Ansi.yellow("terrain") + " qui vous téléporte à un autre endroit");
            pause(0.7);
            System.out.println("(Appuie sur Entrée pour continuer)");
            scanner.nextLine();

            System.out.println("Il y a des " + Ansi.yellow("élélments") + " !");
            pause(0.7);
            System.out.println(Ansi.element(Element.WATER, "Eau") + " bat " + Ansi.element(Element.FIRE, "Feu"));
            pause(0.7);
            System.out.println(Ansi.element(Element.FIRE, "Feu") + " bat " + Ansi.element(Element.NATURE, "Nature"));
            pause(0.7);
            System.out.println(Ansi.element(Element.NATURE, "Nature") + " bat " + Ansi.element(Element.WATER, "Eau"));
            pause(1);
            System.out.println("Un élément contraire donne un malus d'attaque à la créature");
            pause(0.8);
            System.out.println("Et vous donne un bonus contre une créature");
            pause(0.8);
            System.out.println("Les heals sont plus éfficaces dans leurs terrains et moins dans le mauvais");
            pause(0.8);
            System.out.println("(Appuie sur Entrée pour comemncer la partie)");
            scanner.nextLine();
        }
    }

    private void showCards() {
        int count = 0;
        for (Card card : hand) {
            ++count;
            if (card == null) {
                System.out.println("[" + count + "] " + "NO CARDS");
            } else {
                System.out.println("[" + count + "] " + describe(card));
            }
        }
    }

    private int numberOfCardsInHand() {
        int count = 0;
        for (Card card : hand) {
            if (card != null) {
                count++;
            }
        }
        return count;
    }

    private void getNewCard() {
        int i = 0;
        while (hand[i] != null) {
            i++;
        }
        if (i <= 6) {
            Card newCard = pool.draw();
            hand[i] = newCard;
            System.out.println("Tu as reçu : " + describe(newCard));
        } else {
            System.out.println("NO SPACE AVAILABLE");
        }
    }

    private boolean checkAdversaryAndReplace() {
        int i = 0;
        while (i < enemies.length && enemies[i] == null) {
            i++;
        }

        if (!enemies[i].isAlive() && i != enemies.length - 1) {
            System.out.println("change adv");
            replaceAdversary(i);
            return true;
        } else if (!enemies[i].isAlive() && i == enemies.length - 1) {
            return false;
        } else {
            return true;
        }
    }

    private void replaceAdversary(int i) {
        enemies[i] = null;
        System.out.println("NEW CHALLENGER");
        currentEnemy = enemies[i + 1];
        System.out.println(describe(currentEnemy));
    }

    private boolean checkInputPlay(int input) {
        if ((input > 7 || input < 1) && (input != 0)) {
            System.out.println("Veuillez choisir un nombre entre 0 et 7");
            return false;
        } else if (input == 0) {
            if (numberOfCardsInHand() == 7) {
                System.out.println("Votre main est pleine");
                return false;
            } else {
                return true;
            }
        } else if (hand[input - 1] == null) {
            System.out.println("Veuillez choisir un slot qui contient une carte");
            return false;
        }
        return true;
    }

    public static void pause(double seconds) {
        try {
            Thread.sleep((int) (seconds * 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
