package cardistry;

import cardistry.core.Game;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Game game = new Game();

        game.playIntro();

        while (!game.isWon() && game.player().isAlive()) {
            game.turn++;
            game.showGameState();

            Game.pause(1);
            game.playerPlay();
            Game.pause(1);
        }
    }
}
