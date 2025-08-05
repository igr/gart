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
