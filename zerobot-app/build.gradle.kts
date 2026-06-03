plugins {
    application
}

dependencies {
    implementation(project(":zerobot-core"))
    implementation("ch.qos.logback:logback-classic:1.5.6")
}

application {
    mainClass.set("cn.zerobot.app.ZeroBotApplication")
}

val prepareDistributionPluginsDir by tasks.registering {
    val keepFile = layout.buildDirectory.file("distribution/plugins/.keep")
    outputs.file(keepFile)
    doLast {
        val file = keepFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText("")
    }
}

distributions {
    main {
        contents {
            from(rootProject.file("config.yml")) {
                into("")
            }
            into("plugins") {
                from(prepareDistributionPluginsDir.map { it.outputs.files.singleFile })
            }
            from(rootProject.file("start.bat")) {
                into("")
            }
        }
    }
}
