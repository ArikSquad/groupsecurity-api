subprojects {
    group = "eu.mikart"
    version = "0.1.0"

    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    repositories { mavenCentral() }

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<Jar>().configureEach {
        from(rootProject.file("LICENSE")) { into("META-INF") }
    }

    extensions.configure<PublishingExtension> {
        val repositoryDirectory = providers.gradleProperty("mavenRepositoryDirectory").orNull
        if (repositoryDirectory != null) {
            repositories.maven {
                name = "Distribution"
                url = rootProject.uri(repositoryDirectory)
            }
        }

        publications.create<MavenPublication>("api") {
            from(components["java"])
            pom {
                name.set(project.name)
                url.set("https://github.com/ArikSquad/groupsecurity-api")
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/ArikSquad/groupsecurity-api.git")
                    url.set("https://github.com/ArikSquad/groupsecurity-api")
                }
            }
        }
    }
}
