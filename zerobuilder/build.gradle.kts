plugins {
  id("java-library")
  id("com.vanniktech.maven.publish") version "0.37.0"
}

group = "io.github.jbock-java"

tasks.withType<JavaCompile>().configureEach {
  options.encoding = "UTF-8"
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<Javadoc>().configureEach {
  options.encoding = "UTF-8"
  (options as CoreJavadocOptions).addBooleanOption("Xdoclint:none", true)
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
  coordinates("io.github.jbock-java", "zerobuilder", project.version?.toString())
  pom {
      name = "zerobuilder"
      packaging = "jar"
      description = "zerobuilder annotations"
      url = "https://github.com/h908714124/zerobuilder"

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
          connection = "scm:git:https://github.com/h908714124/zerobuilder.git"
          developerConnection = "scm:git:https://github.com/h908714124/zerobuilder.git"
          url = "https://github.com/h908714124/zerobuilder"
      }
  }
  publishToMavenCentral()
  signAllPublications()
}
