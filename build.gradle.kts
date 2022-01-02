import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

plugins {
    id("java-library")
    id("maven-publish")
    id("me.champeau.gradle.jmh") version "0.5.3"
}

group = "com.esaulpaugh"
version = "5.6.2-SNAPSHOT"

project.ext.set("archivesBaseName", "headlong")

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(arrayOf("--release", "17"))
    options.encoding = "UTF-8"
}

tasks.withType<Test> {
    maxParallelForks = Runtime.getRuntime().availableProcessors()
    useJUnitPlatform()
}

tasks {
    val sourcesJar by creating(Jar::class) {
        dependsOn(JavaPlugin.CLASSES_TASK_NAME)
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

    val javadocJar by creating(Jar::class) {
        dependsOn(JavaPlugin.JAVADOC_TASK_NAME)
        archiveClassifier.set("javadoc")
        from(JavaPlugin.JAVADOC_TASK_NAME)
        finalizedBy("sourcesJar")
    }

    artifacts {
        add("archives", sourcesJar)
        add("archives", javadocJar)
    }
}

fun todayUTC() : String {
    val sdf = SimpleDateFormat ("MMMMM d yyyy")
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date())
}

tasks.withType<Jar> {
    manifest {
        attributes(
                Pair<String, Any?>("Implementation-Title", project.name),
                Pair<String, Any?>("Implementation-Version", project.version),
                Pair<String, Any?>("Automatic-Module-Name", project.name),
                Pair<String, Any?>("Build-Date", todayUTC())
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.esaulpaugh"
            artifactId = "headlong"
            version = "5.6.2-SNAPSHOT"
            from(components["java"])
            artifact("sourcesJar")
            artifact("javadocJar")
        }
    }
}

repositories {
    mavenCentral()
}

val junitVersion = "5.8.2"
val jmhVersion = "1.33"
val bcVersion = "1.69"

dependencies {
    implementation("com.google.code.gson:gson:2.8.9")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.bouncycastle:bcprov-jdk15on:$bcVersion")
}