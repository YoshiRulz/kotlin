/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import org.apache.maven.model.Dependency
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.stream.Collectors
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

typealias PomFilter = (Path, BasicFileAttributes) -> Boolean

class ArtifactsTest {

    private val mavenReader = MavenXpp3Reader()
    private val expectedRepoPath = Paths.get("repo", "artifacts-tests", "src", "test", "resources", "org", "jetbrains", "kotlin")
    private val kotlinVersion = System.getProperty("kotlin.version")
    private val mavenLocal = System.getProperty("maven.repo.local")
    private val localRepoPath = Paths.get(mavenLocal, "org", "jetbrains", "kotlin")

    @Test
    fun verifyArtifacts() {
        val expectedPoms = readPoms(expectedRepoPath, pomFilter)
        val actualPoms = readPoms(localRepoPath, pomFilterWithVersion)
        val errors = mutableSetOf<String>()
        actualPoms.forEach { actual ->
            val expectedPomPathWithModel = expectedPoms[actual.key]
            val actualPomPathWithModel = actual.value
            val actualPomPath = actualPomPathWithModel.path.toAbsolutePath()
            if (expectedPomPathWithModel == null) {
                errors.add("Unknown artifact $actualPomPath")
            } else {
                compareArtifactProperties(expectedPomPathWithModel.model, actualPomPathWithModel.model, actualPomPath, errors)
                compareParent(expectedPomPathWithModel.model, actualPomPathWithModel.model, actualPomPath, errors)
                compareDependencies(
                    expectedPomPathWithModel.model.dependencies,
                    actualPomPathWithModel.model.dependencies,
                    actualPomPath,
                    "dependencies",
                    errors
                )
                compareDependencies(
                    expectedPomPathWithModel.model.dependencyManagement?.dependencies,
                    actualPomPathWithModel.model.dependencyManagement?.dependencies,
                    actualPomPath,
                    "dependencyManagement",
                    errors
                )
            }
        }

        expectedPoms.forEach { expected ->
            if (!actualPoms.containsKey(expected.key)) {
                errors.add("Missing artifact ${expected.key}")
            }
        }

        if (errors.isNotEmpty()) {
            fail(errors.joinToString(System.lineSeparator()))
        }
    }

    // Utility test to sync poms locally
    // @Test
    fun sync() {
        Files.find(localRepoPath, Integer.MAX_VALUE, pomFilterWithVersion)
            .forEach {
                val pomPath = localRepoPath.relativize(it)
                val artifactPath = pomPath.parent.parent
                val newPomPath = expectedRepoPath.resolve(artifactPath.resolve("${artifactPath.fileName}.pom"))
                Files.createDirectories(newPomPath.parent)
                Files.copy(it, newPomPath.toAbsolutePath(), StandardCopyOption.REPLACE_EXISTING)
            }
    }

    private fun compareArtifactProperties(
        expected: Model,
        actual: Model,
        pomPath: Path,
        errors: MutableCollection<String>,
    ) {
        if (actual.groupId != expected.groupId) {
            errors.add("GroupId doesn't match in $pomPath")
        }
        if (actual.packaging != expected.packaging) {
            errors.add("Packaging doesn't match in $pomPath")
        }
    }

    private fun compareParent(
        expected: Model,
        actual: Model,
        pomPath: Path,
        errors: MutableCollection<String>,
    ) {
        if (actual.parent != null) {
            if (expected.parent == null) {
                errors.add("Unexpected parent in $pomPath")
            } else {
                if (actual.parent.artifactId != expected.parent.artifactId) {
                    errors.add("Parent artifactId doesn't match in $pomPath")
                }
                if (actual.parent.groupId != expected.parent.groupId) {
                    errors.add("Parent groupId doesn't match in $pomPath")
                }
            }
        } else {
            if (expected.parent != null) {
                errors.add("Parent is missing in $pomPath")
            }
        }
    }

    private fun compareDependencies(
        expected: Iterable<Dependency>?,
        actual: Iterable<Dependency>?,
        pomPath: Path,
        pomSection: String,
        errors: MutableCollection<String>,
    ) {
        actual?.forEach { actualDependency ->
            val expectedDependency = expected?.find {
                it.artifactId == actualDependency.artifactId
            }
            if (expectedDependency == null) {
                errors.add("[$pomSection] Unexpected dependency $actualDependency in $pomPath")
            } else {
                compareDependencies(expectedDependency, actualDependency, pomPath, pomSection, errors)
            }
        }
    }

    private fun compareDependencies(
        expected: Dependency,
        actual: Dependency,
        pomPath: Path,
        pomSection: String,
        errors: MutableCollection<String>,
    ) {
        if (actual.groupId != expected.groupId) {
            errors.add("[$pomSection] groupId mismatch for $actual in $pomPath")
        }
        if (actual.scope != expected.scope) {
            errors.add("[$pomSection] scope mismatch for $actual in $pomPath")
        }
        if (actual.classifier != expected.classifier) {
            errors.add("[$pomSection] classifier mismatch for $actual in $pomPath")
        }
    }

    private val pomFilter: PomFilter = { path: Path, fileAttributes: BasicFileAttributes ->
        fileAttributes.isRegularFile && "${path.fileName}".endsWith(".pom", ignoreCase = true)
    }

    private val pomFilterWithVersion: PomFilter = { path: Path, fileAttributes: BasicFileAttributes ->
        pomFilter(path, fileAttributes) && path.contains(Paths.get(kotlinVersion))
    }

    private fun readPoms(path: Path, pomFilter: PomFilter): Map<ArtifactId, PomPathWithModel> {
        assertTrue("Path not found: ${path.toAbsolutePath()}") { Files.isDirectory(path) }
        return Files.find(path, Integer.MAX_VALUE, pomFilter)
            .map { PomPathWithModel(it, pomToModel(it)) }
            .collect(Collectors.toMap({ ArtifactId(it.model.artifactId) }, { it }))
    }

    private fun pomToModel(pathToPom: Path): Model {
        return Files.newBufferedReader(pathToPom.toAbsolutePath()).use {
            mavenReader.read(it)
        }
    }

    @JvmInline
    value class ArtifactId(val artifactId: String)
    data class PomPathWithModel(val path: Path, val model: Model)
}