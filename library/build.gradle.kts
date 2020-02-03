plugins {
    kotlin("jvm")
}

val swtVersion = extra["swtVersion"]
val swtPlatform = extra["swtPlatform"]

val api by configurations
val implementation by configurations
val compileOnly by configurations
val testImplementation by configurations

dependencies {
    api(kotlin("stdlib-jdk8"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$version")
    compileOnly("org.eclipse.platform:org.eclipse.swt:$swtVersion")

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.12")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$version")
    testImplementation("org.eclipse.platform:org.eclipse.swt:$swtVersion")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
