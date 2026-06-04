plugins {
    `java-library`
}

dependencies {
    api(project(":zerobot-plugin-api"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.2")
    implementation("org.jline:jline-reader:3.26.3")
    implementation("org.jline:jline-terminal:3.26.3")
    runtimeOnly("org.jline:jline-terminal-jna:3.26.3")
    implementation("org.slf4j:slf4j-api:2.0.13")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("com.squareup.okhttp3:mockwebserver3:5.0.0-alpha.14")
}
