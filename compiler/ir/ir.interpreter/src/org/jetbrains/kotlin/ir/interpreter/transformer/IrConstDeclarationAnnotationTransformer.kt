/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.transformer

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrFile

internal class IrConstDeclarationAnnotationTransformer : IrConstAnnotationTransformer() {
    override fun visitFile(declaration: IrFile, data: Nothing?): IrFile {
        transformAnnotations(declaration)
        return super.visitFile(declaration, data)
    }

    override fun visitDeclaration(declaration: IrDeclarationBase, data: Nothing?): IrStatement {
        transformAnnotations(declaration)
        return super.visitDeclaration(declaration, data)
    }
}
