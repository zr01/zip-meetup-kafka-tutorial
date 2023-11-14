import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.spring") version "1.9.10"
    kotlin("plugin.jpa") version "1.9.10"
}

group="studio.camelcase"

java {
    sourceCompatibility = JavaVersion.VERSION_20
}

dependencies {
    implementation("org.apache.avro:avro:1.11.2")
    implementation("org.apache.avro:avro-tools:1.11.2")
}

repositories {
    mavenCentral()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "20"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

application {
    mainClass.set("org.apache.avro.tool.Main")
}

tasks.withType<Jar> {
    enabled = true
}

//tasks.run {
//    delete("src/main/kotlin")
//}

// Execute code gen of schemas by running
// gradlew :kafka-avro:run --args="compile schema schemas src/main/java"