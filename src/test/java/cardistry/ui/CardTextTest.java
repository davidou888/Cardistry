package cardistry.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cardistry.model.card.CardPool;
import org.junit.jupiter.api.Test;

/** Textes d'une carte hors combat : valeurs de base, rareté, pouvoir ou effet de terrain. */
class CardTextTest {

    @Test
    void resumesHorsCombat() {
        assertEquals("Boule de feu · sort feu · Commun : 10 dégâts · ★ 10 % d'infliger Brûlure",
                CardText.summary(CardPool.bouleDeFeu()));
        assertEquals("Pet de Mamouz · sort neutre · Commun : aucun dégât · ★ inflige Intox",
                CardText.summary(CardPool.petDeMamouz()));
        assertEquals("Moindre carré · sort algebrique · Peu commun : 1-40 dégâts",
                CardText.summary(CardPool.moindreCarre()));
        assertEquals("Professeur d'or · sort spirituel · Légendaire : 35 dégâts (non multipliés)"
                + " · ★ hypnotise l'adversaire pendant 5 tours", CardText.summary(CardPool.professeurDOr()));
        assertEquals("Dojo · terrain neutre · Commun · +10 % de dégâts", CardText.summary(CardPool.dojo()));
    }
}
