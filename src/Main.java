public class Main {


    public static void main(String[] args) {

        //Ecran.setFond(Ecran.FOND_VOLCAN);

        Helper helper = new Helper();

        Jeu jeu = new Jeu();

        jeu.playIntro();

        while (!jeu.isWon() && jeu.joueur.isAlive()){
            jeu.tour++;
            jeu.afficherGameState();


            helper.pause(1);
            jeu.playerPlay();
            helper.pause(1);


        }
        //Ecran.reinitialiser();

    }


}
