plugins {
  id("java-library")
  alias(libs.plugins.spotless)
  alias(libs.plugins.checkerFramework)
}

group = "oscarvarto.mx"
version = "1.0.0-SNAPSHOT"
description = "Java without Lombok — IDE-generated boilerplate, Checker Framework for type safety"

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(25))
  }
  withSourcesJar()
}

dependencies {
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

  classpath = configurations.compileClasspath.get()
  destinationDirectory.set(layout.buildDirectory.dir("qualifiers-classes"))

  options.encoding = "UTF-8"
  options.release.set(25)
  options.compilerArgs.add("-proc:none")
}

// Make qualifier classes visible to both main and test compilation
for (taskName in listOf("compileJava", "compileTestJava")) {
  tasks.named<JavaCompile>(taskName) {
    dependsOn(compileQualifiers)

    val qualifierOut = files(compileQualifiers.map { it.destinationDirectory })

    classpath += qualifierOut

    options.annotationProcessorPath = files(
      options.annotationProcessorPath,
      qualifierOut
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
    palantirJavaFormat(libs.versions.palantirJavaFormat.get()).apply {
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
