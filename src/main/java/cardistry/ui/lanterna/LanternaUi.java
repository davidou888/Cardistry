package cardistry.ui.lanterna;

import static cardistry.ui.Format.mult;
import static cardistry.ui.Format.num;

import cardistry.core.Action;
import cardistry.core.GameState;
import cardistry.core.RewardChoice;
import cardistry.core.event.GameEvent;
import cardistry.items.Collectible;
import cardistry.model.Enemy;
import cardistry.ui.ItemKeys;
import cardistry.ui.Input;
import cardistry.ui.QuitGameException;
import cardistry.ui.Renderer;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Interface plein écran (Lanterna) : affichage, animations et clavier. */
public final class LanternaUi implements Renderer, Input, AutoCloseable {

    private enum Page { TITLE, RULES, COMBAT, INVENTORY, REWARD, REPLACE, DISCARD, GAME_OVER }

    private static final int FRAME_MS = 45;
    private static final int POLL_MS = 15;

    private final Screen screen;
    private final boolean fast;
    private final ViewState view = new ViewState();

    private Page page = Page.TITLE;
    private GameState state;
    private long seed;
    private List<Collectible> rewardOptions = List.of();
    private int rewardSelected = 0;
    private int replaceSelected = 0;
    private int discardSelected = 0;

    /** Ouvre le terminal courant en plein écran. */
    public static LanternaUi open(boolean fast) throws IOException {
        Terminal terminal = new DefaultTerminalFactory(System.out, System.in, StandardCharsets.UTF_8)
                .setForceTextTerminal(true)
                .createTerminal();
        return new LanternaUi(terminal, fast);
    }

    /** @param fast si {@code true}, les animations ne font aucune pause */
    public LanternaUi(Terminal terminal, boolean fast) throws IOException {
        this.screen = new TerminalScreen(terminal);
        this.fast = fast;
        screen.startScreen();
        screen.setCursorPosition(null);
    }

    @Override
    public void close() throws IOException {
        screen.stopScreen();
    }

    // ===================== Renderer =====================

    @Override
    public void onEvent(GameEvent event) {
        switch (event) {
            case GameEvent.GameStarted e -> seed = e.seed();
            case GameEvent.TurnStarted e -> {
                view.log("— Tour " + e.turn() + " —", Palette.DIM);
                view.toast = null;
            }
            case GameEvent.PlayerTurnStarted e -> {
                // Rien de plus à afficher.
            }
            case GameEvent.CardPlayed e -> view.log("Tu joues la carte " + e.card().name(), Palette.TEXT);
            case GameEvent.CardReplaced e -> {
                view.log(e.removed().name() + " remplacée par " + e.replacement().name(), Palette.INFO);
                render();
            }
            case GameEvent.CardChoiceOffered e -> {
                // Le choix est affiché par l'écran de choix.
            }
            case GameEvent.CardChosen e -> view.log("Tu gardes " + e.kept().name() + ", tu défausses "
                    + String.join(", ", e.discarded().stream().map(cardistry.model.card.Card::name).toList()), Palette.INFO);
            case GameEvent.ItemUsed e -> {
                view.log("Tu utilises l'objet " + e.item().name(), Palette.INFO);
                render();
            }
            case GameEvent.CardDrawn e -> {
                view.log("Tu pioches " + e.card().name(), Palette.INFO);
                animateDraw();
            }
            case GameEvent.DamageDealt e -> {
                if (e.target() instanceof Enemy enemy) {
                    view.log(enemy.name() + " subit " + num(e.amount()) + " dégâts" + mult(e.multiplier()), Palette.ACCENT);
                    animateEnemyHit(enemy, e.amount());
                } else {
                    view.log("Tu subis " + num(e.amount()) + " dégâts" + mult(e.multiplier()), Palette.DAMAGE);
                    animatePlayerHit(e.amount());
                }
            }
            case GameEvent.Healed e -> {
                view.log("Tu récupères " + num(e.amount()) + " PV" + mult(e.multiplier()), Palette.HEAL);
                animateHeal(e.amount());
            }
            case GameEvent.TerrainChanged e -> {
                view.log("Téléportation : " + e.previous().name() + " → " + e.current().name(), Palette.INFO);
                animateTerrain();
            }
            case GameEvent.EnemyAttacked e -> {
                view.log(e.enemy().name() + " t'attaque !", Palette.DAMAGE);
                animateEnemyAttack();
            }
            case GameEvent.EnemyKilled e -> {
                view.log("Tu as vaincu " + e.enemy().name() + " !", Palette.ACCENT);
                animateEnemyDeath();
            }
            case GameEvent.CombatStarted e -> {
                view.log("Combat " + e.number() + "/" + e.total() + " : " + e.enemy().name(), Palette.WARNING);
                if (e.number() > 1) {
                    animateEnemyArrival();
                }
            }
            case GameEvent.HandDealt e -> {
                view.log("Nouvelle main : " + e.cards().size() + " cartes", Palette.INFO);
                view.selected = 0;
            }
            case GameEvent.ActionRejected e -> {
                view.log(switch (e.reason()) {
                    case INVALID_CHOICE -> "Pas de carte à ce numéro.";
                    case HAND_FULL -> "Ta main est pleine : joue une carte.";
                    case EMPTY_SLOT -> "Cet emplacement est vide.";
                    case NO_ITEM -> "Pas d'objet à cet emplacement.";
                    case INVALID_REWARD -> "Choix de récompense invalide.";
                    case INVENTORY_FULL -> "Inventaire plein : choisis quoi remplacer.";
                    case NO_CIGARETTE -> "Plus de clope : G pour en rouler une.";
                    case PACK_FULL -> "Paquet plein : impossible de rouler.";
                }, Palette.WARNING);
                render();
            }
            case GameEvent.EffectAdded e -> view.log(e.effect().describe(), Palette.INFO);
            case GameEvent.EffectActivated e -> {
                view.log(e.effect().name() + " se déclenche !", Palette.ACCENT);
                render();
            }
            case GameEvent.EffectEnded e -> view.log(e.effect().name() + " : terminé", Palette.DIM);
            case GameEvent.PlayerTurnSkipped e -> {
                view.log("Tu sautes ce tour (" + String.join(", ", e.causes()) + ")", Palette.DAMAGE);
                render();
                pause(600);
            }
            case GameEvent.EnemyTurnSkipped e -> {
                view.log(e.message(), Palette.HEAL);
                render();
            }
            case GameEvent.AttackCancelled e -> view.log(e.cause() + " annule l'attaque de " + e.enemy().name(),
                    Palette.HEAL);
            case GameEvent.DrunkennessChanged e -> {
                if (e.drunkenness() > 0) {
                    view.log("Ivresse " + e.drunkenness() + "/" + e.threshold(), Palette.WARNING);
                }
                render();
            }
            case GameEvent.CurseGained e -> view.log(e.curse().displayName() + " au prochain combat ("
                    + e.curse().description(1) + ")", Palette.DAMAGE);
            case GameEvent.CurseStarted e -> view.log(e.curse().displayName() + " : "
                    + e.curse().description(state == null ? 1 : state.activeIntensity(e.curse())), Palette.DAMAGE);
            case GameEvent.CurseReduced e -> view.log(e.curse().displayName()
                    + (e.intensity() == 0 ? " évitée !" : " réduite (" + e.curse().description(e.intensity()) + ")"),
                    Palette.HEAL);
            case GameEvent.CigarettesChanged e -> render();
            case GameEvent.CigaretteSmoked e -> {
                if (e.transformedIntoPetitPetou()) {
                    view.log("La clope se transforme en Petit pétou !", Palette.ACCENT);
                    for (int i = 0; i < 4; i++) {
                        view.floating.clear();
                        view.floating.add(new ViewState.Floating("Petit pétou !", Palette.ACCENT, false, i / 2));
                        frame();
                    }
                    view.resetEffects();
                } else {
                    view.log("Tu fumes une clope", Palette.TEXT);
                }
                render();
            }
            case GameEvent.CigaretteRolled e -> view.log("Tu roules une clope", Palette.TEXT);
            case GameEvent.CurseEnded e -> view.log(e.curse().displayName() + " terminée", Palette.DIM);
            case GameEvent.RewardOffered e -> view.log("Récompense : choisis 1 parmi " + e.options().size(), Palette.ACCENT);
            case GameEvent.RewardTaken e -> view.log("Tu prends " + e.taken().name()
                    + (e.replaced() != null ? " (à la place de " + e.replaced().name() + ")" : ""), Palette.ACCENT);
            case GameEvent.SetTierActivated e -> {
                String text = "Set " + e.setName() + " " + e.count() + " activé : " + e.description();
                view.log(text, Palette.ACCENT);
                view.toast = text;
            }
            case GameEvent.SetTierDeactivated e -> view.log("Set " + e.setName() + " " + e.count() + " perdu : "
                    + e.description(), Palette.WARNING);
            case GameEvent.TrinketDiscarded e -> view.log("Tu jettes " + e.trinket().name(), Palette.WARNING);
            case GameEvent.RewardSkipped e -> view.log("Récompense refusée", Palette.DIM);
            case GameEvent.GameWon e -> view.log("Victoire !", Palette.ACCENT);
            case GameEvent.GameLost e -> view.log("Défaite...", Palette.DAMAGE);
        }
    }

    @Override
    public void showIntro(GameState state) {
        this.state = state;
        page = Page.RULES;
        render();
        while (true) {
            KeyStroke key = readKey();
            if (isQuit(key)) {
                throw new QuitGameException();
            }
            if (key.getKeyType() == KeyType.Enter || isChar(key, ' ')) {
                return;
            }
        }
    }

    @Override
    public void showStatus(GameState state) {
        enterCombat(state);
    }

    @Override
    public void showHand(GameState state) {
        enterCombat(state);
    }

    @Override
    public void showGameOver(GameState state) {
        this.state = state;
        pause(600);
        page = Page.GAME_OVER;
        render();
    }

    private void enterCombat(GameState state) {
        this.state = state;
        page = Page.COMBAT;
        clampSelection();
        render();
    }

    // ===================== Input =====================

    @Override
    public boolean wantsIntro() {
        page = Page.TITLE;
        render();
        while (true) {
            KeyStroke key = readKey();
            if (isQuit(key)) {
                throw new QuitGameException();
            }
            if (key.getKeyType() == KeyType.Enter) {
                return false;
            }
            if (isChar(key, 'r')) {
                return true;
            }
        }
    }

    @Override
    public void waitForContinue() {
        // Plein écran : pas besoin de valider entre l'état et la main.
    }

    @Override
    public Action nextAction(GameState state) {
        this.state = state;
        page = Page.COMBAT;
        clampSelection();
        render();
        while (true) {
            KeyStroke key = readKey();
            int handSize = state.hand().size();
            switch (key.getKeyType()) {
                case ArrowLeft -> moveSelection(-1, handSize);
                case ArrowRight -> moveSelection(1, handSize);
                case Enter -> {
                    if (handSize > 0) {
                        return new Action.PlayCard(view.selected);
                    }
                    return new Action.DrawCard();
                }
                case Escape -> confirmQuit();
                case Character -> {
                    char c = Character.toLowerCase(key.getCharacter());
                    if (c == 'q') {
                        confirmQuit();
                    } else if (c == 'p' || c == '0') {
                        return new Action.DrawCard();
                    } else if (c == 'f') {
                        return new Action.Smoke();
                    } else if (c == 'g') {
                        return new Action.RollCigarette();
                    } else if (c == 'i') {
                        showInventory();
                    } else if (ItemKeys.slot(c, state.itemSlots()) >= 0) {
                        return new Action.UseItem(ItemKeys.slot(c, state.itemSlots()));
                    } else if (c >= '1' && c <= '9') {
                        int slot = c - '1';
                        if (slot < handSize) {
                            view.selected = slot;
                        }
                        return new Action.PlayCard(slot);
                    } else if (c == 'h') {
                        moveSelection(-1, handSize);
                    } else if (c == 'l') {
                        moveSelection(1, handSize);
                    }
                }
                default -> {
                }
            }
        }
    }

    @Override
    public RewardChoice chooseReward(GameState state, List<Collectible> options) {
        this.state = state;
        this.rewardOptions = options;
        rewardSelected = Math.min(rewardSelected, options.size() - 1);
        page = Page.REWARD;
        render();
        while (true) {
            KeyStroke key = readKey();
            if (page == Page.REWARD) {
                switch (key.getKeyType()) {
                    case ArrowLeft -> rewardSelected = Math.floorMod(rewardSelected - 1, options.size());
                    case ArrowRight -> rewardSelected = Math.floorMod(rewardSelected + 1, options.size());
                    case Enter -> {
                        if (!ItemScreens.isFull(state, options.get(rewardSelected))) {
                            return leaveReward(RewardChoice.Take.into(rewardSelected));
                        }
                        replaceSelected = 0;
                        page = Page.REPLACE;
                    }
                    case Character -> {
                        char c = Character.toLowerCase(key.getCharacter());
                        if (c == 's') {
                            return leaveReward(new RewardChoice.Skip());
                        } else if (c == 'q') {
                            confirmQuit();
                        }
                    }
                    default -> {
                    }
                }
            } else {
                int owned = options.get(rewardSelected) instanceof cardistry.items.Trinket
                        ? state.trinkets().size() : state.items().size();
                switch (key.getKeyType()) {
                    case ArrowLeft -> replaceSelected = Math.floorMod(replaceSelected - 1, owned);
                    case ArrowRight -> replaceSelected = Math.floorMod(replaceSelected + 1, owned);
                    case Enter -> {
                        return leaveReward(new RewardChoice.Take(rewardSelected, replaceSelected));
                    }
                    case Escape -> page = Page.REWARD;
                    default -> {
                    }
                }
            }
            render();
        }
    }

    @Override
    public int chooseTrinketToDiscard(GameState state) {
        this.state = state;
        Page previous = page;
        discardSelected = Math.min(discardSelected, state.trinkets().size() - 1);
        page = Page.DISCARD;
        render();
        while (true) {
            KeyStroke key = readKey();
            int count = state.trinkets().size();
            switch (key.getKeyType()) {
                case ArrowUp, ArrowLeft -> discardSelected = Math.floorMod(discardSelected - 1, count);
                case ArrowDown, ArrowRight -> discardSelected = Math.floorMod(discardSelected + 1, count);
                case Enter -> {
                    page = previous;
                    int chosen = discardSelected;
                    discardSelected = 0;
                    return chosen;
                }
                default -> {
                }
            }
            render();
        }
    }

    private RewardChoice leaveReward(RewardChoice choice) {
        page = Page.COMBAT;
        rewardSelected = 0;
        return choice;
    }

    private void showInventory() {
        page = Page.INVENTORY;
        render();
        readKey();
        page = Page.COMBAT;
        render();
    }

    @Override
    public void waitForExit() {
        readKey();
    }

    private void moveSelection(int delta, int handSize) {
        if (handSize == 0) {
            return;
        }
        view.selected = Math.floorMod(view.selected + delta, handSize);
        render();
    }

    private void clampSelection() {
        int size = state.hand().size();
        view.selected = size == 0 ? 0 : Math.min(view.selected, size - 1);
    }

    private void confirmQuit() {
        view.confirmingQuit = true;
        render();
        KeyStroke key = readKey();
        view.confirmingQuit = false;
        if (isChar(key, 'o') || isChar(key, 'y') || isChar(key, 'q')) {
            throw new QuitGameException();
        }
        render();
    }

    // ===================== Animations =====================

    private void animateEnemyHit(Enemy enemy, double amount) {
        double before = enemy.hp() + amount;
        int[] shake = {2, -2, 1, -1, 0, 0};
        for (int i = 0; i < shake.length; i++) {
            view.enemyOffsetX = shake[i];
            view.enemyFlash = i < 3 ? TextColor.ANSI.WHITE_BRIGHT : null;
            view.enemyHpShown = before - amount * (i + 1) / shake.length;
            view.floating.clear();
            view.floating.add(new ViewState.Floating("-" + num(amount), Palette.ACCENT, true, i / 2));
            frame();
        }
        view.resetEffects();
        render();
    }

    private void animatePlayerHit(double amount) {
        double before = state.player().hp() + amount;
        for (int i = 0; i < 6; i++) {
            view.playerFlash = i % 2 == 0 ? Palette.DAMAGE : null;
            view.playerHpShown = before - amount * (i + 1) / 6;
            view.floating.clear();
            view.floating.add(new ViewState.Floating("-" + num(amount), Palette.DAMAGE, false, i / 2));
            frame();
        }
        view.resetEffects();
        render();
    }

    private void animateHeal(double amount) {
        double before = state.player().hp() - amount;
        for (int i = 0; i < 6; i++) {
            view.playerFlash = i % 2 == 0 ? Palette.HEAL : null;
            view.playerHpShown = before + amount * (i + 1) / 6;
            view.floating.clear();
            view.floating.add(new ViewState.Floating("+" + num(amount), Palette.HEAL, false, i / 2));
            frame();
        }
        view.resetEffects();
        render();
    }

    private void animateTerrain() {
        for (int i = 0; i < 4; i++) {
            view.terrainFlash = i % 2 == 0 ? TextColor.ANSI.WHITE_BRIGHT : null;
            frame();
        }
        view.resetEffects();
        render();
    }

    private void animateEnemyAttack() {
        pause(250);
        for (int offset : new int[] {-2, -4, -2, 0}) {
            view.enemyOffsetX = offset;
            frame();
        }
        view.resetEffects();
    }

    private void animateEnemyDeath() {
        for (int i = 0; i < 6; i++) {
            view.enemyHidden = i % 2 == 1;
            view.enemyDim = true;
            frame();
        }
        view.resetEffects();
        view.enemyHidden = true;
        render();
        pause(300);
        view.resetEffects();
    }

    private void animateEnemyArrival() {
        for (int offset = 12; offset >= 0; offset -= 3) {
            view.enemyOffsetX = offset;
            frame();
        }
        view.resetEffects();
        render();
    }

    private void animateDraw() {
        view.selected = state.hand().size() - 1;
        for (int i = 0; i < 4; i++) {
            view.highlightSlot = i % 2 == 0 ? state.hand().size() - 1 : -1;
            frame();
        }
        view.resetEffects();
        render();
    }

    /** Dessine une image d'animation puis attend. */
    private void frame() {
        render();
        pause(FRAME_MS);
    }

    private void pause(int millis) {
        if (fast || page != Page.COMBAT) {
            return;
        }
        sleep(millis);
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ===================== Écran et clavier =====================

    private void render() {
        try {
            screen.doResizeIfNecessary();
            TerminalSize size = screen.getTerminalSize();
            screen.clear();
            Painter painter = new Painter(screen.newTextGraphics(), size);
            if (size.getColumns() < MenuScreens.MIN_COLS || size.getRows() < MenuScreens.MIN_ROWS) {
                MenuScreens.tooSmall(painter);
            } else {
                switch (page) {
                    case TITLE -> MenuScreens.title(painter, seed);
                    case RULES -> MenuScreens.rules(painter, state);
                    case COMBAT -> {
                        if (state != null) {
                            CombatScreen.draw(painter, state, view, seed);
                        }
                    }
                    case INVENTORY -> ItemScreens.inventory(painter, state);
                    case REWARD -> ItemScreens.reward(painter, state, rewardOptions, rewardSelected, seed);
                    case REPLACE -> ItemScreens.replace(painter, state, rewardOptions.get(rewardSelected),
                            replaceSelected, seed);
                    case DISCARD -> ItemScreens.discard(painter, state, discardSelected, seed);
                    case GAME_OVER -> MenuScreens.gameOver(painter, state, seed);
                }
                if (view.confirmingQuit) {
                    painter.text(1, size.getRows() - 1, "─".repeat(size.getColumns() - 2), Palette.FRAME);
                    painter.text(2, size.getRows() - 1, " Quitter la partie ? [O]ui / [N]on ", Palette.WARNING, true);
                }
            }
            screen.refresh();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Attend une touche ; redessine si le terminal change de taille. */
    private KeyStroke readKey() {
        try {
            while (true) {
                KeyStroke key = screen.pollInput();
                if (key != null) {
                    if (key.getKeyType() == KeyType.EOF) {
                        throw new QuitGameException();
                    }
                    return key;
                }
                if (screen.doResizeIfNecessary() != null) {
                    render();
                }
                sleep(POLL_MS);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean isChar(KeyStroke key, char expected) {
        return key.getKeyType() == KeyType.Character
                && Character.toLowerCase(key.getCharacter()) == expected;
    }

    private static boolean isQuit(KeyStroke key) {
        return key.getKeyType() == KeyType.Escape || isChar(key, 'q');
    }
}
