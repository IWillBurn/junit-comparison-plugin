import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "org.atpfivt.comparison"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// the IDE to build against and to run with `runIde`: IntelliJ IDEA Community 2025.2, the version the plugin was written
// against, downloaded once; -PideaHome=<installation> uses an installed IDE instead
val ideaHome = providers.gradleProperty("ideaHome")

// JUnit for the Java project that the fixture tests open (JavaProjectTestCase), not for running the tests
val testProjectLibraries: Configuration = configurations.create("testProjectLibraries") {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_API))
    }
}

dependencies {
    intellijPlatform {
        if (ideaHome.isPresent) {
            local(ideaHome)
        } else {
            intellijIdeaCommunity("2025.2.6.3")
        }
        bundledPlugin("com.intellij.java")
        bundledPlugin("JUnit")
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")
    // the IDE-level test is a JUnit 3/4 style BasePlatformTestCase
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.13.4")
    testProjectLibraries("org.junit.jupiter:junit-jupiter-api:6.1.3")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

intellijPlatform {
    instrumentCode = false
    buildSearchableOptions = false
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252"
            untilBuild = provider { null }
        }
    }
}

tasks.test {
    useJUnitPlatform()
    inputs.files(testProjectLibraries)
    val libraries = testProjectLibraries
    doFirst {
        systemProperty("test.project.libraries", libraries.asPath)
    }
}
