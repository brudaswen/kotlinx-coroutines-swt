plugins {
    base
    kotlin("jvm") version "1.3.61" apply false
}

val swtVersion = "3.113.0"
val swtPlatform = getOsgiPlatform()

allprojects {
    group = "de.brudaswen.kotlinx.coroutines"
    version = "1.3.3"

    extra["swtVersion"] = swtVersion
    extra["swtPlatform"] = swtPlatform

    repositories {
        jcenter()
        mavenCentral()
    }

    configurations.all {
        resolutionStrategy.dependencySubstitution {
            substitute(module("org.eclipse.platform:org.eclipse.swt.\${osgi.platform}"))
                .because("The maven property \${osgi.platform} is not handled by Gradle so we replace the dependency, using the osgi platform from the project settings")
                .with(module("org.eclipse.platform:org.eclipse.swt.$swtPlatform:$swtVersion"))
        }
    }
}

/** Get SWT platform identifier. */
fun getOsgiPlatform(): String {
    val library = getSWTWindowingLibrary(System.getProperty("os.name"))
    val platform = getSWTPlatform(System.getProperty("os.name"))
    val arch = getSWTArch(System.getProperty("os.arch"))
    return "$library.$platform.$arch"
}

/** Get SWT windowing library. */
fun getSWTWindowingLibrary(platform: String): String {
    val value = platform.replace(" ", "").toLowerCase()
    return when {
        value.contains("linux") -> "gtk"
        value.contains("darwin") -> "cocoa"
        value.contains("osx") -> "cocoa"
        value.contains("win") -> "win32"
        else -> error("Unknown platform '$platform'.")
    }
}

/** Get SWT platform. */
fun getSWTPlatform(platform: String): String {
    val value = platform.replace(" ", "").toLowerCase()
    return when {
        value.contains("linux") -> "linux"
        value.contains("darwin") -> "macosx"
        value.contains("osx") -> "macosx"
        value.contains("win") -> "win32"
        else -> error("Unknown platform '$platform'.")
    }
}

/** Get SWT architecture. */
fun getSWTArch(arch: String): String =
    when {
        arch.contains("64") -> "x86_64"
        else -> "x86"
    }
