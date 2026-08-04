plugins {
    java
    id("io.quarkus") version "3.33.3"
}

group = "com.devsquad"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.33.3"))
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-flyway")
    implementation("io.quarkus:quarkus-narayana-jta")
    implementation("io.quarkus:quarkus-oidc")
    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation(platform("software.amazon.awssdk:bom:2.50.3"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:url-connection-client")
    // Provides CRC64NVME and its native-image metadata without bundling binaries for other platforms.
    runtimeOnly("software.amazon.awssdk.crt:aws-crt:0.48.2:linux-x86_64")
    implementation("com.svix:svix:1.99.1")
    compileOnly("org.osgi:org.osgi.annotation.bundle:2.0.0")
    testCompileOnly("org.osgi:org.osgi.annotation.bundle:2.0.0")

    testImplementation("io.quarkus:quarkus-junit")
    testImplementation("io.quarkus:quarkus-test-security")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.5"))
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all"))
    options.encoding = "UTF-8"
}
