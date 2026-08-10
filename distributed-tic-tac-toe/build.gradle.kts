plugins {
    java
    application
}

group = "it.unibo.pcd.ttt"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainClass.set("it.unibo.pcd.ttt.server.GameServer")
}

tasks.register<JavaExec>("runServer") {
    group = "application"
    description = "Avvia il server RMI del Tris distribuito."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("it.unibo.pcd.ttt.server.GameServer")
    standardInput = System.`in`
}

tasks.register<JavaExec>("runClient") {
    group = "application"
    description = "Avvia un client Swing del Tris distribuito."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("it.unibo.pcd.ttt.client.ClientMain")
    standardInput = System.`in`
}
