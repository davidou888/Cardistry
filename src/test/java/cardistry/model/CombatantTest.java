package cardistry.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** PV toujours entiers : arrondis à l'entier le plus proche quand ils changent. */
class CombatantTest {

    @Test
    void degatsEtSoinsArrondisALEntierLePlusProche() {
        Player player = new Player(100);
        assertEquals(6, player.takeDamage(6.25));
        assertEquals(94, player.hp());
        assertEquals(8, player.takeDamage(7.5)); // x,5 vers le haut
        assertEquals(86, player.hp());
        assertEquals(23, player.heal(22.5));
        assertEquals(109, player.hp());
        assertEquals(0, player.heal(0.4));
        assertEquals(109, player.hp());
    }

    @Test
    void pvDeDepartEtPvMaxDesAdversairesArrondis() {
        assertEquals(100, new Player(99.6).hp());
        Enemy enemy = Enemy.builder("e", "E", Element.FIRE, cardistry.model.enemy.EnemyRank.COMMON).hp(28)
                .scale(1.4, 1.0).behavior(cardistry.model.enemy.EnemyBehavior.always(
                        cardistry.model.enemy.EnemyMove.basicAttack(5))).build();
        assertEquals(39, enemy.hp());
        assertEquals(39, enemy.maxHp());
        enemy.takeDamage(10.5);
        assertEquals(28, enemy.hp());
        assertEquals(11, enemy.healSelf(20.6)); // plafonné aux PV max
        assertEquals(39, enemy.hp());
    }
}
