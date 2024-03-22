// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

plugins {
    id("org.springframework.boot")
    id("com.diffplug.spotless") version "6.25.0"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    implementation("io.github.oshai:kotlin-logging-jvm:6.0.1")
    implementation(kotlin("reflect"))

    implementation("org.eclipse.californium:californium-core:${rootProject.extra["californiumVersion"]}")
    implementation("org.eclipse.californium:scandium:${rootProject.extra["californiumVersion"]}")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-cbor")
    implementation("commons-codec:commons-codec:1.16.0")

    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // Generate test and integration test reports
    jacocoAggregation(project(":application"))
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootBuildImage> {
    imageName.set("ghcr.io/osgp/sng-coap-http-proxy:${version}")
    if (project.hasProperty("publishImage")) {
        publish.set(true)
        docker {
            publishRegistry {
                username.set(System.getenv("GITHUB_ACTOR"))
                password.set(System.getenv("GITHUB_TOKEN"))
            }
        }
    }
}

testing {
    suites {
        val integrationTest by registering(JvmTestSuite::class) {
            useJUnitJupiter()
            dependencies {
                implementation(project())
                implementation("org.wiremock:wiremock-standalone:3.3.1")
                implementation("org.springframework.boot:spring-boot-starter-test")
                implementation("org.mock-server:mockserver-spring-test-listener:${rootProject.extra["mockServerVersion"]}")
                implementation("org.eclipse.californium:californium-core:${rootProject.extra["californiumVersion"]}")
                implementation("org.eclipse.californium:scandium:${rootProject.extra["californiumVersion"]}")
                implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-cbor")
                implementation("io.github.microutils:kotlin-logging-jvm:${rootProject.extra["kotlinLoggingJvmVersion"]}")
            }
        }
    }
}

