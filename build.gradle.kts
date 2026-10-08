plugins {
    id("java")
}

group = "com.voxcrafterlp"
version = "1.4.1-RELEASE"

repositories {
    mavenCentral()
    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.21.11-R0.2-SNAPSHOT")
    testImplementation("org.spigotmc:spigot-api:1.21.11-R0.2-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testImplementation("org.mockito:mockito-core:5.20.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    tasks.withType<JavaCompile> {
        options.release.set(17)
    }
}

tasks.jar {
    archiveBaseName.set("CauldronInteract")
    from(files("LICENSE", "README.md")) {
        into("META-INF")
    }
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}
