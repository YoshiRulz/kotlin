/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.containers.CollectionFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirGlobalResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirLazyDeclarationResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkCanceled
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.analysis.providers.createAnnotationResolver
import org.jetbrains.kotlin.analysis.providers.createDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.createPackageProvider
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.FirModuleDataImpl
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.JsPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.konan.NativePlatform
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import java.util.concurrent.ConcurrentMap

internal class LLFirSessionCache(private val project: Project) {
    companion object {
        fun getInstance(project: Project): LLFirSessionCache {
            return project.getService(LLFirSessionCache::class.java)
        }
    }

    private val sourceCache: ConcurrentMap<KtModule, CachedValue<LLFirSession>> = CollectionFactory.createConcurrentSoftValueMap()
    private val binaryCache: ConcurrentMap<KtModule, CachedValue<LLFirSession>> = CollectionFactory.createConcurrentSoftValueMap()

    /**
     * Returns the existing session if found, or creates a new session and caches it.
     * Analyzable session will be returned for a library module.
     */
    fun getSession(module: KtModule, preferBinary: Boolean = false): LLFirSession {
        if (module is KtBinaryModule && (preferBinary || module is KtSdkModule)) {
            return getCachedSession(module, binaryCache) {
                createPlatformAwareSessionFactory(module).createBinaryLibrarySession(module)
            }
        }

        return getCachedSession(module, sourceCache, ::createSession)
    }

    /**
     * Returns a session without caching it.
     * Note that session dependencies are still cached.
     */
    internal fun getSessionNoCaching(module: KtModule): LLFirSession {
        return createSession(module)
    }

    private fun <T : KtModule> getCachedSession(
        module: T,
        storage: ConcurrentMap<KtModule, CachedValue<LLFirSession>>,
        factory: (T) -> LLFirSession
    ): LLFirSession {
        checkCanceled()

        return storage.computeIfAbsent(module) {
            CachedValuesManager.getManager(project).createCachedValue {
                val session = factory(module)
                CachedValueProvider.Result(session, session.modificationTracker)
            }
        }.value
    }

    private fun createSession(module: KtModule): LLFirSession {
        val sessionFactory = createPlatformAwareSessionFactory(module)
        return when (module) {
            is KtSourceModule -> sessionFactory.createSourcesSession(module)
            is KtLibraryModule, is KtLibrarySourceModule -> sessionFactory.createLibrarySession(module)
            is KtSdkModule -> sessionFactory.createBinaryLibrarySession(module)
            is KtScriptModule -> sessionFactory.createScriptSession(module)
            is KtCodeFragmentModule -> createCodeFragmentResolvableSession(module)
            is KtNotUnderContentRootModule -> sessionFactory.createNotUnderContentRootResolvableSession(module)
            else -> error("Unexpected module kind: ${module::class.simpleName}")
        }
    }

    private fun createPlatformAwareSessionFactory(module: KtModule): LLFirAbstractSessionFactory {
        val targetPlatform = module.platform
        return when {
            targetPlatform.all { it is JvmPlatform } -> LLFirJvmSessionFactory(project)
            targetPlatform.all { it is JsPlatform } -> LLFirJsSessionFactory(project)
            targetPlatform.all { it is NativePlatform } -> LLFirNativeSessionFactory(project)
            // TODO(kirpichenkov): falling back to JVM. Common session factory hasn't been implemented correctly yet and breaks tests
            else -> LLFirJvmSessionFactory(project)
        }
    }
}

internal fun LLFirSessionConfigurator.Companion.configure(session: LLFirSession) {
    val project = session.project
    for (extension in extensionPointName.getExtensionList(project)) {
        extension.configure(session)
    }
}

@Deprecated(
    "This is a dirty hack used only for one usage (building fir for psi from stubs) and it should be removed after fix of that usage",
    level = DeprecationLevel.ERROR
)
@OptIn(PrivateSessionConstructor::class)
fun createEmptySession(): FirSession {
    return object : FirSession(null, Kind.Source) {}.apply {
        val moduleData = FirModuleDataImpl(
            Name.identifier("<stub module>"),
            dependencies = emptyList(),
            dependsOnDependencies = emptyList(),
            friendDependencies = emptyList(),
            platform = JvmPlatforms.unspecifiedJvmPlatform,
            analyzerServices = JvmPlatformAnalyzerServices
        )
        registerModuleData(moduleData)
        moduleData.bindSession(this)
    }
}

    private fun createCodeFragmentResolvableSession(
        module: KtCodeFragmentModule
    ): LLFirCodeFragmentResolvableModuleSession {
        val builtinsSession = LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(JvmPlatforms.unspecifiedJvmPlatform)
        val scopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
        val globalResolveComponents = LLFirGlobalResolveComponents(project)
        val components = LLFirModuleResolveComponents(
            module.rawContext.containingFile.getKtModule(module.project),
            globalResolveComponents,
            scopeProvider
        )

        val dependencies = collectSourceModuleDependencies(module)
        val dependencyTracker = createSourceModuleDependencyTracker(module, dependencies)
        return LLFirCodeFragmentResolvableModuleSession(
            builtinsSession.ktModule,
            dependencyTracker,
            builtinsSession.builtinTypes,
            components
        ).apply session@{
            components.session = this
            val moduleData = LLFirModuleData(module).apply { bindSession(this@session) }
            register(FirKotlinScopeProvider::class, scopeProvider)
            registerIdeComponents(project)
            registerCommonComponents(LanguageVersionSettingsImpl.DEFAULT)
            registerCommonJavaComponents(JavaModuleResolver.getInstance(project))
            registerCommonComponentsAfterExtensionsAreConfigured()
            registerJavaSpecificResolveComponents()
            registerResolveComponents()
            registerModuleData(moduleData)
            register(FirLazyDeclarationResolver::class, LLFirLazyDeclarationResolver())
            val annotationsResolver = project.createAnnotationResolver(module.contentScope)
            register(FirRegisteredPluginAnnotations::class, LLFirIdeRegisteredPluginAnnotations(this@session, annotationsResolver))
            register(FirPredicateBasedProvider::class, FirEmptyPredicateBasedProvider)
            val provider = LLFirProvider(
                this,
                components,
                project.createDeclarationProvider(module.contentScope),
                project.createPackageProvider(module.contentScope),
                canContainKotlinPackage = true,
            )
            register(FirProvider::class, provider)
            val dependencyProvider = LLFirDependenciesSymbolProvider(this, buildList {
                addDependencySymbolProvidersTo(this@session, dependencies, this)
                add(builtinsSession.symbolProvider)
            })
            val javaSymbolProvider = createJavaSymbolProvider(this, moduleData, project, module.contentScope)
            val codeFragmentSymbolProvider = LLFirCodeFragmentSymbolProvider(this)
            register(
                FirSymbolProvider::class,
                LLFirModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOf(
                        codeFragmentSymbolProvider,
                        javaSymbolProvider,
                    ),
                    dependencyProvider,
                )
            )
            register(LLFirCodeFragmentSymbolProvider::class, codeFragmentSymbolProvider)
            register(JavaSymbolProvider::class, javaSymbolProvider)
            register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, dependencyProvider)
            LLFirSessionConfigurator.configure(this)
        }
    }
}
