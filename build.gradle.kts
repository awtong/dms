plugins {
    jacoco
    java
    pmd
    alias(libs.plugins.gatling)
    alias(libs.plugins.openapi.doc)
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.owasp)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "awt"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

configurations.configureEach {
    exclude(group = "commons-logging", module = "commons-logging")
}

dependencies {
    constraints {
        testImplementation("org.apache.commons:commons-compress:1.26.2") {
            because("previous versions have vulnerabilities")
        }
        testImplementation("org.apache.james:apache-mime4j-core:0.8.11") {
            because("previous versions have vulnerabilities")
        }

    }

    implementation(platform(libs.micrometer.bom))
    implementation(platform(libs.opentelemetry.bom))
    implementation(platform(libs.spring.cloud.bom))

    implementation("io.micrometer:micrometer-tracing")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${libs.versions.spring.doc.get()}")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-vault-config")
    implementation("org.springframework.cloud:spring-cloud-vault-config-databases")
    implementation("org.springframework.cloud:spring-cloud-stream")
    implementation("org.springframework.cloud:spring-cloud-stream-binder-rabbit")

    runtimeOnly("io.micrometer:micrometer-tracing-bridge-otel")
    runtimeOnly("io.opentelemetry:opentelemetry-exporter-otlp")
    runtimeOnly("org.springframework.boot:spring-boot-starter-actuator")

    testImplementation(platform(libs.testcontainers.bom))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.cloud:spring-cloud-stream-test-binder")
    testImplementation("org.testcontainers:mongodb")
    testImplementation("org.testcontainers:rabbitmq")
    testImplementation("com.github.dasniko:testcontainers-keycloak:${libs.versions.testcontainers.keycloak.get()}")
}

testing {
    suites {
        configureEach {
            if (this is JvmTestSuite) {
                val junitVersion = dependencyManagement.importedProperties["junit-jupiter.version"]
                useJUnitJupiter(junitVersion!!)
            }
        }

        register<JvmTestSuite>("integrationTest") {
            testType.set(TestSuiteType.INTEGRATION_TEST)
            dependencies {
                implementation(project())
                implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
                implementation("org.springframework.boot:spring-boot-starter-validation")
                implementation("org.springframework.boot:spring-boot-starter-web")
                implementation("org.springframework.boot:spring-boot-starter-test")
                implementation("org.springframework.boot:spring-boot-testcontainers")
                implementation("org.springframework.security:spring-security-test")
                implementation("org.testcontainers:mongodb")
                implementation("org.testcontainers:rabbitmq")
                implementation("com.github.dasniko:testcontainers-keycloak:${libs.versions.testcontainers.keycloak.get()}")
            }
        }
    }
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

openApi {
    customBootRun {
        mainClass.set("awt.dms.LocalDmsApplication")
        classpath.from(sourceSets["integrationTest"].runtimeClasspath)
    }
}

openApiGenerate {
    generatorName.set("postman-collection")
    inputSpec.set("${layout.buildDirectory.get().asFile}/openapi.json")
}

tasks.bootTestRun {
    mainClass.set("awt.dms.LocalDmsApplication")
    doFirst {
        sourceResources(sourceSets["integrationTest"])
        classpath(sourceSets["integrationTest"].runtimeClasspath)
    }
}

tasks.check {
    dependsOn(testing.suites.named("integrationTest"))
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
}

tasks.openApiGenerate {
    dependsOn(tasks.generateOpenApiDocs)
}

tasks.test {
    jvmArgs = listOf("-Xshare:off")
}