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
        // classes and resources both - a module run straight off its build dirs (the sweeper from
        // gart) needs its fonts on the path too, not just its classes. wrapped in files() so the
        // configuration cache can store it
        val outputDirs = files(javaExt.sourceSets["main"].output)
        val runtimeFiles = configurations["runtimeClasspath"].incoming.files
        tasks.register("writeClasspath") {
            description = "Writes runtime classpath to build/classpath.txt"
            val outputFile = layout.buildDirectory.file("classpath.txt")
            inputs.files(runtimeFiles, outputDirs)
            outputs.file(outputFile)
            doLast {
                outputFile.get().asFile.writeText("-cp\n${outputDirs.asPath}:${runtimeFiles.asPath}")
            }
        }
        tasks.register("writeLauncherClasspath") {
            description = "Writes dependency-only classpath (no module classes) to build/launcher-cp.txt"
            val outputFile = layout.buildDirectory.file("launcher-cp.txt")
            inputs.files(runtimeFiles)
            outputs.file(outputFile)
            doLast {
                outputFile.get().asFile.writeText("-cp\n${runtimeFiles.asPath}")
            }
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
