import net.ltgt.gradle.errorprone.errorprone
plugins {
    id("java")
    id("net.ltgt.errorprone") version "4.1.0"
}

dependencies {
    errorprone("com.google.errorprone:error_prone_core:2.28.0")
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone.disableWarningsInGeneratedCode.set(true)
    options.errorprone.disableAllChecks = true
}
