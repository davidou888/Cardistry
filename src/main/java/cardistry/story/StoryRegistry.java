package cardistry.story;

import java.util.List;
import java.util.Optional;

/**
 * Registre des événements. Les trois actuels sont des <b>placeholders</b> : Louis fournira les vrais
 * (références EPFL et soirées). Ajouter un événement : une entrée dans {@link #all()} ; les
 * conséquences passent par {@link StoryContext}.
 */
public final class StoryRegistry {

    /** Prix du paquet de la machine à clopes. */
    public static final int CIGARETTE_MACHINE_PRICE = 20;

    private StoryRegistry() {
    }

    public static List<Story> all() {
        return List.of(vendingMachine(), stranger(), cigaretteMachine());
    }

    public static Optional<Story> byId(String id) {
        return all().stream().filter(s -> s.id().equals(id)).findFirst();
    }

    /** Placeholder : le distributeur en panne. */
    private static Story vendingMachine() {
        return new Story("distributeur", "Distributeur en panne",
                "Un distributeur de boissons clignote dans un couloir désert. Une canette est coincée derrière la vitre.",
                List.of(
                        StoryChoice.of("Secouer la machine", "50 % : des pièces tombent (+25 Choomies), sinon −6 PV",
                                ctx -> {
                                    if (ctx.chance(0.5)) {
                                        ctx.choomies(25);
                                        ctx.say("Des pièces dégringolent : +25 Choomies.");
                                    } else {
                                        ctx.hurt(6);
                                        ctx.say("La machine bascule sur ton pied : −6 PV.");
                                    }
                                }),
                        StoryChoice.of("Passer ton chemin", "", ctx -> ctx.say("Tu laisses la canette à son sort."))));
    }

    /** Placeholder : un inconnu offre un verre. */
    private static Story stranger() {
        return new Story("inconnu", "Un inconnu généreux",
                "Quelqu'un que tu ne connais pas te tend un verre. « Allez, c'est ma tournée ! »",
                List.of(
                        StoryChoice.of("Accepter", "+15 PV, Gueule de bois au prochain combat", ctx -> {
                            ctx.heal(15);
                            ctx.hangover();
                            ctx.say("C'est fort. Ça fait du bien... pour l'instant.");
                        }),
                        StoryChoice.of("Lui demander une astuce", "+1 Boule de feu dans ton deck", ctx -> {
                            ctx.addCard(cardistry.model.card.CardPool.bouleDeFeu());
                            ctx.say("Il griffonne un sort sur une serviette : Boule de feu rejoint ton deck.");
                        }),
                        StoryChoice.of("Refuser poliment", "", ctx -> ctx.say("Il hausse les épaules et s'en va."))));
    }

    /** Placeholder : la machine à clopes. */
    private static Story cigaretteMachine() {
        return new Story("machine-a-clopes", "Machine à clopes",
                "Une vieille machine à clopes ronronne près de l'entrée. Elle accepte les Choomies.",
                List.of(
                        StoryChoice.when("Acheter un paquet", "−" + CIGARETTE_MACHINE_PRICE + " Choomies, +4 clopes",
                                s -> s.choomies() >= CIGARETTE_MACHINE_PRICE,
                                "il te faut " + CIGARETTE_MACHINE_PRICE + " Choomies", ctx -> {
                                    ctx.choomies(-CIGARETTE_MACHINE_PRICE);
                                    int added = ctx.cigarettes(4);
                                    ctx.say("Clac : +" + added + " clopes.");
                                }),
                        StoryChoice.of("Taper dessus", "30 % : +2 clopes, sinon −4 PV", ctx -> {
                            if (ctx.chance(0.3)) {
                                int added = ctx.cigarettes(2);
                                ctx.say("Deux clopes tombent ! (+" + added + ")");
                            } else {
                                ctx.hurt(4);
                                ctx.say("Aïe. La machine gagne : −4 PV.");
                            }
                        }),
                        StoryChoice.of("Partir", "", ctx -> ctx.say("Tu repars sans rien."))));
    }
}
