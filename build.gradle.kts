plugins {
    `java-library`
    `maven-publish`
}

group = "eu.mikart"
version = "0.0.2"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("api") {
            from(components["java"])
        }
    }
}
