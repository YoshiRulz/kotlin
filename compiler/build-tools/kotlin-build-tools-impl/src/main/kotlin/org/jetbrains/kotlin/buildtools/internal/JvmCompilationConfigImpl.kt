/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.jvm.*
import java.io.File

internal class JvmCompilationConfigImpl(
    override var kotlinScriptExtensions: List<String> = emptyList(),
    override var logger: KotlinLogger = DefaultKotlinLogger,
) : JvmCompilationConfig {
    override fun classpathSnapshotBasedIncrementalCompilationDefaults() =
        ClasspathSnapshotBasedIncrementalJvmCompilationConfigImpl()

    override fun <P : IncrementalCompilationApproachParameters> useIncrementalCompilation(
        workingDirectory: File,
        rootProjectDir: File,
        sourcesChanges: SourcesChanges,
        approachParameters: P,
        options: IncrementalJvmCompilationConfig<P>,
    ) = TODO()
}

internal abstract class JvmIncrementalCompilationConfigImpl<P : IncrementalCompilationApproachParameters>(
    override var usePreciseJavaTracking: Boolean = true,
    override var preciseCompilationResultsBackup: Boolean = false,
    override var keepIncrementalCompilationCachesInMemory: Boolean = false,
) : IncrementalJvmCompilationConfig<P> {
    override fun nonIncremental() {
        TODO("Not yet implemented")
    }
}

internal class ClasspathSnapshotBasedIncrementalJvmCompilationConfigImpl :
    JvmIncrementalCompilationConfigImpl<ClasspathSnapshotBasedIncrementalCompilationApproachParameters>(),
    ClasspathSnapshotBasedIncrementalJvmCompilationConfig {

    override fun noClasspathSnapshotsChanges() {
        TODO("Not yet implemented")
    }
}