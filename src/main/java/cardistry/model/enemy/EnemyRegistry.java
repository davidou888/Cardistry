package cardistry.model.enemy;

import static cardistry.model.Element.FIRE;
import static cardistry.model.Element.NATURE;
import static cardistry.model.Element.NEUTRAL;
import static cardistry.model.Element.WATER;

import cardistry.core.Rng;
import cardistry.model.Element;
import cardistry.model.Enemy;
import cardistry.model.enemy.MoveEffect.Hit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registre de tous les adversaires : PV et attaques au niveau 1, multipliés par niveau
 * ({@link #HP_SCALE}, {@link #DAMAGE_SCALE}). Ajouter un adversaire : une entrée dans
 * {@link #definitions()} (builder de {@link Enemy}, comportement de {@link EnemyBehavior}) et son
 * sprite dans {@code src/main/resources/sprites/<id>.txt}.
 */
public final class EnemyRegistry {

    /** Multiplicateur des PV par niveau (index 0 = niveau 1). Les boss ont des PV fixés. */
    public static final double[] HP_SCALE = {1.0, 1.4, 1.9};
    /** Multiplicateur des dégâts par niveau (boss compris). */
    public static final double[] DAMAGE_SCALE = {1.0, 1.25, 1.5};
    /** Chance qu'un combat commun soit contre Clarence. */
    public static final double CLARENCE_CHANCE = 0.03;
    public static final int LEVELS = 3;

    public static final String CADDIE = "caddie-enerve";
    public static final String CHOOMAH = "choomah";
    public static final String CHOOMAH_PRESSE = "choomah-presse";
    public static final String CHOOMAH_COSTAUD = "choomah-costaud";
    public static final String CAME = "came";
    public static final String PARESSEUX = "paresseux";
    public static final String CLARENCE = "clarence";
    public static final String SEAU = "seau-de-fuite";
    public static final String VAFI = "vafi";
    public static final String PENEU = "le-peneu";
    public static final String TREX = "t-rex-defonce";
    public static final String ETUDIANT = "etudiant-post-examen";

    /** Communs tirés pour un combat (Clarence à part) ; les Choomah comptent pour un. */
    public static final List<String> COMMONS = List.of(CADDIE, CHOOMAH, CAME, PARESSEUX, SEAU);
    public static final List<String> CHOOMAH_VARIANTS = List.of(CHOOMAH, CHOOMAH_PRESSE, CHOOMAH_COSTAUD);
    public static final List<String> ELITES = List.of(VAFI, PENEU, TREX, ETUDIANT);

    private interface Definition {
        Enemy.Builder create();
    }

    private static final Map<String, Definition> DEFINITIONS = definitions();

    private EnemyRegistry() {
    }

    /** Identifiants de tous les adversaires (boss compris), dans l'ordre du registre. */
    public static List<String> ids() {
        return List.copyOf(DEFINITIONS.keySet());
    }

    /** Nouvelle instance de l'adversaire {@code id} au niveau {@code level} (1 à 3). */
    public static Enemy create(String id, int level) {
        Definition definition = DEFINITIONS.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Adversaire inconnu : " + id);
        }
        int index = Math.max(0, Math.min(LEVELS, level) - 1);
        Enemy.Builder builder = definition.create();
        boolean boss = id.startsWith("boss-");
        return builder.scale(boss ? 1.0 : HP_SCALE[index], DAMAGE_SCALE[index]).build();
    }

    /** Identifiant du boss du niveau {@code level}. */
    public static String bossId(int level) {
        return "boss-" + level;
    }

    /**
     * Adversaire d'un combat commun : Clarence ({@link #CLARENCE_CHANCE}), sinon un commun au hasard
     * différent de {@code previousId} (une variante au hasard pour les Choomah).
     */
    public static String pickCommon(Rng rng, String previousId) {
        if (rng.nextDouble() < CLARENCE_CHANCE) {
            return CLARENCE;
        }
        String previous = CHOOMAH_VARIANTS.contains(previousId) ? CHOOMAH : previousId;
        List<String> choices = COMMONS.stream().filter(id -> !id.equals(previous)).toList();
        String id = rng.pick(choices);
        return id.equals(CHOOMAH) ? rng.pick(CHOOMAH_VARIANTS) : id;
    }

    /** Élite au hasard, différente de {@code previousId}. */
    public static String pickElite(Rng rng, String previousId) {
        return rng.pick(ELITES.stream().filter(id -> !id.equals(previousId)).toList());
    }

    // ===================== Contenu =====================

    private static Map<String, Definition> definitions() {
        Map<String, Definition> all = new LinkedHashMap<>();
        common(all, CADDIE, "Caddie énervé", NEUTRAL, 28, caddie());
        choomah(all, CHOOMAH, "Choomah", 22, 4, 3);
        choomah(all, CHOOMAH_PRESSE, "Choomah pressé", 18, 3, 2);
        choomah(all, CHOOMAH_COSTAUD, "Choomah costaud", 30, 6, 3);
        common(all, CAME, "Camé", FIRE, 25, came());
        common(all, PARESSEUX, "Paresseux", NATURE, 24, paresseux());
        all.put(CLARENCE, () -> Enemy.builder(CLARENCE, "Clarence", NEUTRAL, EnemyRank.COMMON).hp(1).choomies(75)
                .behavior(EnemyBehavior.always(EnemyMove.of("regard", "Regard", "Clarence te regarde."))));
        common(all, SEAU, "Seau de fuite", WATER, 26, seau());
        elite(all, VAFI, "Vafi", FIRE, 55, vafi(), 0);
        elite(all, PENEU, "Le Peneu", NEUTRAL, 90, peneu(), 0.2);
        elite(all, TREX, "T-Rex défoncé", NATURE, 65, EnemyBehavior.always(EnemyMove.of("morsure", "Morsure",
                "Bras trop courts : 30 % de rater ; quand il rate, fringale (il se soigne)",
                new Hit(13, 1, 0, 0.30, 8))), 0);
        elite(all, ETUDIANT, "Étudiant post-examen", WATER, 60, etudiant(), 0);
        boss(all, 1, FIRE, 130, 10, 22);
        boss(all, 2, WATER, 200, 10, 22);
        boss(all, 3, NATURE, 280, 10, 22);
        return all;
    }

    private static void common(Map<String, Definition> all, String id, String name, Element element, double hp,
            EnemyBehavior behavior) {
        all.put(id, () -> Enemy.builder(id, name, element, EnemyRank.COMMON).hp(hp).behavior(behavior));
    }

    private static void elite(Map<String, Definition> all, String id, String name, Element element, double hp,
            EnemyBehavior behavior, double reflect) {
        all.put(id, () -> Enemy.builder(id, name, element, EnemyRank.ELITE).hp(hp).behavior(behavior).reflect(reflect));
    }

    /** Boss provisoire (nom à venir) : une attaque, et une grosse attaque annoncée toutes les 4. */
    private static void boss(Map<String, Definition> all, int level, Element element, double hp, double hit,
            double heavy) {
        String id = bossId(level);
        EnemyMove normal = EnemyMove.of("coup", "Coup", "Coup", Hit.of(hit));
        EnemyMove big = EnemyMove.of("grosse-attaque", "Grosse attaque", "Grosse attaque, annoncée", Hit.of(heavy));
        all.put(id, () -> Enemy.builder(id, "Boss du niveau " + level, element, EnemyRank.BOSS).hp(hp)
                .sprite("boss").behavior(EnemyBehavior.every(4, big, normal)));
    }

    private static EnemyBehavior caddie() {
        EnemyMove wheel = EnemyMove.of("roue-voilee", "Roue voilée", "Roue voilée", Hit.of(6));
        EnemyMove charge = EnemyMove.of("charge-de-parking", "Charge de parking",
                "Tous les 3 tours : dégâts ×2", Hit.of(12));
        return EnemyBehavior.every(3, charge, wheel);
    }

    /**
     * Choomah : tout ce qu'il dit est un cri. Coup, et tous les {@code period} tours « Vol à la
     * tire » (5 Choomies, sinon 1 clope, sinon un coup).
     */
    private static void choomah(Map<String, Definition> all, String id, String name, double hp, double hit,
            int period) {
        EnemyMove strike = EnemyMove.of("coup", "ooOOOaAAAOaAOOA", "OAUUAOOAOAUAOOOooooooO", Hit.of(hit));
        EnemyMove steal = EnemyMove.of("vol-a-la-tire", "AAAOOAaAAOAOaAOOA", "oOOOOAOOAOAOAOAOOoOOOOOAAAAAAAA",
                new MoveEffect.Pickpocket(5, 1, hit));
        all.put(id, () -> Enemy.builder(id, name, NATURE, EnemyRank.COMMON).hp(hp).screams().sprite(CHOOMAH)
                .behavior(EnemyBehavior.every(period, steal, strike)));
    }

    private static EnemyBehavior came() {
        EnemyMove hit = EnemyMove.of("coup", "Coup de coude", "Coup de coude", Hit.of(5));
        EnemyMove tax = EnemyMove.of("taxe", "Taxe", "Te prend 1 clope ; si tu n'en as pas, attaque deux fois",
                new MoveEffect.Tax(1, 5, 2));
        return EnemyBehavior.every(2, tax, hit);
    }

    /** Accroché à sa branche : peu de dégâts, et il se balance pour esquiver. */
    private static EnemyBehavior paresseux() {
        EnemyMove claw = EnemyMove.of("griffe", "Griffe molle", "Griffe molle", Hit.of(3));
        EnemyMove swing = EnemyMove.of("badoing", "Badoing, badoing !",
                "Il se balance à sa branche : 50 % d'esquiver ton prochain sort, sinon deux petits coups",
                new MoveEffect.Gamble(0.5, new MoveEffect.Dodge(), Hit.of(2, 2)));
        return EnemyBehavior.every(2, swing, claw);
    }

    /** Le seau se remplit : 2, 3, 4, 5, puis « Débordement » et retour à zéro. */
    private static EnemyBehavior seau() {
        EnemyMove overflow = EnemyMove.of("debordement", "Débordement", "Le seau déborde !", Hit.of(14));
        return self -> {
            int step = self.actions() % 5;
            if (step == 4) {
                return overflow;
            }
            return EnemyMove.of("ca-goutte", "Ça goutte", "Le seau se remplit : +1 dégât par tour",
                    Hit.of(2 + step));
        };
    }

    private static EnemyBehavior vafi() {
        EnemyMove hit = EnemyMove.of("correction", "Correction", "Correction", Hit.of(8));
        EnemyMove series = EnemyMove.of("serie-d-algebre", "Série d'algèbre",
                "Une carte de ta main devient « Exercice 3b » (injouable pendant ce combat)",
                new MoveEffect.CurseCard("Exercice 3b"));
        EnemyMove pause = EnemyMove.of("pause-petou", "Pause pétou", "Se soigne et n'attaque pas",
                new MoveEffect.HealSelf(12));
        return self -> {
            if (self.hpRatio() < 0.5 && self.uses(pause.id()) < 2) {
                return pause;
            }
            return (self.actions() + 1) % 3 == 0 ? series : hit;
        };
    }

    /** Rebond (passif) et « Crevaison », qui monte de 2 à chaque utilisation. */
    private static EnemyBehavior peneu() {
        return self -> EnemyMove.of("crevaison", "Crevaison",
                "Monte à chaque utilisation. Rebond : te renvoie 20 % des dégâts reçus",
                Hit.of(6 + 2 * self.uses("crevaison")));
    }

    private static EnemyBehavior etudiant() {
        EnemyMove tears = EnemyMove.of("larmes", "Larmes", "Plus fortes si ta main est presque vide",
                new Hit(6, 1, 3, 0, 0));
        EnemyMove crisis = EnemyMove.of("crise-de-nerfs", "Crise de nerfs", "Deux attaques par tour",
                new Hit(6, 2, 3, 0, 0));
        return self -> self.hpRatio() < 0.5 ? crisis : tears;
    }
}
