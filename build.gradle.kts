plugins {
    id("java")
    id("java-gradle-plugin")
    id("maven-publish")
    jacoco
    id("com.github.ben-manes.versions") version("0.53.0")
}

group = "de.living-mainframe"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("commons-io","commons-io","2.20.0")
    implementation("org.jetbrains", "annotations", "26.0.2")
    testImplementation("org.junit.jupiter", "junit-jupiter-api", "6.0.0")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "6.0.0")
    testRuntimeOnly("org.junit.platform", "junit-platform-launcher", "6.0.0")
}

gradlePlugin {
    plugins {
        create("gnu-cobol") {
            id = "de.living-mainframe.gnu-cobol"
            implementationClass = "de.livingmainframe.plugins.cobol.gnu.GnuCobolPlugin"
        }

        create("ibm-enterprise-cobol") {
            id = "de.living-mainframe.ibm-enterprise-cobol"
            implementationClass = "de.livingmainframe.plugins.cobol.ibmenterprise.IbmEnterpriseCobolPlugin"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
}

// Taken from https://github.com/ben-manes/gradle-versions-plugin/blob/bbf550829b87c908b088ea4f24dace5362a392d4/examples/kotlin/build.gradle.kts#L32-L37
fun String.isNonStable(): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(this)
    return isStable.not()
}

// Make sure we only see stable version updates for plugins and dependencies
tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
    rejectVersionIf {
        candidate.version.isNonStable()
    }
}