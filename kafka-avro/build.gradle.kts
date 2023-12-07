import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.spring") version "1.9.21"
    kotlin("plugin.jpa") version "1.9.21"
}

group="studio.camelcase"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation("org.apache.avro:avro:1.11.2")
    runtimeOnly("org.apache.avro:avro-tools:1.11.2")
}

repositories {
    mavenCentral()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
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