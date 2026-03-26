_default:
  @just --list

# Cleans the project.
clean:
    ./gradlew clean

# Builds the project.
build:
    ./gradlew build

# Runs the tests.
test:
    ./gradlew test

# Runs the hot reload for Kotlin files.
hotreload:
    ./gradlew compileKotlin --continuous -Dorg.gradle.continuous.quietperiod=100

# Make NEW thumbnails (restores git state!).
thumbs:
    fd -g '*.png' arts/* -x magick {} -thumbnail 240x240 -unsharp 0x0.75+0.75+0.008 {.}_thumb.png
    rm arts/z/etc/*_thumb.png
    git restore .

# Generates README file.
readme:
    ./gradlew :example:run

# Dev session: continuous compile + auto-restart JVM on class changes.
# Usage: just dev work dev.oblac.gart.cosmic.CosmicTopoKt
# Usage: just dev arts:flowforce dev.oblac.gart.flowforce.monolith.MonolithKt
dev module main:
    #!/usr/bin/env bash
    set -e
    # Convert module path (e.g. "arts:flowforce" or "work") to Gradle project and directory
    GRADLE_PROJECT=":{{module}}"
    MODULE_DIR="$(echo "{{module}}" | tr ':' '/')"
    echo "Building and resolving classpath..."
    ./gradlew "${GRADLE_PROJECT}:writeClasspath" "${GRADLE_PROJECT}:classes" -q
    SESSION="gart-dev"
    CP_FILE="$(pwd)/${MODULE_DIR}/build/classpath.txt"
    tmux kill-session -t "$SESSION" 2>/dev/null || true
    tmux new-session -d -s "$SESSION" -n compile \
        "bash -c './gradlew ${GRADLE_PROJECT}:compileKotlin --continuous -Dorg.gradle.continuous.quietperiod=100'"
    tmux set-option -t "$SESSION" remain-on-exit on
    tmux split-window -t "$SESSION" -h \
        "bash -c 'while true; do find ${MODULE_DIR}/build/classes -name \"*.class\" | entr -d -r java -XX:CICompilerCount=1 -XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Xverify:none -Dgart.align=right @${CP_FILE} {{main}}; done'"
    tmux attach -t "$SESSION"

# Stops the dev session.
dev-stop:
    tmux kill-session -t gart-dev 2>/dev/null || echo "No dev session running."

