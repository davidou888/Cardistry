plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.googlecode.lanterna:lanterna:3.1.5")
    // Lecture des musiques OGG Vorbis (Java pur, LGPL 2+, jars séparés : voir licenses/THIRD-PARTY.md).
    runtimeOnly("com.googlecode.soundlibs:vorbisspi:1.0.3.3")
    runtimeOnly("com.googlecode.soundlibs:jorbis:0.0.17.4")
    runtimeOnly("com.googlecode.soundlibs:tritonus-share:0.3.7.4")

    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.encoding = "UTF-8"
}

application {
    mainClass = "cardistry.Main"
}

tasks.named<JavaExec>("run") {
    // Le jeu lit le clavier : on branche l'entrée standard du terminal.
    standardInput = System.`in`
}

tasks.test {
    useJUnitPlatform()
    // Rendu de la fenêtre testé dans des images, jamais à l'écran (CI sans affichage).
    systemProperty("java.awt.headless", "true")
    // Les tests ne jouent jamais de son.
    systemProperty("cardistry.audio", "off")
}
