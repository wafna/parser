group = "parser"
version = "0.1"

plugins {
    id("com.github.ben-manes.versions") version "0.53.0" apply false
    id("org.owasp.dependencycheck") version "12.1.9" apply false
    id("org.jetbrains.dokka")
}

subprojects {
    apply(plugin = "org.owasp.dependencycheck")
    apply(plugin = "com.github.ben-manes.versions")
    tasks.withType<Test> {
        testLogging {
            events("passed", "failed", "skipped")
            showStandardStreams = false
        }
    }
}

repositories {
    mavenCentral()
}
