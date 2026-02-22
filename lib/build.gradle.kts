plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.commons.math3)
    val arrowVersion = "2.2.1.1"
    implementation("io.arrow-kt:arrow-core:$arrowVersion")
    implementation("io.arrow-kt:arrow-core-jvm:$arrowVersion")
    implementation("io.arrow-kt:arrow-fx-coroutines-jvm:$arrowVersion")
}

testing {
    suites {
        Suppress("unused")
        @Suppress("UnstableApiUsage")
        val test by getting(JvmTestSuite::class) {
            useKotlinTest("2.3.20-RC")
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
