/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.findDefaultConstructorFor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.memoryOptimizedPlus

// Move static member declarations from classes to top level
class CollectClassesWhichRequiresExtendedMetadataLowering(val context: JsIrBackendContext) : BodyLoweringPass {
    private var IrClass.needExtendedMetadata by context.mapping.classesWithExtendedMetadata

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)

            override fun visitCall(expression: IrCall) {
                if (expression.symbol == context.reflectionSymbols.getKClass) {
                    val klass = expression.getTypeArgument(0)!!.classifierOrFail.owner as IrClass
                    if (klass.isClass) klass.needExtendedMetadata = true
                }
                super.visitCall(expression)
            }
        })
    }
}

class SkipExtendedMetadataLowering(val context: JsIrBackendContext) : DeclarationTransformer {
    private val IrClass.needExtendedMetadata by context.mapping.classesWithExtendedMetadata

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrClass && declaration.needExtendedMetadata != true) {
            declaration.markWithJsSkipFullReflection()
        }
        return null
    }

    private fun IrDeclaration.markWithJsSkipFullReflection() {
        val jsSkipFullReflectionCtor = context.intrinsics.jsSkipFullReflectionAnnotationSymbol.constructors.single()
        annotations = annotations memoryOptimizedPlus JsIrBuilder.buildConstructorCall(jsSkipFullReflectionCtor)
    }
}
