package cardistry.core;

/** Résultat d'une action soumise au moteur. */
public enum ActionResult {
    /** Action refusée : rien n'a changé (un ActionRejected a été publié). */
    REJECTED,
    /** Action gratuite effectuée (objet) : le tour continue. */
    FREE_ACTION,
    /** Le tour est terminé (carte jouée, pioche, ou adversaire vaincu). */
    TURN_ENDED
}
