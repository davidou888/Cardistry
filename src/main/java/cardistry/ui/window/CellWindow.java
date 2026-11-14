package cardistry.ui.window;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import com.googlecode.lanterna.terminal.virtual.VirtualTerminalListener;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Fenêtre du jeu : le jeu dessine dans un terminal virtuel Lanterna (comme en plein écran
 * terminal), et cette fenêtre en dessine elle-même les cellules avec la police embarquée.
 * <ul>
 *   <li>double buffer : à chaque {@code flush} du terminal, la grille est dessinée dans une image
 *       hors écran, puis affichée d'un coup (aucun scintillement) ;</li>
 *   <li>images alignées sur la grille ({@link ImageLayer}) et filtre CRT (F10, {@code --crt}) ;</li>
 *   <li>redimensionnable (80×24 cellules au minimum), Ctrl + / Ctrl − pour la taille de police
 *       (multiples de la grille de la police pixel), F11 pour agrandir ;</li>
 *   <li>fermer la fenêtre (ou Cmd+Q) envoie « fin de saisie » au jeu, qui quitte proprement.</li>
 * </ul>
 */
public final class CellWindow implements AutoCloseable {

    public static final int DEFAULT_COLUMNS = 100;
    public static final int DEFAULT_ROWS = 32;
    public static final int MIN_COLUMNS = 80;
    public static final int MIN_ROWS = 24;
    public static final int DEFAULT_SCALE = 2;
    public static final int MAX_SCALE = 4;

    private final DefaultVirtualTerminal terminal;
    private final ImageLayer images = new ImageLayer();
    private final JFrame frame;
    private final Canvas canvas = new Canvas();
    private volatile GridRenderer renderer;
    private volatile BufferedImage front;
    private volatile boolean crt;
    private int scale;

    /** Ouvre la fenêtre (à appeler hors de l'EDT). */
    public static CellWindow open(String title, boolean crt) {
        CellWindow[] window = new CellWindow[1];
        try {
            SwingUtilities.invokeAndWait(() -> window[0] = new CellWindow(title, crt));
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw new IllegalStateException("Fenêtre impossible à ouvrir", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return window[0];
    }

    private CellWindow(String title, boolean crt) {
        this.crt = crt;
        this.scale = fittingScale();
        this.renderer = new GridRenderer(new CellFonts(scale));
        this.terminal = new DefaultVirtualTerminal(new TerminalSize(DEFAULT_COLUMNS, DEFAULT_ROWS));
        terminal.addVirtualTerminalListener(new VirtualTerminalListener() {
            @Override
            public void onFlush() {
                redraw();
            }

            @Override
            public void onBell() {
            }

            @Override
            public void onClose() {
            }

            @Override
            public void onResized(Terminal t, TerminalSize size) {
            }
        });
        frame = new JFrame(title);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                requestQuit();
            }
        });
        installQuitHandler();
        frame.setFocusTraversalKeysEnabled(false);
        canvas.setFocusTraversalKeysEnabled(false);
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                onKeyPressed(e);
            }

            @Override
            public void keyTyped(KeyEvent e) {
                KeyStroke key = KeyMapper.typed(e);
                if (key != null) {
                    terminal.addInput(key);
                }
            }
        });
        frame.setIconImages(AppIcon.images());
        canvas.setPreferredSize(pixelSize(DEFAULT_COLUMNS, DEFAULT_ROWS));
        frame.add(canvas);
        frame.pack();
        updateMinimumSize();
        frame.setLocationRelativeTo(null);
        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                fitGridToWindow();
            }
        });
        frame.setVisible(true);
        frame.requestFocus();
    }

    /** Terminal dans lequel le jeu dessine (à donner à {@code LanternaUi}). */
    public Terminal terminal() {
        return terminal;
    }

    /** Calque d'images, rempli par l'interface à chaque écran. */
    public ImageLayer images() {
        return images;
    }

    public boolean crt() {
        return crt;
    }

    // --- Dessin ---

    /** Appelé à chaque flush du terminal (fil du jeu) : nouvelle image hors écran, puis affichage. */
    private void redraw() {
        front = renderer.render(terminal, images.snapshot(), crt);
        canvas.repaint();
    }

    private final class Canvas extends JComponent {

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(CellPalette.BACKGROUND);
            g.fillRect(0, 0, getWidth(), getHeight());
            BufferedImage image = front;
            if (image != null) {
                int x = Math.max(0, (getWidth() - image.getWidth()) / 2);
                int y = Math.max(0, (getHeight() - image.getHeight()) / 2);
                g.drawImage(image, x, y, null);
            }
        }
    }

    // --- Taille ---

    private Dimension pixelSize(int cols, int rows) {
        return new Dimension(cols * renderer.cellWidth(), rows * renderer.cellHeight());
    }

    /** La grille suit la taille de la fenêtre (jamais sous 80×24). */
    private void fitGridToWindow() {
        int cols = Math.max(MIN_COLUMNS, canvas.getWidth() / renderer.cellWidth());
        int rows = Math.max(MIN_ROWS, canvas.getHeight() / renderer.cellHeight());
        TerminalSize size = new TerminalSize(cols, rows);
        if (!size.equals(terminal.getTerminalSize())) {
            terminal.setTerminalSize(size); // le jeu redessine au prochain passage (doResizeIfNecessary)
        }
    }

    private void updateMinimumSize() {
        Dimension min = pixelSize(MIN_COLUMNS, MIN_ROWS);
        java.awt.Insets insets = frame.getInsets();
        frame.setMinimumSize(new Dimension(min.width + insets.left + insets.right,
                min.height + insets.top + insets.bottom));
    }

    /** Plus grand multiple de la police qui fait tenir 100×32 cellules à l'écran (au moins 1). */
    private static int fittingScale() {
        if (GraphicsEnvironment.isHeadless()) {
            return DEFAULT_SCALE;
        }
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        GridRenderer one = new GridRenderer(new CellFonts(1));
        for (int s = DEFAULT_SCALE; s > 1; s--) {
            if (DEFAULT_COLUMNS * one.cellWidth() * s <= screen.width * 0.95
                    && DEFAULT_ROWS * one.cellHeight() * s <= screen.height * 0.9) {
                return s;
            }
        }
        return 1;
    }

    private void setScale(int newScale) {
        int clamped = Math.max(1, Math.min(MAX_SCALE, newScale));
        if (clamped == scale) {
            return;
        }
        scale = clamped;
        renderer = new GridRenderer(new CellFonts(scale));
        TerminalSize size = terminal.getTerminalSize();
        canvas.setPreferredSize(pixelSize(size.getColumns(), size.getRows()));
        updateMinimumSize();
        if ((frame.getExtendedState() & Frame.MAXIMIZED_BOTH) == 0) {
            frame.pack();
        }
        fitGridToWindow();
        redraw();
    }

    // --- Clavier ---

    private void onKeyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_F10) {
            crt = !crt;
            redraw();
            return;
        }
        if (code == KeyEvent.VK_F11) {
            frame.setExtendedState(frame.getExtendedState() ^ Frame.MAXIMIZED_BOTH);
            return;
        }
        boolean zoomKey = e.isControlDown() || e.isMetaDown();
        if (zoomKey && (code == KeyEvent.VK_EQUALS || code == KeyEvent.VK_PLUS || code == KeyEvent.VK_ADD)) {
            setScale(scale + 1);
            return;
        }
        if (zoomKey && (code == KeyEvent.VK_MINUS || code == KeyEvent.VK_SUBTRACT)) {
            setScale(scale - 1);
            return;
        }
        KeyStroke key = KeyMapper.pressed(e);
        if (key != null) {
            terminal.addInput(key);
        }
    }

    // --- Fermeture ---

    /** Le jeu reçoit « fin de saisie » et quitte proprement (écran restauré, sauvegarde faite). */
    private void requestQuit() {
        terminal.addInput(new KeyStroke(KeyType.EOF));
    }

    /** Cmd+Q sous macOS : même chose que fermer la fenêtre. */
    private void installQuitHandler() {
        try {
            if (java.awt.Desktop.isDesktopSupported()
                    && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.APP_QUIT_HANDLER)) {
                java.awt.Desktop.getDesktop().setQuitHandler((event, response) -> {
                    requestQuit();
                    response.cancelQuit(); // le jeu quitte lui-même
                });
            }
        } catch (UnsupportedOperationException | SecurityException e) {
            // Pas de gestionnaire : la fermeture de la fenêtre suffit.
        }
    }

    @Override
    public void close() {
        SwingUtilities.invokeLater(frame::dispose);
    }
}
