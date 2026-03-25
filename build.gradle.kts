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

    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion = JavaLanguageVersion.of(21)
            }
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-parameters")
        }
    }

    plugins.withId("application") {
        tasks.named("distTar") { enabled = false }
        tasks.named("distZip") { enabled = false }
    }

    plugins.withType<JavaPlugin> {
        val javaExt = extensions.getByType<JavaPluginExtension>()
        val classesDirs = javaExt.sourceSets["main"].output.classesDirs
        val runtimeFiles = configurations["runtimeClasspath"].incoming.files
        tasks.register("writeClasspath") {
            description = "Writes runtime classpath to build/classpath.txt"
            val outputFile = layout.buildDirectory.file("classpath.txt")
            inputs.files(runtimeFiles, classesDirs)
            outputs.file(outputFile)
            doLast {
                outputFile.get().asFile.writeText("-cp\n${classesDirs.asPath}:${runtimeFiles.asPath}")
            }
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
