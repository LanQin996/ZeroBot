plugins {
    java
}

import org.gradle.api.tasks.bundling.Zip

allprojects {
    group = "cn.zerobot"
    version = "0.1.5"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

tasks.jar {
    archiveBaseName.set("ZeroBot")
    manifest {
        attributes["Main-Class"] = "cn.zerobot.app.ZeroBotApplication"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(
        ":zerobot-plugin-api:classes",
        ":zerobot-core:classes",
        ":zerobot-app:classes",
        project(":zerobot-app").configurations.runtimeClasspath
    )

    from(project(":zerobot-plugin-api").sourceSets.main.get().output)
    from(project(":zerobot-core").sourceSets.main.get().output)
    from(project(":zerobot-app").sourceSets.main.get().output)
    from({
        project(":zerobot-app").configurations.runtimeClasspath.get()
            .filter { it.exists() }
            .map { if (it.isDirectory) it else zipTree(it) }
    })
}

tasks.register<Zip>("releaseZip") {
    group = "distribution"
    description = "Builds a runnable ZeroBot release zip with scripts and default config."
    dependsOn(tasks.jar)

    archiveBaseName.set("ZeroBot")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    val releaseDir = "ZeroBot-${project.version}"
    into(releaseDir) {
        from(tasks.jar.flatMap { it.archiveFile }) {
            rename { "ZeroBot.jar" }
        }
        from("config.yml")
        from("dist/start.bat")
        from("dist/start.sh")
        into("plugins") {
            from("dist/plugins/README.md")
        }
    }
}
