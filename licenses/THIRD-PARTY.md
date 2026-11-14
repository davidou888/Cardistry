# Composants tiers

Cardistry inclut les composants suivants. Leurs licences complètes sont dans ce dossier et dans
`src/main/resources/fonts/`.

## Polices (fenêtre du jeu)

| Composant | Version | Auteur | Licence | Source |
|---|---|---|---|---|
| Departure Mono | 1.500 | © 2022–2024 Helena Zhang | SIL Open Font License 1.1 (`src/main/resources/fonts/OFL-DepartureMono.txt`) | https://github.com/rektdeckard/departure-mono |
| JetBrains Mono | 2.304 | © 2020 The JetBrains Mono Project Authors | SIL Open Font License 1.1 (`src/main/resources/fonts/OFL-JetBrainsMono.txt`) | https://github.com/JetBrains/JetBrainsMono |

## Lecture des musiques OGG Vorbis

Bibliothèques Java non modifiées, livrées comme **jars séparés** (dans l'app installée : dossier
`lib/` ou `app/`). Elles peuvent être remplacées par une autre version compatible, comme le permet la
LGPL. Sources : Maven Central, groupe `com.googlecode.soundlibs` (jars `-sources` disponibles).

| Composant | Version | Auteurs | Licence |
|---|---|---|---|
| JOrbis | 0.0.17.4 | © 2000 ymnk, JCraft, Inc. | GNU LGPL 2 ou ultérieure (`LGPL-2.1.txt`) |
| VorbisSPI | 1.0.3.3 | JavaZOOM | GNU LGPL 2 ou ultérieure (`LGPL-2.1.txt`) |
| Tritonus-share | 0.3.7.4 | © 1999–2001 Matthias Pfisterer et contributeurs | GNU LGPL 2 ou ultérieure (`LGPL-2.1.txt`) ; deux fichiers sous licence BSD, ci-dessous |

### Avis BSD (Tritonus-share : `FloatSampleInput`, `FloatInputStream`)

```
Copyright (c) 2006 by Florian Bomers <http://www.bomers.de>
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

- Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.
- Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
OF THE POSSIBILITY OF SUCH DAMAGE.
```
