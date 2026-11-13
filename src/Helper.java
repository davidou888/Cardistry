public class Helper{

    public static final String RESET = "\u001B[0m";
    public static final String FIN_COULEUR = "\u001B[39m";
    public static final String ROUGE = "\u001B[31m";
    public static final String VERT  = "\u001B[32m";
    public static final String JAUNE = "\u001B[33m";
    public static final String BLEU  = "\u001B[34m";


    public String printType(String type){
        if(type.equalsIgnoreCase("feu")){
            return ROUGE + type+ FIN_COULEUR;
        } else if (type.equalsIgnoreCase("eau")){
            return BLEU + type+ FIN_COULEUR;
        } else if (type.equalsIgnoreCase("nature")){
            return VERT + type+ FIN_COULEUR;
        }else{return type;}
    }

    public String printYellow(String str){
        return JAUNE + str + FIN_COULEUR;
    }

    public void pause(double t){
        try {
            Thread.sleep((int)(t*1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}




class Ecran {

    // Quelques fonds prêts à l'emploi (couleurs RGB)
    public static final int[] FOND_VOLCAN   = {80, 20, 10};
    public static final int[] FOND_MARECAGE = {10, 40, 80};
    public static final int[] FOND_FORET    = {15, 60, 25};

    // Change le fond de tout l'écran
    public static void setFond(int r, int g, int b) {
        System.out.print("\u001B[48;2;" + r + ";" + g + ";" + b + "m"); // fond RGB
        System.out.print("\u001B[97m");  // texte blanc pour rester lisible
        effacer();
    }

    public static void setFond(int[] rgb) {
        setFond(rgb[0], rgb[1], rgb[2]);
    }

    // Efface l'écran (avec le fond actif) et remet le curseur en haut
    public static void effacer() {
        System.out.print("\u001B[2J");
        System.out.print("\u001B[H");
        System.out.flush();
    }

    // Remet le terminal dans son état normal : à appeler avant de quitter
    public static void reinitialiser() {
        System.out.print("\u001B[0m");
        effacer();
    }
}
