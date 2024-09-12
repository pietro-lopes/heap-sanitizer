import org.gradle.internal.impldep.org.apache.commons.lang3.BooleanUtils
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import java.time.Instant
import java.time.format.DateTimeFormatter

plugins {
    java
    idea
    `maven-publish`
}

val runTests: String by project

java {
    withSourcesJar()
    toolchain.languageVersion = JavaLanguageVersion.of(8)
}

base {
    version = "1.0.0"
    archivesName = "heap-sanitizer"
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.1.0")
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.6")

    implementation("commons-io:commons-io:2.15.0")
    implementation("org.apache.commons:commons-lang3:3.14.0")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.22.1")

    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
    testImplementation("org.junit-pioneer:junit-pioneer:1.9.1")
}


val sharedManifest = java.manifest {
    attributes(
        mapOf(
            "Specification-Title" to project.name,
            "Specification-Vendor" to "pietro-lopes",
            "Specification-Version" to "1",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "pietro-lopes",
            "Implementation-Timestamp" to DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        )
    )
}

tasks {
    jar {
        manifest {
            from(sharedManifest)
        }
    }
    named<Jar>("sourcesJar") {
        manifest {
            from(sharedManifest)
        }
    }
    named<Test>("test") {
        useJUnitPlatform()
        onlyIf { runTests.toBoolean() }
//        forkEvery = 1
    }
}

idea {
    project {
        jdkName = java.sourceCompatibility.toString()
        languageLevel = IdeaLanguageLevel(java.sourceCompatibility.toString())
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "dev.uncandango"
            artifactId = "heap-sanitizer"
            version = project.version.toString()

            from(components["java"])
        }
    }
}