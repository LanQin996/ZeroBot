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

distributions {
    main {
        contents {
            from(rootProject.file("config.yml")) {
                into("")
            }
            into("plugins") {
                from(rootProject.file("plugins/.keep"))
            }
            from(rootProject.file("start.bat")) {
                into("")
            }
        }
    }
}
