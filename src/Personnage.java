public class Personnage{
    protected Helper helper = new Helper();
    double pv;


    public Personnage(double pv){
        this.pv = pv;
    }

    public void updateLife(double updatePV){
        pv += updatePV;

    }

    public boolean isAlive(){
        return pv>0;
    }
}


class Player extends Personnage{



    public Player(){
        super(100.0);
    }

    public String toString(){
        return "Vous avez " + pv + " points de vie";
    }
}



class Adversaire extends Personnage{

    String nom;
    String type;
    double attack;

    public Adversaire( String nom, String type, double pv, double attack){
        super(pv);
        this.nom = nom;
        this.type = type;
        this.attack = attack;
    }

    public void attackPlayer(Player joueur, double multiplicateur){
        System.out.println(nom + " t'attaque et fait " + multiplicateur +"*"+ attack + " dégats");

        joueur.updateLife(-1*attack*multiplicateur);

    }


    public String toString() {
        return "Adversaire actuel: " + nom + " de type " + helper.printType(type) + ", il lui reste " + pv + " points de vie";
    }
}
