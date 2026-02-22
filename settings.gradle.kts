rootProject.name = "parser"

plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("org.jetbrains.dokka") version "2.1.0" apply false
}

rootProject.name = "parser"
include("lib")
