import com.vanniktech.maven.publish.SonatypeHost
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.plugins.signing.SigningExtension

plugins {
    id("java")
    id("com.vanniktech.maven.publish") version "0.30.0" apply false
}

val publishableModules = listOf(
    "atoma-api", "atoma-core", "atoma-storage-mongo"
)

allprojects {
    group = property("GROUP") as String
    version = property("VERSION") as String
    apply(plugin = "java")

    repositories {
        mavenCentral()
    }
}

subprojects {
    if (name !in publishableModules) return@subprojects

    apply(plugin = "com.vanniktech.maven.publish")
    apply(plugin = "signing")

    configure<SigningExtension> {
        val keyContent = findProperty("signing.key")?.toString()
        val keyPassword = findProperty("signing.password")?.toString()
        if (!keyContent.isNullOrEmpty()) {
            useInMemoryPgpKeys(keyContent, keyPassword)
        }
    }

    configure<MavenPublishBaseExtension> {
        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
        signAllPublications()

        pom {
            name.set(project.name)
            description.set("Atoma: Distributed Concurrency Primitives")
            url.set("https://github.com/atoma-project/atoma-java")

            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }

            developers {
                developer {
                    id.set("marsxuefeng")
                    name.set("XueFeng Ma")
                    email.set("maxuefeng1211@email.com")
                }
            }

            scm {
                connection.set("scm:git:https://github.com/atoma-project/atoma-java.git")
                developerConnection.set("scm:git:ssh://github.com:atoma-project/atoma-java.git")
                url.set("https://github.com/atoma-project/atoma-java")
            }
        }
    }
}