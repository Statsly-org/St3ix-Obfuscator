plugins {
    java
    application
}

group = "st3ix"
version = "1.0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass.set("st3ix.obfuscator.gui.Launcher")
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
        attributes["Main-Class"] = "st3ix.obfuscator.gui.Launcher"
        attributes["Implementation-Version"] = version
    }
}

tasks.register<Jar>("fatJar") {
    archiveBaseName.set("st3ix-obfuscator")
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "st3ix.obfuscator.gui.Launcher"
        attributes["Implementation-Version"] = project.version
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    from(sourceSets.main.get().output)
    from("Images") { into("Images") }
    dependsOn(tasks.classes)
}

tasks.register<Copy>("dist") {
    dependsOn("fatJar")
    from(tasks.named<Jar>("fatJar")) {
        rename { "st3ix-obfuscator.jar" }
    }
    from("scripts")
    from("config.yml.example")
    from("Images")
    into(layout.buildDirectory.dir("dist"))
}
