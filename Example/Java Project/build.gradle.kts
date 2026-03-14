plugins {
    java
    application
}

application {
    mainClass.set("example.Main")
}

group = "example"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "example.Main"
    }
}

val obfuscatorJar = file("../../build/dist/st3ix-obfuscator-v1.0.3.jar")

tasks.register<JavaExec>("obfuscate") {
    dependsOn(tasks.jar)
    mainClass.set("st3ix.obfuscator.cli.CliRunner")
    classpath = files(obfuscatorJar)
    args(
        "-i", "${layout.buildDirectory.get()}/libs/example-java-project-${version}.jar",
        "-o", "${layout.buildDirectory.get()}/libs/example-java-project-obfuscated.jar"
    )
    doFirst {
        if (!obfuscatorJar.exists()) {
            throw GradleException("Obfuscator not found. Run '../gradlew dist' from project root first.")
        }
    }
}
