import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.gradle.api.Project

import org.gradle.util.GradleVersion.version
import org.w3c.dom.Element

plugins {
  id("java-library")
  id("maven-publish")
  alias(libs.plugins.spotless)
  alias(libs.plugins.checkerFramework)
}

group = "oscarvarto.mx"
version = "1.0.0-SNAPSHOT"
description = "A comparison"

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(25))
  }
  withSourcesJar()
}

dependencies {
  // Lombok should be annotation-processor based (no delombok needed for Checker Framework)
  compileOnly(libs.lombok)
  annotationProcessor(libs.lombok)
  testCompileOnly(libs.lombok)
  testAnnotationProcessor(libs.lombok)

  // Plugin 1.0.x manages checker + checker-qual dependencies automatically

  implementation(platform(libs.jackson.bom))
  implementation(libs.functionaljava)
  implementation(libs.jackson.databind)
  implementation(libs.jackson.module.parameter.names)
  implementation(libs.jackson.datatype.jsr310)
  implementation(libs.jackson.datatype.jdk8)
  implementation(libs.typesafe.config)
  implementation(libs.slf4j.api)

  testImplementation(libs.testng)
  testImplementation(libs.assertj)
  testImplementation(libs.logback.classic)
}

// Compile project-defined qualifier annotations FIRST so SubtypingChecker can load them
val compileQualifiers by tasks.registering(JavaCompile::class) {
  description = "Compiles Checker Framework qualifier annotations so checkers can load them."
  group = "build"

  source = fileTree("src/main/java") {
    include("**/qual/*.java")
  }

  // Needs Checker Framework annotation types on classpath (e.g., org.checkerframework.framework.qual.*)
  classpath = configurations.compileClasspath.get()

  destinationDirectory.set(layout.buildDirectory.dir("qualifiers-classes"))

  options.encoding = "UTF-8"
  options.release.set(25)

  // Prevent Checker Framework (and any other processors) from running on this bootstrap compile
  options.compilerArgs.add("-proc:none")
}

// Make qualifier classes visible to both main and test compilation
for (taskName in listOf("compileJava", "compileTestJava")) {
  tasks.named<JavaCompile>(taskName) {
    dependsOn(compileQualifiers)

    val qualifierOut = files(compileQualifiers.map { it.destinationDirectory })

    // Make qualifier classes visible to compilation
    classpath += qualifierOut

    // Critical: make qualifier classes visible to the checker (processor classloader)
    // In plugin 1.0.x, annotationProcessor already extends checkerFramework,
    // so we only need to append our compiled qualifiers.
    options.annotationProcessorPath = files(
      options.annotationProcessorPath,                  // already includes checker deps via plugin
      qualifierOut                                      // your compiled qualifiers
    )
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.encoding = "UTF-8"
  options.release.set(25)
  options.compilerArgs.addAll(
    listOf(
      "-parameters",
      "-Xmaxerrs",
      "10000",
      "-Xmaxwarns",
      "10000",
    )
  )

  // Silence "sun.misc.Unsafe::objectFieldOffset" warnings from Lombok on JDK 24+.
  // Lombok uses sun.misc.Unsafe internally (lombok.permit.Permit) and hasn't migrated
  // to VarHandle yet. Forking javac lets us pass the JEP 498 flag to the compiler JVM.
  //   - JEP 471 (deprecation):   https://openjdk.org/jeps/471
  //   - JEP 498 (warn phase):    https://openjdk.org/jeps/498
  //   - Lombok tracking issue:   https://github.com/projectlombok/lombok/issues/3852
  // NOTE: JDK 26 moves to Phase 3 (throws by default). This workaround still opts in,
  //       but should be removed once Lombok migrates off sun.misc.Unsafe.
  options.isFork = true
  options.forkOptions.jvmArgs = listOf("--sun-misc-unsafe-memory-access=allow")
}

tasks.withType<Test>().configureEach {
  useTestNG {
    suites("testng.xml")
  }
  maxParallelForks = 1
}

spotless {
  format("misc") {
    target(".gitattributes", ".gitignore")
    trimTrailingWhitespace()
    endWithNewline()
    leadingSpacesToTabs(2)
  }
  java {
    toggleOffOn()
    encoding("UTF-8")
    palantirJavaFormat().apply {
      version(libs.versions.palantirJavaFormat.get())
      style("PALANTIR")
      formatJavadoc(false)
    }
    importOrder("", "\\#")
    removeUnusedImports()
    formatAnnotations()
  }
}

checkerFramework {
  checkers = listOf(
    "org.checkerframework.common.subtyping.SubtypingChecker",
    "org.checkerframework.checker.nullness.NullnessChecker",
  )
  extraJavacArgs =
    listOf(
      "-Aquals=oscarvarto.mx.qual.ErrorMsg,oscarvarto.mx.qual.NotErrorMsg",
      "-Awarns",
    )
  excludeTests = false
  version = libs.versions.checkerframework.get()
}
