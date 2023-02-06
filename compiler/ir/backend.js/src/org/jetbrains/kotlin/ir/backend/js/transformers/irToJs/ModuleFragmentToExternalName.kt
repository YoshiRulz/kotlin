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
        return "${file.module.safeName}${file.stableFileName}"
    }

    fun getExternalNameFor(module: IrModuleFragment): String {
        return module.getJsOutputName()
    }

    private fun IrModuleFragment.getJsOutputName(): String {
        return jsOutputNamesMapping[this] ?: sanitizeName(safeName)
    }

    private fun String.getExternalModuleNameForPerFile(file: IrFile) = "$this/${file.stableFileName}"

    private val IrFile.stableFileName: String get() =
        path.takeLastWhile { it != '/' }.dropLast(3) + path.cityHash64().toULong().toString(16)
}