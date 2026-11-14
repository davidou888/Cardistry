@echo off
rem Compile puis lance Cardistry (Windows). Par defaut : jeu dans une fenetre.
rem Options : --seed N, --fast, --no-ui (mode texte dans la console), --ascii, --no-color
cd /d "%~dp0"
call gradlew.bat -q installDist || exit /b 1
call build\install\cardistry\bin\cardistry.bat %*
