import java.util.ArrayList;
import java.util.Random;

public abstract class Carte{
    String nom;
    String type;
    double pvChange;
    //String description;

    public Carte(String nom,String type, double pvChange){
        this.nom = nom;
        this.type  =type;
        this.pvChange = pvChange;
    }

    abstract void afficher();
    abstract void jouer(Jeu jeu);
    abstract double getMult(Jeu jeu);


    protected boolean typeRelation(String type1, String type2){
        if(((type1 == "feu")&&(type2=="nature")) || (type1 == "nature")&&(type2=="eau") || (type1 == "eau")&&(type2=="feu")){
            return true;
        }else {
            return false;
        }
    }


}




class Sortilege extends Carte{
    Helper helper = new Helper();

    public Sortilege(String nom, String type, double degat){
        super(nom, type, -1*degat);

    }

    public void afficher() {
        System.out.println(this.getClass().getSimpleName() + ", nom: "+ nom + ", type: " +  helper.printType(type) + ", dégats: "+ pvChange);
    }

    public void jouer(Jeu jeu){
        System.out.println("Tu joues " + nom + " qui fait " + (getMult(jeu)*pvChange));
        jeu.changeLife(jeu.currentAdversaire, this);
    }

    public double getMult(Jeu jeu){
        if(typeRelation(type, jeu.currentAdversaire.type)){
            return 1.5;
        } else if (jeu.currentAdversaire.type == type) {
            return 0.5;
        }else{
            return 1.0;
        }

    }

}


class Terrain extends Carte{
    Helper helper = new Helper();
    //String description;

    public Terrain(String nom, String type){
        super(nom, type, 0.0);
        //System.out.println("Vous êtes maintenant dans " + nom + ", un terrain de type " + type);
    }

    public void afficher() {
        System.out.println(this.getClass().getSimpleName() +", "+ nom + " de type " +  helper.printType(type));
    }

    public void jouer(Jeu jeu){
        System.out.println("Tu joues " + nom + " qui te téléporte de " + jeu.terrain.nom +" à "+ this.nom);
        jeu.setTerrain(this);

    }

    public double getMult(Jeu jeu) {return 1.0;}

    public double getMult(Adversaire adv){
        if(typeRelation(type, adv.type)){
            return 0.75;
        } else if (adv.type == type) {
            return 1.25;
        }else{
            return 1.0;
        }
    }


}

class Heal extends Carte{

    Helper helper = new Helper();


    public Heal(String nom, String type, double pvHeal){
        super(nom, type, pvHeal);

    }

    public void afficher() {
        System.out.println(this.getClass().getSimpleName() +", nom: "+ nom + ", type: " +  helper.printType(type) + ", heal: "+ pvChange);
    }

    public void jouer(Jeu jeu){
        System.out.println("Tu joues " + nom + " qui te redonne " + (getMult(jeu)*pvChange));
        jeu.changeLife(jeu.joueur, this);

    }

    public double getMult(Jeu jeu) {
        if(typeRelation(jeu.terrain.type, type)){
            return 0.75;
        } else if (type == jeu.terrain.type) {
            return 1.5;
        } else{ return 1.0;}
    }

}




class Deck {

    private ArrayList<Carte> cartes = new ArrayList<>();
    private Random random = new Random();

    public Deck() {
        // --- Sortilèges (nom, type, dégâts) ---
        cartes.add(new Sortilege("Boule de feu",      "feu",    10.0));
        cartes.add(new Sortilege("Pluie de flammes",  "feu",    15.0));
        cartes.add(new Sortilege("Jet d'eau",         "eau",    10.0));
        cartes.add(new Sortilege("Tsunami",           "eau",    15.0));
        cartes.add(new Sortilege("Fouet de lianes",   "nature", 10.0));
        cartes.add(new Sortilege("Tempête d'épines",  "nature", 15.0));
        cartes.add(new Sortilege("Pluie de flammes",  "feu",    15.0));
        cartes.add(new Sortilege("Tsunami",           "eau",    15.0));
        cartes.add(new Sortilege("Fouet de lianes",   "nature", 10.0));

        // --- Terrains (nom, type) ---
        cartes.add(new Terrain("EPFL",   "feu"));
        cartes.add(new Terrain("Marécage", "eau"));
        cartes.add(new Terrain("Forêt",    "nature"));
        cartes.add(new Terrain("Enfer",   "feu"));
        cartes.add(new Terrain("Antlantis", "eau"));
        cartes.add(new Terrain("Amazonie",    "nature"));

        // --- Heals (nom, type, PV rendus) ---
        cartes.add(new Heal("Potion",        "nature", 10.0));
        cartes.add(new Heal("Caïpirinha",        "eau", 10.0));
        cartes.add(new Heal("Petit pétou",        "feu", 15.0));
        cartes.add(new Heal("Grande potion", "nature", 20.0));
    }

    public Carte piocher() {
        int indice = random.nextInt(cartes.size());
        return cartes.get(indice);
    }
}