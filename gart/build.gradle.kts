plugins {
    id("org.jetbrains.kotlin.jvm")
    id("java-library")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.test {
    useJUnitPlatform()
}

val osName = System.getProperty("os.name")
val targetOs = when {
    osName == "Mac OS X" -> "macos"
    osName.startsWith("Win") -> "windows"
    osName.startsWith("Linux") -> "linux"
    else -> error("Unsupported OS: $osName")
}

val osArch = System.getProperty("os.arch")
val targetArch = when (osArch) {
    "x86_64", "amd64" -> "x64"
    "aarch64" -> "arm64"
    else -> error("Unsupported arch: $osArch")
}

val version = "0.9.43"
val target = "${targetOs}-${targetArch}"

dependencies {
    api("org.jetbrains.skiko:skiko-awt-runtime-$target:$version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    api("org.bytedeco:ffmpeg-platform:7.1.1-1.5.12")
    api("org.bytedeco:ffmpeg-platform-gpl:7.1.1-1.5.12")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.compileKotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}
