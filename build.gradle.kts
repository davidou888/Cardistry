plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.googlecode.lanterna:lanterna:3.1.5")

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
}
