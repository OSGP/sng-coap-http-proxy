// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

import com.diffplug.gradle.spotless.SpotlessExtension
import io.spring.gradle.dependencymanagement.internal.dsl.StandardDependencyManagementExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.3.3" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
    kotlin("jvm") version "2.0.20" apply false
    kotlin("plugin.spring") version "2.0.20" apply false
    kotlin("plugin.jpa") version "2.0.20" apply false
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1" apply false
    id("com.diffplug.spotless") version "6.25.0"
    id("org.sonarqube") version "5.1.0.4882"
    id("eclipse")
}

version = System.getenv("GITHUB_REF_NAME")?.replace("/", "-")?.lowercase() ?: "develop"

sonar {
    properties {
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.projectKey", "OSGP_sng-coap-http-proxy")
        property("sonar.organization", "gxf")
    }
}

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

    repositories { mavenCentral() }

    extensions.configure<SpotlessExtension> {
        kotlin {
            // by default the target is every '.kt' and '.kts' file in the java source sets
            diktat("2.0.0").configFile { "${project.rootDir}/diktat-analysis.yml" }

            licenseHeaderFile(
                "${project.rootDir}/license-template.kt",
                "package")
                .updateYearWithLatest(false)
        }
    }

    extensions.configure<KotlinJvmProjectExtension> {
        jvmToolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
        compilerOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }
    extensions.configure<StandardDependencyManagementExtension> {
        imports { mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES) }
    }

    tasks.withType<Test> { useJUnitPlatform() }

    tasks.register<Copy>("updateGitHooks") {
        description = "Copies the pre-commit Git Hook to the .git/hooks folder."
        group = "verification"
        from("${project.rootDir}/scripts/pre-commit")
        into("${project.rootDir}/.git/hooks")
    }

    tasks.withType<KotlinCompile> {
        dependsOn(
            tasks.named("updateGitHooks")
        )
    }
}
