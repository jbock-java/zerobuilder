### gradle

Non-android gradle example

````bash
./gradlew compileJava
./gradlew run
````

### Intellij note

Gradle's generated source root is `./build/generated/source/apt/main`.
To run `GradleMan#main` from intellij, I had to manually mark this folder as "generated sources root",
and possibly run `./gradlew clean` once.
