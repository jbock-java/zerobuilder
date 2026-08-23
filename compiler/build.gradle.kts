plugins {
    id("java-library")
    id("com.vanniktech.maven.publish") version "0.37.0"
}

group = "io.github.jbock-java"

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    (options as CoreJavadocOptions).addBooleanOption("Xdoclint:none", true)
}

repositories {
  mavenCentral()
}

dependencies {
    var zerobuilder = project(":zerobuilder")
    var sc_version = "1.026"
    var simple_component = "io.github.jbock-java:simple-component:$sc_version"
    compileOnly(simple_component)
    annotationProcessor("io.github.jbock-java:simple-component-compiler:$sc_version")
    implementation(zerobuilder)
    implementation("com.palantir.javapoet:javapoet:0.18.0")
    testImplementation("io.github.jbock-java:compile-testing:0.19.12")
    testImplementation(platform("org.junit:junit-bom:6.1.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(simple_component)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Jar> {
    manifest {
        attributes["Implementation-Version"] = project.version
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

// https://vanniktech.github.io/gradle-maven-publish-plugin/central/
mavenPublishing {
    coordinates("io.github.jbock-java", "zerobuilder-compiler", project.version?.toString())
    pom {
        name = "zerobuilder-compiler"
        packaging = "jar"
        description = "zerobuilder annotation processor"
        url = "https://github.com/jbock-java/zerobuilder"

        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/licenses/MIT"
            }
        }
        developers {
            developer {
                id = "Various"
                name = "Various"
                email = "jbock-java@gmx.de"
            }
        }
        scm {
            connection = "scm:git:https://github.com/jbock-java/zerobuilder.git"
            developerConnection = "scm:git:https://github.com/jbock-java/zerobuilder.git"
            url = "https://github.com/jbock-java/zerobuilder"
        }
    }
    publishToMavenCentral()
    signAllPublications()
}
