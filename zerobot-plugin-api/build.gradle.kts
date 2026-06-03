plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    api("org.slf4j:slf4j-api:2.0.13")
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    (options as org.gradle.external.javadoc.StandardJavadocDocletOptions)
        .addStringOption("Xdoclint:none", "-quiet")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("ZeroBot Plugin API")
                description.set("ZeroBot 插件开发 API，插件项目只需要依赖这个包即可编译。")
            }
        }
    }

    repositories {
        maven {
            name = "zeroBotRepository"
            url = uri(
                providers.gradleProperty("zerobotMavenUrl")
                    .orElse(providers.environmentVariable("ZEROBOT_MAVEN_URL"))
                    .orElse(layout.buildDirectory.dir("repo").map { it.asFile.toURI().toString() })
                    .get()
            )
            credentials {
                username = providers.gradleProperty("zerobotMavenUsername")
                    .orElse(providers.environmentVariable("ZEROBOT_MAVEN_USERNAME"))
                    .orNull
                password = providers.gradleProperty("zerobotMavenPassword")
                    .orElse(providers.environmentVariable("ZEROBOT_MAVEN_PASSWORD"))
                    .orNull
            }
        }
    }
}

tasks.register("uploadPluginApi") {
    group = "publishing"
    description = "发布 ZeroBot 插件 API 到配置的 Maven 仓库"
    dependsOn("publishMavenJavaPublicationToZeroBotRepositoryRepository")
}
