plugins {
    application
}

dependencies {
    implementation(project(":zerobot-core"))
    implementation("org.slf4j:slf4j-simple:2.0.13")
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
        }
    }
}
