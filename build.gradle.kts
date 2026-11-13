plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.encoding = "UTF-8"
}

application {
    mainClass = "Magic"
}

tasks.named<JavaExec>("run") {
    // Le jeu lit le clavier : on branche l'entrée standard du terminal.
    standardInput = System.`in`
}

tasks.test {
    useJUnitPlatform()
}
