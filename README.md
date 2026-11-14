# Cardistry

Jeu de cartes roguelite au tour par tour, en Java, joué dans le terminal (interface plein écran
[Lanterna](https://github.com/mabe02/lanterna)). Tourne sous **Linux, macOS et Windows**.

## Prérequis

- **JDK 21 ou plus récent** (`java -version` pour vérifier). Gradle n'a pas besoin d'être installé :
  le wrapper (`gradlew`, `gradlew.bat`) le télécharge.
- Un terminal d'au moins **80×24** pour le plein écran.

## Lancer le jeu

| OS | Commande | Affichage |
|---|---|---|
| Linux | `./play` | plein écran dans le terminal |
| macOS | `./play` | plein écran dans le terminal (Terminal.app, iTerm2…) |
| Windows | `play.bat` | **fenêtre** dédiée (voir ci-dessous) |

Mode texte simple, partout : `./play --no-ui` (Linux, macOS) ou `play.bat --no-ui` (Windows).

> `./gradlew run` lance aussi le jeu, mais Gradle ne transmet pas un vrai terminal : le jeu passe
> alors automatiquement en mode texte. Utilisez `play` / `play.bat` pour le plein écran.

### Options

| Option | Effet |
|---|---|
| `--seed N` | rejoue la partie de seed N |
| `--fast` | sans pauses ni animations |
| `--no-ui` | mode texte simple (lignes, saisie au clavier + Entrée) |
| `--window` | jeu dans une fenêtre (par défaut sous Windows) |
| `--ascii` | affichage en ASCII pur (cadres `+-|`, barres `#`, accents retirés) |
| `--no-color` | mode texte sans couleurs ANSI |

Exemples : `./play --seed 42 --fast`, `play.bat --no-ui --no-color`.

## Particularités par OS

### Linux et macOS
- Plein écran directement dans le terminal.
- L'**ASCII pur** s'active tout seul si le terminal n'est pas en UTF-8 (ex. `LANG=C`).
- Ctrl+Z suspend le jeu comme n'importe quel programme (le shell reprend la main ; `fg` pour revenir).

### Windows
- **Lanterna 3.1 n'a pas de mode plein écran dans la console Windows** (cmd, PowerShell, Windows
  Terminal) : seule une version 3.2 *alpha* (2020) en contient un. `play.bat` ouvre donc le jeu dans
  la **fenêtre de l'émulateur de terminal de Lanterna** (Swing), en Unicode, avec toutes les touches.
  Fermer la fenêtre quitte le jeu.
- Sans affichage graphique (session distante sans bureau…), le jeu passe en **mode texte** dans la
  console.
- Mode texte dans la console (`play.bat --no-ui`) : les couleurs ne sont activées que dans
  **Windows Terminal** ou un terminal de type Unix (Git Bash) ; dans la vieille console `cmd`, elles
  sont coupées automatiquement (sinon des codes parasites s'affichent). `--no-color` les coupe partout.
- Si la console n'est pas en UTF-8 (page de code 850/437 par défaut), le mode texte passe en ASCII pur
  automatiquement. Pour les accents : `chcp 65001` avant de lancer.

## Sauvegarde

Le jeu range ses données (déblocages à venir) dans :

| OS | Dossier |
|---|---|
| Linux | `$XDG_DATA_HOME/cardistry`, sinon `~/.local/share/cardistry` |
| macOS | `~/Library/Application Support/Cardistry` |
| Windows | `%APPDATA%\Cardistry` |

## Développement

- Tests : `./gradlew test` (Windows : `gradlew.bat test`). La CI GitHub Actions les lance sur Ubuntu,
  macOS et Windows.
- Les tests d'interface écrivent chaque écran testé dans `build/screens/*.txt`.
- Contexte et conventions du projet : [`CLAUDE.md`](CLAUDE.md) ; game design :
  [`docs/GAME_DESIGN.md`](docs/GAME_DESIGN.md).
