import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.21"
    `java-library`
    `maven-publish`
}

group = "com.github.mohamead.allure"
version = "1.0.0-SNAPSHOT"
description = "Kotlin wrapper around Allure 2 that exposes report generation as a class library API"

val allureVersion = "2.46.1"

repositories {
    mavenCentral()
}

dependencies {
    // Allure 2 report generator (bundles the web UI); its plugin API types appear in our public API.
    api("io.qameta.allure:allure-generator:$allureVersion")

    // Since 2.4x these are no longer part of the generator. The Allure CLI shipped them as plugins; without
    // them JUnit XML (surefire/failsafe) results are ignored and the behaviors/packages tabs are empty.
    implementation("io.qameta.allure.plugins:junit-xml-plugin:$allureVersion")
    implementation("io.qameta.allure.plugins:behaviors-plugin:$allureVersion")
    implementation("io.qameta.allure.plugins:packages-plugin:$allureVersion")

    testImplementation(kotlin("test-junit5"))
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.17")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

java {
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 17
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
