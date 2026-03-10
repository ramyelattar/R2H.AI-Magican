plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

group = "com.r2h.magican.buildlogic"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.8")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:12.1.1")
}

gradlePlugin {
    plugins {
        register("magicanQuality") {
            id = "com.r2h.magican.quality"
            implementationClass = "MagicanQualityConventionPlugin"
        }
        register("magicanDependencyLocking") {
            id = "com.r2h.magican.dependency-locking"
            implementationClass = "MagicanDependencyLockingConventionPlugin"
        }
    }
}
