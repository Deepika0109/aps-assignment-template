plugins {
    kotlin("jvm") version "2.1.20"
}

group = "org.example.secondary"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":library"))

    // Mocking & testing dependencies
    val mockkVersion = "1.14.2"
    testImplementation(group = "io.mockk", name = "mockk", mockkVersion)
    testImplementation(group = "io.mockk", name = "mockk-agent", mockkVersion)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}