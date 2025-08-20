plugins {
    kotlin("jvm") version "2.1.20"
    `java-library`
    `java-test-fixtures`
}

group = "org.example.library"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Logger dependencies
    implementation(group = "ch.qos.logback", name = "logback-classic", version = "1.5.18")

    // Ktor Server dependencies
    val ktorVersion = "3.1.3"
    api(group = "io.ktor", name = "ktor-server-core-jvm", ktorVersion)
    implementation(group = "io.ktor", name = "ktor-server-cio-jvm", ktorVersion)
    implementation(group = "io.ktor", name = "ktor-server-content-negotiation", ktorVersion)
    implementation(group = "io.ktor", name = "ktor-serialization-jackson", ktorVersion)
    implementation(group = "io.ktor", name = "ktor-server-status-pages", ktorVersion)


    val exposed = "0.52.0"
    testFixturesImplementation(kotlin("test"))
    testFixturesImplementation("io.mockk:mockk:1.14.2")
    testFixturesImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testFixturesImplementation("io.ktor:ktor-serialization-jackson-jvm:$ktorVersion")
    testFixturesImplementation("com.h2database:h2:2.2.224")
    testFixturesImplementation("org.jetbrains.exposed:exposed-core:$exposed")
    testFixturesImplementation("org.jetbrains.exposed:exposed-jdbc:$exposed")
    testFixturesImplementation("org.jetbrains.exposed:exposed-java-time:$exposed")

    testFixturesImplementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    testFixturesImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testFixturesImplementation("io.ktor:ktor-client-core:$ktorVersion")

    // Jackson dependencies
    val jacksonVersion = "2.18.3"
    api(group = "com.fasterxml.jackson.core", name = "jackson-databind", jacksonVersion)
    api(group = "com.fasterxml.jackson.datatype", name = "jackson-datatype-jsr310", jacksonVersion)
    api(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", jacksonVersion)

    // === Dependencies for the extra features ===
    // RabbitMQ dependencies
    api(group = "com.rabbitmq", name = "amqp-client", version = "5.25.0")

    // OpenSearch dependencies
    api(group = "org.opensearch.client", name = "opensearch-java", version = "2.22.0")
    api(group = "org.apache.httpcomponents.client5", name = "httpclient5", version = "5.2.1")
}

kotlin {
    jvmToolchain(21)
}