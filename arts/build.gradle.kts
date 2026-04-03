subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        "implementation"(project(":gart"))
    }

    configure<JavaPluginExtension> {
        sourceSets.named("main") {
            java.srcDir("src")
            resources.srcDir("res")
        }
    }
}
