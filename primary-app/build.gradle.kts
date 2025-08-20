plugins {
    kotlin("jvm") version "2.1.20"
}

group = "org.example.primary"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":library"))
    // ---- DB deps ----
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("org.jetbrains.exposed:exposed-core:0.52.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.52.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.52.0")

    // ---- Testing dependencies ----
    testImplementation(kotlin("test"))
    testImplementation(testFixtures(project(":library")))
    val ktor = "3.1.3"
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktor")
    testImplementation("io.ktor:ktor-server-content-negotiation:$ktor")
    testImplementation("io.ktor:ktor-client-core:$ktor")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktor")
    testImplementation("io.ktor:ktor-serialization-jackson-jvm:$ktor")

    // JUnit5
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0")

    // Mocking & testing dependencies
    val mockkVersion = "1.14.2"
    testImplementation(group = "io.mockk", name = "mockk", mockkVersion)
    testImplementation(group = "io.mockk", name = "mockk-agent", mockkVersion)

}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}