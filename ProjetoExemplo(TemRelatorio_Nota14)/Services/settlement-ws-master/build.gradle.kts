import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "pt.ulisboa.tecnico.meic.ie.a11"
version = "1.0-SNAPSHOT"

val javaVersion = JavaVersion.VERSION_1_8
val kotlinVersion  = plugins.getPlugin(KotlinPluginWrapper::class.java).kotlinPluginVersion
val jacksonVersion = "2.9.8"
val fuelVersion = "2.1.0"

plugins {
    val ktVersion = "1.3.31"

    kotlin("jvm")            version ktVersion
    kotlin("plugin.jpa")     version ktVersion
    kotlin("plugin.noarg")   version ktVersion
    kotlin("plugin.allopen") version ktVersion

    application
    id("com.github.johnrengelman.shadow") version "5.0.0"
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib-jdk${javaVersion.majorVersion}", kotlinVersion))
    // Fuel
    implementation("com.github.kittinunf.fuel:fuel:$fuelVersion")
    implementation("com.github.kittinunf.fuel:fuel-jackson:$fuelVersion")
    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    // Javalin
    implementation("io.javalin:javalin:2.8.0")
    // Kafka
    implementation("org.apache.kafka:kafka-clients:2.2.0")
    // Persistence (Hibernate + database)
    implementation("org.hibernate:hibernate-core:5.4.2.Final")
    runtime("org.mariadb.jdbc:mariadb-java-client:2.1.2")
    // SLF4J
    implementation("org.slf4j:slf4j-simple:1.7.26")
}

allOpen {
    val javaxAnnoPrefix = "javax.annotation"
    annotation("$javaxAnnoPrefix.Embeddable")
    annotation("$javaxAnnoPrefix.Entity")
    annotation("$javaxAnnoPrefix.MappedSuperclass")
}

application {
    mainClassName = "$group.settlement.MainKt"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = javaVersion.toString()
}

tasks.withType<ShadowJar> {
    manifest.attributes("Main-Class" to "${project.group}.settlement.MainKt")
    archiveClassifier.set(null as String?)
    archiveVersion.set(null as String?)
}
