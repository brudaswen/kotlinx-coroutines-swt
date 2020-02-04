plugins {
    kotlin("jvm")
}

val swtVersion = extra["swtVersion"]

val implementation by configurations

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":library"))
    implementation("org.eclipse.platform:org.eclipse.swt:$swtVersion")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
