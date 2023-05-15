/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import java.io.File

public interface CompilerExecutionStrategyConfig {
    /**
     * The default strategy. Could be used to be explicit and mark it as the preferred one.
     */
    public fun useInProcessStrategy()

    public fun useDaemonStrategy(
        classpath: List<File>,
        sessionDir: File,
        jvmArguments: List<String>,
    )
}