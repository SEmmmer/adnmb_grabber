plugins {
  java
  `java-library`
}

group = "org.example"
version = "1.0-SNAPSHOT"

dependencies {
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
  implementation("org.jsoup:jsoup:1.14.3")
  implementation("com.google.code.gson:gson:2.8.9")
  implementation("org.glavo", "kala-common", version = "0.30.0")
  implementation("org.aya-prover", "base", version = "0.12")
  implementation("org.aya-prover", "cli", version = "0.12")
  implementation("org.aya-prover", "pretty", version = "0.12")
}

allprojects {
  apply {
    plugin("java")
    plugin("idea")
    plugin("maven-publish")
    plugin("java-library")
    plugin("signing")
  }

  tasks.getByName<Test>("test") {
    useJUnitPlatform()
  }

  java {
    withSourcesJar()
    if (hasProperty("release")) withJavadocJar()
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
//    toolchain {
//      languageVersion.set(JavaLanguageVersion.of(17))
//    }
  }

  tasks.withType<JavaCompile>().configureEach {
    modularity.inferModulePath.set(true)

    options.apply {
      encoding = "UTF-8"
      isDeprecation = true
//      release.set(17)
      compilerArgs.addAll(
        listOf(
          "-Xlint:unchecked",
          "--enable-preview",
          "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
          "--add-exports", "jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
          "--add-exports", "jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
          "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        ),
      )
    }
  }

  tasks.withType<Javadoc>().configureEach {
    val options = options as StandardJavadocDocletOptions
    options.addBooleanOption("-enable-preview", true)
    options.addStringOption("-source", "17")
    options.encoding("UTF-8")
    options.tags(
      "apiNote:a:API Note:",
      "implSpec:a:Implementation Requirements:",
      "implNote:a:Implementation Note:",
    )
  }

  tasks.withType<Test>().configureEach {
    jvmArgs = listOf("--enable-preview")
    useJUnitPlatform()
    enableAssertions = true
    reports.junitXml.mergeReruns.set(true)
  }

  tasks.withType<JavaExec>().configureEach {
    jvmArgs = listOf("--enable-preview")
    enableAssertions = true
  }
}
