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

## Runs the hot reload for Kotlin files.
#hotreload:
#    ./gradlew compileKotlin --continuous -Dorg.gradle.continuous.quietperiod=100

# Make NEW thumbnails (restores git state!).
[group('docs')]
thumbs:
    fd -g '*.png' arts/* -x magick {} -thumbnail 240x240 -unsharp 0x0.75+0.75+0.008 {.}_thumb.png
    rm arts/z/etc/*_thumb.png
    git restore .

# Generates README file.
[group('docs')]
readme:
    ./gradlew :example:run

# Dev session: continuous compile + classloader-based hot restart.
[group('dev')]
mod dev 'just/dev.just'

# Run a sweeper config from work/sweeps/<name>.sweep (brute-force an art over a grid of inputs).
# Every setting lives in the .sweep file (no flags). Usage: just sweep example
[group('dev')]
sweep name:
    #!/usr/bin/env bash
    set -e
    CONFIG="work/sweeps/{{name}}.sweep"
    if [ ! -f "$CONFIG" ]; then
        if [ -f "{{name}}" ]; then CONFIG="{{name}}"; else
            echo "no sweep config '{{name}}' (looked for $CONFIG). available:"
            ls work/sweeps/*.sweep 2>/dev/null | sed 's#work/sweeps/##; s#\.sweep##; s#^#  #' || echo "  (none yet - add work/sweeps/<name>.sweep)"
            exit 1
        fi
    fi
    # renders run on the art's own module classpath, so build whatever 'module = ...' names
    # (default: work). the sweeper itself lives in the library.
    MODULE="$(sed -n 's/^[[:space:]]*module[[:space:]]*=[[:space:]]*\([^#]*\).*/\1/p' "$CONFIG" | head -1 | tr -d '[:space:]')"
    MODULE="${MODULE:-work}"
    ./gradlew :gart:classes :gart:writeClasspath ":${MODULE}:classes" ":${MODULE}:writeClasspath" -q
    java @gart/build/classpath.txt dev.oblac.gart.sweeper.SweeperKt "$CONFIG"

