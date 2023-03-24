description = "Kotlin metadata manipulation library"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions.apiVersion = "1.9"
    kotlinOptions.freeCompilerArgs += "-XXLanguage:+EnumEntries"
}

dependencies {
    api(kotlinStdlib())
    compileOnly(project(":core:metadata"))
    compileOnly(protobufLite())
}
