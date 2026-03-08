plugins {
    java
    application
}

group = "st3ix"
version = "0.1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass.set("st3ix.obfuscator.cli.CliRunner")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-commons:9.6")
    implementation("org.yaml:snakeyaml:2.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "st3ix.obfuscator.cli.CliRunner"
    }
}

tasks.register<Jar>("fatJar") {
    archiveBaseName.set("st3ix-obfuscator")
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "st3ix.obfuscator.cli.CliRunner"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    from(sourceSets.main.get().output)
    dependsOn(tasks.classes)
}

tasks.register<Copy>("dist") {
    dependsOn("fatJar")
    from(tasks.named<Jar>("fatJar")) {
        rename { "st3ix-obfuscator.jar" }
    }
    from("scripts")
    from("config.yml.example")
    into(layout.buildDirectory.dir("dist"))
}
