plugins {
    id("org.jetbrains.kotlin.jvm")
    id("application")
}
dependencies {
    implementation(project(":gart"))
    implementation(project(":gart-box2d"))
}
sourceSets {
    main {
        kotlin.srcDir("src")
        resources.srcDir("res")
    }
}
application {
    mainClass = "dev.oblac.gart.ppt.MainKt"
}
