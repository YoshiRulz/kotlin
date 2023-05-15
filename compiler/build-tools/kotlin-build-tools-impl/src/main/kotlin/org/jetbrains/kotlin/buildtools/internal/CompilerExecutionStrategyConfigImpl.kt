/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfig
import java.io.File

class CompilerExecutionStrategyConfigImpl : CompilerExecutionStrategyConfig {
    override fun useInProcessStrategy() {
    }

    override fun useDaemonStrategy(classpath: List<File>, sessionDir: File, jvmArguments: List<String>) {
        TODO("Not yet implemented")
    }
}