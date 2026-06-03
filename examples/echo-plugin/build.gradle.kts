plugins {
    java
}

dependencies {
    compileOnly(project(":zerobot-plugin-api"))
}

tasks.jar {
    archiveBaseName.set("zerobot-echo-plugin")
}
