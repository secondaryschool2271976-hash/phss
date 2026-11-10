pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "Pharaohs"

// Automatically restore debug.keystore from debug.keystore.base64 if missing on local clone
val keystoreFile = File(rootDir, "debug.keystore")
val base64File = File(rootDir, "debug.keystore.base64")
if (!keystoreFile.exists() && base64File.exists()) {
  try {
    val decoded = java.util.Base64.getDecoder().decode(base64File.readText().trim())
    keystoreFile.writeBytes(decoded)
  } catch (_: Exception) {
  }
}

include(":app")
