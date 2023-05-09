/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.structure.impl.source

import com.intellij.openapi.project.Project
import com.intellij.psi.*

abstract class JavaElementSourceFactory {
    abstract fun <PSI : PsiElement> createPsiSource(psi: PSI): JavaElementPsiSource<PSI>
    abstract fun <TYPE : PsiType> createTypeSource(type: TYPE): JavaElementTypeSource<TYPE>
    abstract fun <TYPE : PsiType> createVariableReturnTypeSource(psi: JavaElementPsiSource<out PsiVariable>): JavaElementTypeSource<TYPE>
    abstract fun <TYPE : PsiType> createMethodReturnTypeSource(psi: JavaElementPsiSource<out PsiMethod>): JavaElementTypeSource<TYPE>

    abstract fun <TYPE : PsiType> createExpressionTypeSource(psi: JavaElementPsiSource<out PsiExpression>): JavaElementTypeSource<TYPE>

    companion object {
        @JvmStatic
        fun getInstance(project: Project): JavaElementSourceFactory {
            return project.getService(JavaElementSourceFactory::class.java)
        }
    }
}

class JavaFixedElementSourceFactory : JavaElementSourceFactory() {
    override fun <PSI : PsiElement> createPsiSource(psi: PSI): JavaElementPsiSource<PSI> {
        return JavaElementPsiSourceWithFixedPsi(psi)
    }

    override fun <TYPE : PsiType> createTypeSource(type: TYPE): JavaElementTypeSource<TYPE> {
        return JavaElementTypeSourceWithFixedType(type, this)
    }

    override fun <TYPE : PsiType> createVariableReturnTypeSource(psi: JavaElementPsiSource<out PsiVariable>): JavaElementTypeSource<TYPE> {
        @Suppress("UNCHECKED_CAST")
        return createTypeSource(psi.psi.type as TYPE)
    }


    override fun <TYPE : PsiType> createExpressionTypeSource(psi: JavaElementPsiSource<out PsiExpression>): JavaElementTypeSource<TYPE> {
        @Suppress("UNCHECKED_CAST")
        return createTypeSource(psi.psi.type as TYPE)
    }

    override fun <TYPE : PsiType> createMethodReturnTypeSource(psi: JavaElementPsiSource<out PsiMethod>): JavaElementTypeSource<TYPE> {
        val psiType: PsiType = psi.psi.returnType
            ?: error("Method is not a constructor and has no return type: " + psi.psi.name)
        @Suppress("UNCHECKED_CAST")
        return createTypeSource(psiType as TYPE)
    }
}
