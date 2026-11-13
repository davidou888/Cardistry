
import java.util.Scanner;

class Magic {
	public static void main(String[] args) {

		//Ecran.setFond(Ecran.FOND_VOLCAN);
		/*
		Helper helper = new Helper();

		Jeu jeu = new Jeu();

		jeu.playIntro();

		while (!jeu.isWon() && jeu.joueur.isAlive()){
			jeu.tour++;
			jeu.afficherGameState();


			helper.pause(1);
			jeu.playerPlay();
			helper.pause(1);


		}*/
		//Ecran.reinitialiser();

	}



}


class Jeu{

	Helper helper = new Helper();
	Scanner scanner = new Scanner(System.in);

	public int tour = 0;

	Adversaire[] listeAdversaires = new Adversaire[3];


	Adversaire currentAdversaire;
	Terrain terrain = new Terrain("EPFL", "feu");

	private Deck deck = new Deck();
	Carte[] maMain = new Carte[7];

	Player joueur = new Player();



	public Jeu() {
		distribuer();
		listeAdversaires[0] = new Adversaire("Hydre", "feu", 20.0, 5.0);
		listeAdversaires[1] = new Adversaire("Sirène", "eau", 30.0, 10.0);
		listeAdversaires[2] = new Adversaire("Goblin", "nature", 40.0, 15.0);
		currentAdversaire = listeAdversaires[0];


	}





	public void distribuer() {
		for (int i = 0; i < maMain.length; i++) {
			maMain[i] = deck.piocher();
		}
	}


	public void playerPlay(){

		int choix = -1;
		while (!checkInputPlay(choix)){
			Scanner scanner = new Scanner(System.in);

			System.out.print("Que veux-tu jouer ? ");
			choix = scanner.nextInt();
		};
		//ADD CHECK IF INPUT IS OK




		if(choix == 0){
			getNewCard();
		}else{
			maMain[choix - 1].jouer(this);
			maMain[choix - 1] = null;
			reStackHand(choix-1);

			helper.pause(1);
		}

		if(checkAdversaryAndReplace()){
			currentAdversaire.attackPlayer(joueur, terrain.getMult(currentAdversaire));
			helper.pause(1);
		}



	}


	private void reStackHand(int indexPlayed){

		for(int i = indexPlayed; i < maMain.length-1; i++ ){
			maMain[i] = maMain[i+1];
			maMain[i+1] = null;
		}


	}

	public void setTerrain(Terrain newTerrain){

		this.terrain = newTerrain;

	}

	public void changeLife(Personnage perso, Carte carte){
		double updatePV = carte.pvChange*carte.getMult(this);
		perso.updateLife(updatePV);
	}




	public boolean isWon(){

		for(Adversaire adv : listeAdversaires){
			if(adv != null && adv.isAlive()){return false;}
		}
		System.out.println("WINNNNNNNEEERRRRRRR");
		return true;
	}


	public void afficherGameState(){
		System.out.println("");
		System.out.println("===================");
		System.out.println("PLAYER:");
		System.out.println(joueur);
		helper.pause(1);
		System.out.println("");
		System.out.println("ADVERSARY:");
		System.out.println(currentAdversaire);
		System.out.println("Multiplicateur de l'adversaire: " + terrain.getMult(currentAdversaire));
		helper.pause(0.7);
		System.out.println("");
		System.out.println("TERRAIN ACTUEL:");
		terrain.afficher();
		System.out.println("===================");
		helper.pause(0.5);
		/*System.out.println("(Appuie sur Entrée)");
		scanner.nextLine();*/

		System.out.println("");
		System.out.println("");


		System.out.println("===================");
		System.out.println("VOS CARTES:");
		afficherCartes();
		System.out.println("");
		System.out.println("[0] GET A NEW CARD");
		System.out.println("Vous avez "+ numberOfCardsInHand() + "/7 cartes" );
		System.out.println("===================");
		System.out.println("");




	}

	public void playIntro(){
		Helper helper = new Helper();

		System.out.println("Welcome to GAME NAME");
		System.out.println("");

		System.out.println("(Appuie sur Entrée pour continuer or s to skip)");

		String skip = scanner.nextLine();
		if(!skip.equals("s")){
			System.out.println("Il y 3 ennemis");
			helper.pause(0.7);
			System.out.println("Le "+helper.printYellow("Dragon")+" de type " + helper.printType("feu"));
			helper.pause(0.7);
			System.out.println("La "+helper.printYellow("Sirène")+" de type "+ helper.printType("eau"));
			helper.pause(0.7);
			System.out.println("et le "+helper.printYellow("Goblin")+" de type "+ helper.printType("nature"));
			helper.pause(0.7);

			System.out.println("(Appuie sur Entrée pour continuer)");

			scanner.nextLine();

			System.out.println("à chaque tour");
			helper.pause(0.7);
			System.out.println("Vous pouvez jouer une carte ou piocher");
			helper.pause(0.7);
			System.out.println("Une carte peut être un "+helper.printYellow("sortilège")+" qui fait des dégats à la créature");
			helper.pause(0.9);
			System.out.println("Un "+helper.printYellow("heal")+" qui vous rends des PV");
			helper.pause(0.8);
			System.out.println("Ou un "+helper.printYellow("terrain")+" qui vous téléporte à un autre endroit");
			helper.pause(0.7);
			System.out.println("(Appuie sur Entrée pour continuer)");

			scanner.nextLine();

			System.out.println("Il y a des "+ helper.printYellow("élélments")+" !");
			helper.pause(0.7);
			System.out.println(helper.printType("Eau") +" bat " + helper.printType("Feu"));
			helper.pause(0.7);
			System.out.println(helper.printType("Feu") +" bat " + helper.printType("Nature"));
			helper.pause(0.7);
			System.out.println(helper.printType("Nature") +" bat " + helper.printType("Eau"));
			helper.pause(1);
			System.out.println("Un élément contraire donne un malus d'attaque à la créature");
			helper.pause(0.8);
			System.out.println("Et vous donne un bonus contre une créature");
			helper.pause(0.8);
			System.out.println("Les heals sont plus éfficaces dans leurs terrains et moins dans le mauvais");
			helper.pause(0.8);
			System.out.println("(Appuie sur Entrée pour comemncer la partie)");

			scanner.nextLine();


		}

	}

	private void afficherCartes(){
		int count = 0;
		for(Carte carte: maMain){
			++count;
			if(carte == null){
				System.out.println("["+ count+"] " +"NO CARDS");
			}else {
				System.out.print("["+ count+"] ");
				carte.afficher();
			}

		}
	}

	private int numberOfCardsInHand(){
		int count = 0;
		for(Carte carte: maMain){
			if(carte != null){
				count++;
			}

		}
		return count;
	}


	private void getNewCard(){


		int i = 0;
		while (maMain[i] != null){
			i++;
		}
		if(i<= 6){
			Carte  newCard = deck.piocher();
			maMain[i] = newCard;
			System.out.print("Tu as reçu : ");
			newCard.afficher();
		}
		else{System.out.println("NO SPACE AVAILABLE");}


	}

	private boolean checkAdversaryAndReplace(){
		int i = 0;
		while (i < listeAdversaires.length && listeAdversaires[i] == null){
			i++;
		}

		if(!listeAdversaires[i].isAlive() && i != listeAdversaires.length - 1){
			System.out.println("change adv");
			replaceAdversary(i);
			return true;
		} else if (!listeAdversaires[i].isAlive() && i == listeAdversaires.length - 1) {
			return false;
		}else{
			return true;
		}

	}

	private void replaceAdversary(int i){
		listeAdversaires[i] = null;

		System.out.println("NEW CHALLENGER");

		currentAdversaire = listeAdversaires[i+1];
		System.out.println(currentAdversaire);
	}

	private boolean checkInputPlay(int input){

		if((input > 7 || input < 1) && (input != 0)){
			System.out.println("Veuillez choisir un nombre entre 0 et 7");
			return false;
		} else if (input == 0) {
			if(numberOfCardsInHand() == 7){
				System.out.println("Votre main est pleine");
				return false;
			}else {return true;}
		}else if(maMain[input-1] == null){
			System.out.println("Veuillez choisir un slot qui contient une carte");
			return false;
		}
		return true;
	}

}










// Efface l'écran (avec le fond actif) et remet le curseur en haut



//le joueur à des pv, une liste de cartes
// les cartes sont: des sortilèges, des terrains ou des heals
// 		les sortilèges affectent les adversaires : nom (String), pt de dégats (double), type (String)
//		les terrains donnent des bonus ou des malus aux adversaires: nom str, type str
//		les heals redonnent de la vie au joueur dependant du terrain: nom, type

//il y une liste d'adversaires, chaque adversaire à un nom, un type (feu, eau, nature)
//		methods: attaquer
//				 mourrir
//

// =====================================================
//  TYPES : FEU, EAU, NATURE
// =====================================================
//
//  TABLE "BAT" (qui bat qui)
//  -------------------------
//    FEU    bat  NATURE   (le feu brûle les plantes)
//    NATURE bat  EAU      (les plantes absorbent l'eau)
//    EAU    bat  FEU      (l'eau éteint le feu)
//
//         FEU
//        ↗    ↘
//     EAU  ←  NATURE
//
//
//  TABLE DES DÉGÂTS (multiplicateur sort → adversaire)
//  ---------------------------------------------------
//
//   Sort \ Adversaire |  FEU  |  EAU  | NATURE
//  -------------------+-------+-------+--------
//   FEU               |  x1   | x0.5  |  x1.5
//   EAU               | x1.5  |  x1   |  x0.5
//   NATURE            | x0.5  | x1.5  |  x1
//
//   x1.5 = bonus  (le sort bat le type de l'adversaire)
//   x0.5 = malus  (le type de l'adversaire bat le sort)
//   x1   = neutre (même type)
//



//  TABLE DES TERRAINS (effet sur les dégâts reçus par l'adversaire)
//  ----------------------------------------------------------------
//
//   Terrain \ Adversaire |  FEU   |  EAU   | NATURE
//  ----------------------+--------+--------+--------
//   FEU                  | x0.75  |  x1    | x1.25
//   EAU                  | x1.25  | x0.75  |  x1
//   NATURE               |  x1    | x1.25  | x0.75
//
//   x0.75 = adversaire protégé  (même type que le terrain)
//   x1.25 = adversaire affaibli (le terrain bat son type)
//   x1    = aucun effet

// =====================================================