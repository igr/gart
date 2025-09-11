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

# Make all the thumbnails.
thumbs:
    fd -g '*.png' arts/* -x magick {} -thumbnail 240x240 -unsharp 0x0.75+0.75+0.008 {.}_thumb.png
    rm arts/z/etc/*_thumb.png
    rm arts/example/*_thumb.png

# Generates the README file.
readme:
    ./gradlew :arts:example:run
