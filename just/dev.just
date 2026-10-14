# recipes run from the repo root, not from just/
set working-directory := '..'

_default:
    @just --list dev

# Dev session: continuous compile + classloader-based hot restart.
run module main:
    #!/usr/bin/env bash
    set -e
    GRADLE_PROJECT=":{{module}}"
    MODULE_DIR="$(echo "{{module}}" | tr ':' '/')"
    echo "Building and resolving classpath..."
    ./gradlew "${GRADLE_PROJECT}:writeLauncherClasspath" "${GRADLE_PROJECT}:classes" -q
    SESSION="gart-dev"
    LAUNCHER_CP="$(pwd)/${MODULE_DIR}/build/launcher-cp.txt"
    CLASSES_DIR="$(pwd)/${MODULE_DIR}/build/classes/kotlin/main"
    tmux kill-session -t "$SESSION" 2>/dev/null || true
    tmux new-session -d -s "$SESSION" -n compile \
        "bash -c './gradlew ${GRADLE_PROJECT}:compileKotlin --continuous -Dorg.gradle.continuous.quietperiod=100'"
    tmux set-option -t "$SESSION" remain-on-exit on
    tmux split-window -t "$SESSION" -h \
        "bash -c 'java -XX:CICompilerCount=1 -XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Xverify:none -Dgart.align=right @${LAUNCHER_CP} dev.oblac.gart.hotreload.GartLauncherKt ${CLASSES_DIR} {{main}}'"
    tmux attach -t "$SESSION"

# Dev session for cosmic topo.
cosmic: (run "arts:pixelmania" "pixelmania.cosmic.CosmicTopoKt")

# Dev session for monolith.
monolith: (run "arts:flowforce" "flowforce.monolith.MonolithKt")

# Dev session for unda.
unda: (run "arts:sea" "unda.UndaKt")

# Stops the dev session.
stop:
    tmux kill-session -t gart-dev 2>/dev/null || echo "No dev session running."
