plugins {
    id("org.jetbrains.kotlin.jvm")
}
dependencies {
    implementation(project(":gart"))
}
sourceSets {
    main {
        kotlin.srcDir("src")
        resources.srcDir("res")
    }
}
