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
    finalizedBy("jpackage")
}

tasks.register<Exec>("jpackage") {
    dependsOn("dist")
    val distDir = layout.buildDirectory.dir("dist").get().asFile
    val releaseDir = layout.projectDirectory.dir("release").asFile
    val javaHome = File(System.getProperty("java.home"))
    val jpackageExe = File(javaHome, "bin/jpackage" + if (System.getProperty("os.name").lowercase().contains("win")) ".exe" else "")
    val shortTemp = File(System.getProperty("java.io.tmpdir")).resolve("st3ix-jpkg")
    doFirst {
        if (!jpackageExe.exists()) {
            throw GradleException("jpackage not found at ${jpackageExe}. Use JDK 14+ (not JRE).")
        }
        val outputApp = releaseDir.resolve("St3ixObfuscator")
        if (outputApp.exists()) outputApp.deleteRecursively()
        distDir.resolve("St3ixObfuscator").takeIf { it.exists() }?.deleteRecursively()
        shortTemp.mkdirs()
        project.copy {
            from(distDir.resolve("st3ix-obfuscator.jar"))
            from(distDir.resolve("Images"))
            from(distDir.resolve("config.yml.example"))
            into(shortTemp)
        }
    }
    commandLine(
        jpackageExe.absolutePath,
        "--input", shortTemp.absolutePath,
        "--name", "St3ixObfuscator",
        "--main-jar", "st3ix-obfuscator.jar",
        "--main-class", "st3ix.obfuscator.gui.Launcher",
        "--type", "app-image",
        "--dest", releaseDir.absolutePath,
        "--temp", shortTemp.resolve("work").absolutePath
    )
    doLast {
        shortTemp.deleteRecursively()
    }
}
