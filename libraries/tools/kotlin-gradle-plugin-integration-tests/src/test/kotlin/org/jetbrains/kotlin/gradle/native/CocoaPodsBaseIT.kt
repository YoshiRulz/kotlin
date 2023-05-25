/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.util.replaceText
import java.nio.file.Path
import kotlin.io.path.appendText

abstract class CocoaPodsBaseIT : KGPBaseTest() {

    companion object {
        val String.validFrameworkName: String
            get() = replace('-', '_')
    }

    protected fun TestProject.useCustomFrameworkName(
        subprojectName: String,
        frameworkName: String,
        iosAppLocation: String? = null,
    ) {
        // Change the name at the Gradle side.
        subProject(subprojectName)
            .buildGradleKts
            .appendToFrameworkBlock("baseName = \"$frameworkName\"")

        // Change swift sources import if needed.
        if (iosAppLocation != null) {
            projectPath
                .resolve(iosAppLocation)
                .resolve("ios-app/ViewController.swift")
                .replaceText(
                    "import ${subprojectName.validFrameworkName}",
                    "import $frameworkName"
                )
        }
    }

    private fun Path.appendToKotlinBlock(str: String) = appendLine(str.wrap("kotlin"))

    private fun Path.appendToCocoapodsBlock(str: String) = appendToKotlinBlock(str.wrap("cocoapods"))

    private fun Path.appendToFrameworkBlock(str: String) = appendToCocoapodsBlock(str.wrap("framework"))

    private fun String.wrap(s: String): String =
        """
        |$s {
        |    $this
        |}
        """.trimMargin()

    private fun Path.appendLine(s: String) = appendText("\n$s")
}