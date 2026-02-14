plugins {
    id("org.jetbrains.kotlin.jvm") apply false
}

allprojects {
    version = "1.0.0-SNAPSHOT"
    group = "dev.oblac.gart"

    repositories {
        mavenCentral()
        maven("https://packages.jetbrains.team/maven/p/cmp/dev")
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
