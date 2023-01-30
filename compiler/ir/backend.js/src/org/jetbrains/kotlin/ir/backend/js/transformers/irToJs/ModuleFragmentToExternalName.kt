/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.backend.common.serialization.cityHash64
import org.jetbrains.kotlin.ir.backend.js.utils.sanitizeName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.path

class ModuleFragmentToExternalName(private val jsOutputNamesMapping: Map<IrModuleFragment, String>) {
    fun getExternalNameFor(file: IrFile, granularity: JsGenerationGranularity): String {
        assert(granularity == JsGenerationGranularity.PER_FILE) { "This method should be used only for PER_FILE granularity" }
        return file.module.getJsOutputName().getExternalModuleNameForPerFile(file)
    }

    fun getSafeNameFor(file: IrFile): String {
        return "${file.module.safeName}${file.stableFileName.replace('-', '_')}"
    }

    fun getExternalNameFor(module: IrModuleFragment, granularity: JsGenerationGranularity): String {
        return with(module.getJsOutputName()) {
            when (granularity) {
                JsGenerationGranularity.WHOLE_PROGRAM -> getExternalModuleNameForWholeProgram()
                JsGenerationGranularity.PER_MODULE -> getExternalModuleNameForPerModule()
                JsGenerationGranularity.PER_FILE -> throw AssertionError("This method should be used only for PER_MODULE and WHOLE_PROGRAM granularities")
            }
        }
    }

    private fun IrModuleFragment.getJsOutputName(): String {
        return jsOutputNamesMapping[this] ?: sanitizeName(safeName)
    }

    private fun String.getExternalModuleNameForWholeProgram() = this
    private fun String.getExternalModuleNameForPerModule() = this
    private fun String.getExternalModuleNameForPerFile(file: IrFile) = "$this/${file.stableFileName}"

    private val IrFile.stableFileName: String get() = path.cityHash64().toString()
}