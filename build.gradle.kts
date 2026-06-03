plugins {
    java
}

allprojects {
    group = "cn.zerobot"
    version = "0.1.0"
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
