plugins {
    id("org.jetbrains.kotlin.jvm") apply false
}

allprojects {
    version = "1.0.0-SNAPSHOT"
    group = "dev.oblac.gart"

    repositories {
        mavenCentral()
        maven("https://redirector.kotlinlang.org/maven/compose-dev")
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-parameters")
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
