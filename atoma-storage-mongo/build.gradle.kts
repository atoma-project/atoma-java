import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("java")
    id("java-library")
    id("net.ltgt.errorprone") version "4.1.0"
}

dependencies {
    api(project(":atoma-api"))
    implementation(lib.guava)
    implementation(lib.mongodriver)
    implementation(lib.failsafe)
    implementation(lib.slf4j)
    errorprone("com.google.errorprone:error_prone_core:2.28.0")

    runtimeOnly(lib.logback)

    compileOnly(lib.autoserviceannotations)
    compileOnly(lib.autovalueannotations)
    annotationProcessor(lib.autoservice)
    annotationProcessor(lib.autovalue)
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone.disableWarningsInGeneratedCode.set(true)
    options.errorprone.disableAllChecks = true
}

