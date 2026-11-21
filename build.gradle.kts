import org.gradle.external.javadoc.StandardJavadocDocletOptions

subprojects {
    group = "eu.mikart"
    version = "0.2.0"

    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    repositories { mavenCentral() }

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<Javadoc>().configureEach {
        (options as StandardJavadocDocletOptions).apply {
            encoding = "UTF-8"
            tags("apiNote:a:API Note:", "implSpec:a:Implementation Requirements:")
            addBooleanOption("Werror", true)
        }
    }

    tasks.withType<Jar>().configureEach {
        from(rootProject.file("LICENSE")) { into("META-INF") }
    }

    extensions.configure<PublishingExtension> {
        repositories {
            maven("https://repo.codemc.io/repository/ArikSquad/") {
                val mavenUsername = providers.environmentVariable("JENKINS_USERNAME")
                    .orElse(providers.gradleProperty("mavenUsername"))
                val mavenPassword = providers.environmentVariable("JENKINS_PASSWORD")
                    .orElse(providers.gradleProperty("mavenPassword"))

                if (mavenUsername.isPresent && mavenPassword.isPresent) {
                    credentials {
                        username = mavenUsername.get()
                        password = mavenPassword.get()
                    }
                }
            }
        }

        publications.create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set(project.name)
                description.set("GroupSecurity public API and platform bindings")
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
