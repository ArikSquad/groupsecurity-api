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

    extensions.configure<PublishingExtension> {
        publications.create<MavenPublication>("api") { from(components["java"]) }
    }
}
