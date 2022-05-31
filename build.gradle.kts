import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.google.protobuf.gradle.*
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
  java
  application
  id("com.github.johnrengelman.shadow") version "7.0.0"
  id("com.google.protobuf") version "0.8.18"
}

buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.18")
  }
}

group = "com.langinteger.vertx"
version = "1.0.0-SNAPSHOT"

repositories {
  mavenCentral()
}

val vertxVersion = "4.3.1"
val junitJupiterVersion = "5.7.0"

val mainVerticleName = "com.langinteger.vertx.demo.MainVerticle"
val launcherClassName = "io.vertx.core.Launcher"

val watchForChange = "src/**/*"
val doOnChange = "${projectDir}/gradlew classes"

application {
  mainClass.set(launcherClassName)
}

dependencies {
  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  implementation("io.vertx:vertx-grpc:$vertxVersion")
  implementation("io.vertx:vertx-grpc-server:$vertxVersion")
  implementation("io.vertx:vertx-grpc-client:$vertxVersion")

  implementation("javax.annotation:javax.annotation-api:1.3.2")
  implementation("io.grpc:grpc-services:1.30.1")

  testImplementation("io.vertx:vertx-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<ShadowJar> {
  archiveClassifier.set("fat")
  manifest {
    attributes(mapOf("Main-Verticle" to mainVerticleName))
  }
  mergeServiceFiles()
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events = setOf(PASSED, SKIPPED, FAILED)
  }
}

tasks.withType<JavaExec> {
  args = listOf("run", mainVerticleName, "--redeploy=$watchForChange", "--launcher-class=$launcherClassName", "--on-redeploy=$doOnChange")
}

sourceSets {
  main {
    java {
      srcDirs ("src/main/java")
      srcDirs ("build/generated/source/proto/main/grpc")
      srcDirs ("build/generated/source/proto/main/vertx")
      srcDirs ("build/generated/source/proto/main/java")
    }
    resources {
      srcDirs("src/main/proto/config")
    }
    proto {}
  }
}

protobuf {
  protoc {}
  plugins {
    id("grpc") {
      artifact = "io.grpc:protoc-gen-grpc-java:1.45.1"
    }
    id("vertx") {
      artifact = "io.vertx:vertx-grpc-protoc-plugin:4.2.5"
    }
  }
  generateProtoTasks {
    all().forEach {
      it.plugins {
        // Apply the "grpc" plugin whose spec is defined above, without options.
        id("grpc")
        id("vertx") {

        }
      }
    }
  }
}
