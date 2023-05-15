/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jvm

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import java.io.File

public interface JvmCompilationConfig {
    public var kotlinScriptExtensions: List<String>
    public var logger: KotlinLogger

    /**
     * Could be used by a build system to retrieve current defaults for the approach and to customize them
     */
    public fun classpathSnapshotBasedIncrementalCompilationDefaults():
            IncrementalJvmCompilationConfig<ClasspathSnapshotBasedIncrementalCompilationApproachParameters> {
        error("This version of the Build Tools API does not support the classpath snapshots based incremental compilation")
    }

    public fun <P : IncrementalCompilationApproachParameters> useIncrementalCompilation(
        workingDirectory: File,
        rootProjectDir: File,
        sourcesChanges: SourcesChanges,
        approachParameters: P,
        options: IncrementalJvmCompilationConfig<P>,
    ) {
        error("This version of the Build Tools API does not support incremental compilation")
    }
}

/**
 * Options and handles to tune compilation
 */
public interface IncrementalJvmCompilationConfig<P : IncrementalCompilationApproachParameters> {
    public var usePreciseJavaTracking: Boolean
    public var preciseCompilationResultsBackup: Boolean
    public var keepIncrementalCompilationCachesInMemory: Boolean

    /**
     * Could be used to force the non-incremental mode
     */
    public fun nonIncremental()
}

public sealed interface SourcesChanges {
    /**
     * A build system doesn't know the changes, and it'll be considered as a request for non-incremental compilation
     */
    public object Unknown : SourcesChanges

    /**
     * A build system isn't able to track changes, so changes should be tracked on the incremental compiler side.
     */
    public object ToBeCalculated : SourcesChanges

    public class Known(
        public val modifiedFiles: List<File>,
        public val removedFiles: List<File>,
    ) : SourcesChanges
}

public interface ClasspathSnapshotBasedIncrementalJvmCompilationConfig :
    IncrementalJvmCompilationConfig<ClasspathSnapshotBasedIncrementalCompilationApproachParameters> {
    /**
     * Could be used to mark the snapshot files as not changed, if the check is already performed by a build system
     */
    public fun noClasspathSnapshotsChanges()
}

/**
 * Mandatory parameters for an IC approach
 */
public sealed interface IncrementalCompilationApproachParameters

public class ClasspathSnapshotBasedIncrementalCompilationApproachParameters(
    public val currentClasspathSnapshotFiles: List<File>,
    public val previousClasspathSnapshotFile: File,
) : IncrementalCompilationApproachParameters