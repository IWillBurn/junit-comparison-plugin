plugins {
    `java-library`
    `maven-publish`
}

group = "org.atpfivt.comparison"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile> {
    // usable wherever JUnit 6 is
    options.release = 17
    options.encoding = "UTF-8"
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
