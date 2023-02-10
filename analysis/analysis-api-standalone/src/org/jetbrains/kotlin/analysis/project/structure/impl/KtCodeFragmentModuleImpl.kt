/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.impl

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.project.structure.KtCodeFragmentModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices

internal class KtCodeFragmentModuleImpl(val sourceModule: KtModule): KtCodeFragmentModule {
    override val directRegularDependencies: List<KtModule> = sourceModule.directRegularDependencies
    override val directDependsOnDependencies: List<KtModule> = sourceModule.directDependsOnDependencies
    override val transitiveDependsOnDependencies: List<KtModule> = sourceModule.transitiveDependsOnDependencies
    //override val directRefinementDependencies: List<KtModule> = emptyList()
    override val directFriendDependencies: List<KtModule> = listOf(sourceModule)
    override val contentScope: GlobalSearchScope = sourceModule.contentScope
    override val platform: TargetPlatform
        get() = sourceModule.platform
    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = sourceModule.analyzerServices
    override val project: Project
        get() = sourceModule.project
    override val moduleDescription: String
        get() = "block code fragment from ${sourceModule.moduleDescription}"
}