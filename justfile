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
    fd -g '*.png' arts/* -x nconvert -ratio -resize 0 240 -overwrite -o {.}_thumb.png {}
    fd -g '*.jpg' arts/* -x nconvert -ratio -resize 0 240 -overwrite -o {.}_thumb.png {}
    rm arts/z/etc/*_thumb.png
