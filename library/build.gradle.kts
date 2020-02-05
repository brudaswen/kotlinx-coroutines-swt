plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") version "0.9.18"
    `maven-publish`
    signing
}

val coroutinesVersion = extra["coroutinesVersion"]
val swtVersion = extra["swtVersion"]

val api by configurations
val implementation by configurations
val compileOnly by configurations
val testImplementation by configurations

dependencies {
    api(kotlin("stdlib-jdk8"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    compileOnly("org.eclipse.platform:org.eclipse.swt:$swtVersion")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("org.eclipse.platform:org.eclipse.swt:$swtVersion")
}

java {
    withSourcesJar()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<GenerateModuleMetadata> {
    enabled = !isSnapshot()
}

val dokkaJavadoc by tasks.creating(org.jetbrains.dokka.gradle.DokkaTask::class) {
    // TODO Change to "javadoc" as soon as https://youtrack.jetbrains.com/issue/KT-31710 is fixed
    outputFormat = "html"
    outputDirectory = "$buildDir/dokkaJavadoc"

    externalDocumentationLink {
        url = url("https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/")
    }
}

val dokkaJavadocJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    archiveClassifier.set("javadoc")
    from(dokkaJavadoc)
}

publishing {
    publications {
        create<MavenPublication>("library") {
            artifactId = "kotlinx-coroutines-swt"

            pom {
                name.set("kotlinx-coroutines-swt")
                description.set("Library to easily use kotlinx.coroutines in SWT applications")
                url.set("https://github.com/brudaswen/kotlinx-coroutines-swt/")

                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("brudaswen")
                        name.set("Sven Obser")
                        email.set("dev@brudaswen.de")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/brudaswen/kotlinx-coroutines-swt.git")
                    developerConnection.set("scm:git:ssh://git@github.com:brudaswen/kotlinx-coroutines-swt.git")
                    url.set("https://github.com/brudaswen/kotlinx-coroutines-swt/")
                }
                issueManagement {
                    system.set("GitHub Issues")
                    url.set("https://github.com/brudaswen/kotlinx-coroutines-swt/issues/")
                }
            }

            from(components["java"])
            artifact(dokkaJavadocJar)
        }
    }

    repositories {
        maven {
            name = "MavenCentral"
            url = when {
                isSnapshot() -> uri("https://oss.sonatype.org/content/repositories/snapshots/")
                else -> uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            }

            val mavenCentralUsername: String? by project
            val mavenCentralPassword: String? by project
            credentials {
                username = mavenCentralUsername
                password = mavenCentralPassword
            }
        }
    }
}

signing {
    setRequired { !isSnapshot() }

    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)

    sign(publishing.publications["library"])
}

fun isSnapshot() = version.toString().endsWith("-SNAPSHOT")

fun org.jetbrains.dokka.gradle.DokkaTask.externalDocumentationLink(closure: org.jetbrains.dokka.DokkaConfiguration.ExternalDocumentationLink.Builder.() -> Unit) =
    externalDocumentationLink(delegateClosureOf(closure))

fun url(path: String) = uri(path).toURL()
