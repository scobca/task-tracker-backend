plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("kapt") version "2.2.21"
    kotlin("plugin.jpa") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.0"
    id("com.bakdata.mockito") version "1.8.1"
    id("org.flywaydb.flyway") version "11.13.2"
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("it.nicolasfarabegoli.conventional-commits") version "3.1.3"
}

group = "org.esc"
description = "task-tracker"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

val jsonwebtokenVersion: String by project
val jaxbApiVersion: String by project
val mapstructVersion: String by project
val kotlinxSerializationVersion: String by project
val kotlinxCoroutinesVersion: String by project
val dotenvSpringVersion: String by project
val javaxAnnotationVersion: String by project
val mockkVersion: String by project
val mockkAgent: Configuration by configurations.creating

dependencies {
    // --- Spring Boot Starters ---
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // --- JWT Auth ---
    implementation("io.jsonwebtoken:jjwt:${jsonwebtokenVersion}")
    implementation("javax.xml.bind:jaxb-api:${jaxbApiVersion}")

    // --- Database ---
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // Mapper
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    kapt("org.mapstruct:mapstruct-processor:$mapstructVersion")

    // --- Kotlin dependencies ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")

    // --- Kotlinx serialization ---
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${kotlinxSerializationVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${kotlinxSerializationVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${kotlinxCoroutinesVersion}")

    // --- Utilities ---
    implementation("one.stayfocused.spring:dotenv-spring-boot:$dotenvSpringVersion")
    implementation("javax.annotation:javax.annotation-api:$javaxAnnotationVersion")
    implementation("org.aspectj:aspectjweaver")

    // --- Tests ---
    mockkAgent("net.bytebuddy:byte-buddy-agent:1.17.8")
    testImplementation("io.mockk:mockk:${mockkVersion}")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-websocket-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-Xshare:off", "-XX:+EnableDynamicAgentLoading")

    testLogging {
        events("passed", "skipped", "failed")
    }

    filter {
        if (project.hasProperty("excludeTests")) {
            excludeTestsMatching(project.property("excludeTests") as String)
        }
    }
}

tasks.register<Copy>("copyDeps") {
    from(configurations.runtimeClasspath)
    into("lib")
}

conventionalCommits {
    warningIfNoGitRoot = true
    types += listOf("build", "chore", "docs", "feat", "fix", "refactor", "style", "test")
    scopes = emptyList()
    successMessage = "Сообщение коммита соответствует стандартам Conventional Commit."
    failureMessage = "Сообщение коммита не соответствует стандартам Conventional Commit."
}
