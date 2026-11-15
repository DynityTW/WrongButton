pluginManagement {
    repositories {
        google()
        // Maven Central：先走 Google 的官方鏡像，repo1 當備援（repo.maven.apache.org 在部分網路會限流）。
        maven("https://maven-central.storage-download.googleapis.com/maven2")
        maven("https://repo1.maven.org/maven2")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Maven Central：先走 Google 的官方鏡像，repo1 當備援（repo.maven.apache.org 在部分網路會限流）。
        maven("https://maven-central.storage-download.googleapis.com/maven2")
        maven("https://repo1.maven.org/maven2")
    }
}
rootProject.name = "WrongButton"
include(":app")
