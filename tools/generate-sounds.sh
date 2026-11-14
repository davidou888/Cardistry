#!/usr/bin/env sh
# Régénère les sons PLACEHOLDER (effets WAV + musiques OGG). Demande Java et ffmpeg (avec libvorbis).
set -e
cd "$(dirname "$0")/.."
java tools/GenerateSounds.java
mkdir -p src/main/resources/audio/music
for wav in build/placeholder-music/*.wav; do
    name=$(basename "$wav" .wav)
    ffmpeg -loglevel error -y -i "$wav" -c:a libvorbis -q:a 3 "src/main/resources/audio/music/$name.ogg"
done
echo "Musiques : src/main/resources/audio/music/*.ogg"
