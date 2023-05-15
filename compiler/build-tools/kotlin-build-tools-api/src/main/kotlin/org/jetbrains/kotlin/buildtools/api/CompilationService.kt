/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathEntrySnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.JvmCompilationConfig
import java.io.File

/**
 * A facade for invoking compilation in Kotlin compiler. It allows to use compiler in different modes.
 * TODO: add a mention where to see the available modes after implementing them
 */
public interface CompilationService {
    public fun calculateClasspathSnapshot(classpathEntry: File): ClasspathEntrySnapshot

    public fun saveSnapshot(snapshot: ClasspathEntrySnapshot, path: File)

    /**
     * Could be used by a build system to retrieve current defaults for the strategy and to customize them
     */
    public fun generateCompilerExecutionStrategyConfig(): CompilerExecutionStrategyConfig

    /**
     * Could be used by a build system to retrieve current defaults for compilation and to customize them
     */
    public fun generateJvmCompilationConfig(): JvmCompilationConfig

    public fun compileJvm(
        strategyConfig: CompilerExecutionStrategyConfig,
        compilationConfig: JvmCompilationConfig,
        sources: Iterable<File>,
        arguments: List<String>
    )

    public companion object {
        @JvmStatic
        public fun loadImplementation(classLoader: ClassLoader): CompilationService =
            loadImplementation(CompilationService::class, classLoader)
    }
}