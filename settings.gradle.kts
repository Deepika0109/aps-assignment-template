plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "aps-assignment-template"

include(":library")
include(":primary-app")
include(":secondary-app")