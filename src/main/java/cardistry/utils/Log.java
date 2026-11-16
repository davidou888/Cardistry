package cardistry.utils;


import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Log {
    private static PrintWriter writer;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    static {
        try {
            // false = écrase le fichier à chaque démarrage du projet (plus propre pour débugger)
            // true = garde l'historique des anciens lancements
            writer = new PrintWriter(new FileWriter("debug.log", false), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void println(String message) {
        if (writer != null) {
            // Récupère automatiquement le nom de la classe et la ligne qui a appelé Log.println
            StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
            String location = caller.getFileName() + ":" + caller.getLineNumber();
            String time = LocalTime.now().format(TIME_FORMAT);

            writer.println("[" + time + "] [" + location + "] " + message);
        }
    }
}
