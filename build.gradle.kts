// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

import com.diffplug.gradle.spotless.SpotlessExtension
import io.spring.gradle.dependencymanagement.internal.dsl.StandardDependencyManagementExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.1" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    kotlin("jvm") version "1.9.22" apply false
    kotlin("plugin.spring") version "1.9.22" apply false
    kotlin("plugin.jpa") version "1.9.22" apply false
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1" apply false
    id("com.diffplug.spotless") version "6.25.0" apply false
    id("org.sonarqube") version "4.4.1.3373"
    id("eclipse")
}

version = System.getenv("GITHUB_REF_NAME")?.replace("/", "-")?.lowercase() ?: "develop"

sonar {
    properties {
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.projectKey", "OSGP_sng-coap-http-proxy")
        property("sonar.organization", "gxf")
        property("sonar.gradle.skipCompile", true)
    }
}

extra["archUnitVersion"] = "1.1.0"
extra["californiumVersion"] = "3.8.0"
extra["kotlinLoggingJvmVersion"] = "3.0.5"
extra["mockitoKotlinVersion"] = "5.1.0"
extra["mockServerVersion"] = "5.15.0"

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "eclipse")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
    apply(plugin = "jacoco")
    apply(plugin = "jacoco-report-aggregation")

    group = "org.gxf.standalonenotifyinggateway"
    version = rootProject.version

    repositories {
        mavenCentral()
    }

    extensions.configure<SpotlessExtension> {
        kotlin {
            // by default the target is every '.kt' and '.kts` file in the java source sets
            ktfmt() // has its own section below
            licenseHeader(
                """
                // SPDX-FileCopyrightText: Contributors to the GXF project
                //
                // SPDX-License-Identifier: Apache-2.0

                """.trimIndent(), "package "
            )
        }
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    extensions.configure<StandardDependencyManagementExtension> {
        imports {
            mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "21"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
