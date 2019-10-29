import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.3.41"
}

group = "mrblrrd"
version = "1.0"

application {
    mainClassName = "mrblrrd.fcmsend.FcmSendKt"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.google.auth:google-auth-library-oauth2-http:0.18.0")
    implementation("com.github.ajalt:clikt:2.2.0")
    implementation("org.json:json:20190722")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}