package cardistry.ui.text;

import cardistry.ui.QuitGameException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

/** Terminal texte partagé par le renderer et la saisie : un seul Scanner pour toute la partie. */
public final class Console {

    private final Scanner scanner;
    private final PrintStream out;
    private final boolean fast;
    private final boolean ascii;
    private final boolean color;

    /** @param fast si {@code true}, les pauses sont ignorées */
    public Console(InputStream in, PrintStream out, boolean fast) {
        this(in, out, fast, false, true);
    }

    /**
     * @param fast si {@code true}, les pauses sont ignorées
     * @param ascii si {@code true}, tout texte est converti en ASCII pur
     * @param color si {@code false}, les codes couleur ANSI sont retirés (vieille console Windows)
     */
    public Console(InputStream in, PrintStream out, boolean fast, boolean ascii, boolean color) {
        this.scanner = new Scanner(in);
        this.out = out;
        this.fast = fast;
        this.ascii = ascii;
        this.color = color;
    }

    private static final java.util.regex.Pattern ANSI = java.util.regex.Pattern.compile("\u001B\\[[0-9;]*m");

    private String encode(String text) {
        String result = color ? text : ANSI.matcher(text).replaceAll("");
        return ascii ? cardistry.ui.Glyphs.toAscii(result) : result;
    }

    public void println(String line) {
        out.println(encode(line));
    }

    public void println() {
        out.println();
    }

    public void print(String text) {
        out.print(encode(text));
        out.flush();
    }

    /** @throws QuitGameException si l'entrée est fermée */
    public String readLine() {
        if (!scanner.hasNextLine()) {
            throw new QuitGameException();
        }
        return scanner.nextLine();
    }

    public void pause(double seconds) {
        if (fast) {
            return;
        }
        try {
            Thread.sleep((long) (seconds * 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
