plugins {
    id("org.jetbrains.kotlin.jvm") apply false
}

allprojects {
    version = "1.0.0-SNAPSHOT"
    group = "dev.oblac.gart"

    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
