import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
}

// Same group JitPack uses (com.github.<user>), so local and JitPack builds have identical coordinates
group = "com.github.mohamead"
version = "1.0.0-SNAPSHOT"
description = "Kotlin wrapper around Allure 2 that exposes report generation as a class library API"

repositories {
    mavenCentral()
}

dependencies {
    // Allure 2 report generator (bundles the web UI); its plugin API types appear in our public API.
    api(libs.allure.generator)

    // Since 2.4x these are no longer part of the generator. The Allure CLI shipped them as plugins; without
    // them JUnit XML (surefire/failsafe) results are ignored and the behaviors/packages tabs are empty.
    implementation(libs.allure.junit.xml.plugin)
    implementation(libs.allure.behaviors.plugin)
    implementation(libs.allure.packages.plugin)

    testImplementation(kotlin("test-junit5"))
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly(libs.slf4j.simple)
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
