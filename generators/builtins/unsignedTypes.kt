/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.unsigned

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import org.jetbrains.kotlin.generators.builtins.UnsignedType
import org.jetbrains.kotlin.generators.builtins.convert
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.BuiltInsGenerator
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.BuiltInsSourceGenerator
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.*
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.END_LINE
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.FileBuilder
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.file
import java.io.File
import java.io.PrintWriter

fun generateUnsignedTypes(
    targetDir: File,
    generate: (File, (PrintWriter) -> BuiltInsGenerator) -> Unit
) {
    for (type in UnsignedType.values()) {
        generate(File(targetDir, "kotlin/${type.capitalized}.kt")) { UnsignedTypeGenerator(type, it) }
        generate(File(targetDir, "kotlin/${type.capitalized}Array.kt")) { UnsignedArrayGenerator(type, it) }
    }

    for (type in listOf(UnsignedType.UINT, UnsignedType.ULONG)) {
        generate(File(targetDir, "kotlin/${type.capitalized}Range.kt")) { UnsignedRangeGenerator(type, it) }
    }
}

open class UnsignedTypeGenerator(val type: UnsignedType, val out: PrintWriter) : BuiltInsGenerator {
    val className = type.capitalized
    val storageType = type.asSigned.capitalized

    override fun generate() {
        out.println(generateFile().build())
    }

    internal fun binaryOperatorDoc(operator: String, operand1: UnsignedType, operand2: UnsignedType): String = when (operator) {
        "floorDiv" ->
            """
            Divides this value by the other value, flooring the result to an integer that is closer to negative infinity.
            
            For unsigned types, the results of flooring division and truncating division are the same.
            """.trimIndent()
        "rem" -> {
            """
                Calculates the remainder of truncating division of this value (dividend) by the other value (divisor).
                
                The result is always less than the divisor.
                """.trimIndent()
        }
        "mod" -> {
            """
                Calculates the remainder of flooring division of this value (dividend) by the other value (divisor).

                The result is always less than the divisor.
                
                For unsigned types, the remainders of flooring division and truncating division are the same.
                """.trimIndent()
        }
        else -> BasePrimitivesGenerator.binaryOperatorDoc(operator, operand1.asSigned, operand2.asSigned)
    }

    private fun generateFile(): FileBuilder {
        return file {
            generateClass()
            generateExtensionConversions()
        }.apply { this.modifyGeneratedFile() }
    }

    private fun FileBuilder.generateClass() {
        val className = type.capitalized

        klass {
            name = className
            generateCompanionObject()

            generateCompareTo()

            generateBinaryOperators()
            generateUnaryOperators()
            generateRangeTo()
            generateRangeUntil()

            if (type == UnsignedType.UINT || type == UnsignedType.ULONG) {
                generateBitShiftOperators()
            }

            generateBitwiseOperators()

            generateMemberConversions()
            generateFloatingConversions()

            generateToStringHashCode()


            isValue = true
            parentList = mutableListOf("Comparable<$className>")

            constructor {
                visibility = "internal"
                annotations += listOf("kotlin.internal.IntrinsicConstEvaluation", "PublishedApi")
                param {
                    name = "@PublishedApi internal val data"
                    type = storageType
                }
            }
        }.modifyGeneratedClass()
    }

    internal open fun FileBuilder.modifyGeneratedFile() {
        import("kotlin.experimental.*")
        import("kotlin.jvm.*")
    }

    internal open fun ClassBuilder.modifyGeneratedClass() {
        annotations += listOf(
            "SinceKotlin(\"1.5\")",
            "WasExperimental(ExperimentalUnsignedTypes::class)",
            "JvmInline"
        )
    }

    internal open fun PropertyBuilder.modifyGeneratedCompanionObjectProperty() {}

    private fun ClassBuilder.generateCompanionObject() {
        companionObject {
            property {
                appendDoc("A constant holding the minimum value an instance of $className can have.")
                name = "MIN_VALUE"
                type = className
                value = "$className(0)"
            }.modifyGeneratedCompanionObjectProperty()

            property {
                appendDoc("A constant holding the maximum value an instance of $className can have.")
                name = "MAX_VALUE"
                type = className
                value = "$className(-1)"
            }.modifyGeneratedCompanionObjectProperty()

            property {
                appendDoc("The number of bytes used to represent an instance of $className in a binary form.")
                name = "SIZE_BYTES"
                type = "Int"
                value = this@UnsignedTypeGenerator.type.byteSize.toString()
            }.modifyGeneratedCompanionObjectProperty()

            property {
                appendDoc("The number of bits used to represent an instance of $className in a binary form.")
                name = "SIZE_BITS"
                type = "Int"
                value = "${this@UnsignedTypeGenerator.type.byteSize * 8}"
            }.modifyGeneratedCompanionObjectProperty()
        }
    }

    internal open fun MethodBuilder.modifyGeneratedCompareTo(otherType: UnsignedType) {}

    private fun ClassBuilder.generateCompareTo() {
        for (otherType in UnsignedType.values()) {
            val doc = """
                    Compares this value with the specified value for order.
                    Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
                    or a positive number if it's greater than other.
                """.trimIndent()

            method {
                appendDoc(doc)
                annotations += "kotlin.internal.InlineOnly"
                if (otherType == type) annotations += "Suppress(\"OVERRIDE_BY_INLINE\")"
                signature {
                    isInline = true
                    isOperator = true
                    isOverride = otherType == type
                    methodName = "compareTo"
                    returnType = PrimitiveType.INT.capitalized
                    parameter {
                        name = "other"
                        type = otherType.capitalized
                    }
                }

                when {
                    otherType == type && maxByDomainCapacity(type, UnsignedType.UINT) == type -> "${className.lowercase()}Compare(this.data, other.data)"
                    maxOf(type, otherType) < UnsignedType.UINT -> "this.toInt().compareTo(other.toInt())"
                    else -> {
                        val ctype = maxByDomainCapacity(type, otherType)
                        "${convert("this", type, ctype)}.compareTo(${convert("other", otherType, ctype)})"
                    }
                }.addAsSingleLineBody()
            }.modifyGeneratedCompareTo(otherType)
        }
    }

    private fun ClassBuilder.generateBinaryOperators() {
        for (name in BasePrimitivesGenerator.binaryOperators) {
            generateOperator(name)
        }
        generateFloorDivMod("floorDiv")
        generateFloorDivMod("mod")
    }

    internal open fun MethodBuilder.modifyGeneratedBinaryOperation(operatorName: String, otherType: UnsignedType) {}

    private fun ClassBuilder.generateOperator(operatorName: String) {
        for (otherType in UnsignedType.values()) {
            val opReturnType = getOperatorReturnType(type, otherType)

            method {
                appendDoc(binaryOperatorDoc(operatorName, type, otherType))
                annotations += "kotlin.internal.InlineOnly"
                signature {
                    isInline = true
                    isOperator = true
                    methodName = operatorName
                    returnType = opReturnType.capitalized
                    parameter {
                        name = "other"
                        type = otherType.capitalized
                    }
                }

                if (type == otherType && type == opReturnType) {
                    when (operatorName) {
                        "plus", "minus", "times" -> "$className(this.data.$operatorName(other.data))"
                        "div" -> "${type.capitalized.lowercase()}Divide(this, other)"
                        "rem" -> "${type.capitalized.lowercase()}Remainder(this, other)"
                        else -> error(operatorName)
                    }
                } else {
                    "${convert("this", type, opReturnType)}.$operatorName(${convert("other", otherType, opReturnType)})"
                }.addAsSingleLineBody()
            }.modifyGeneratedBinaryOperation(operatorName, otherType)
        }
    }

    internal open fun MethodBuilder.modifyGeneratedFloorDivModOperator(operatorName: String, otherType: UnsignedType) {}

    private fun ClassBuilder.generateFloorDivMod(operatorName: String) {
        for (otherType in UnsignedType.values()) {
            val operationType = getOperatorReturnType(type, otherType)
            val opReturnType = if (operatorName == "mod") otherType else operationType

            method {
                appendDoc(binaryOperatorDoc(operatorName, type, otherType))
                annotations += "kotlin.internal.InlineOnly"
                signature {
                    isInline = true
                    methodName = operatorName
                    returnType = opReturnType.capitalized
                    parameter {
                        name = "other"
                        type = otherType.capitalized
                    }
                }

                if (type == otherType && type == operationType) {
                    when (operatorName) {
                        "floorDiv" -> "div(other)"
                        "mod" -> "rem(other)"
                        else -> error(operatorName)
                    }
                } else {
                    convert(
                        "${convert("this", type, operationType)}.$operatorName(${convert("other", otherType, operationType)})",
                        operationType, opReturnType
                    )
                }.addAsSingleLineBody()
            }.modifyGeneratedFloorDivModOperator(operatorName, otherType)
        }
    }

    internal open fun MethodBuilder.modifyGeneratedUnaryOperation() {}

    private fun ClassBuilder.generateUnaryOperators() {
        for (name in listOf("inc", "dec")) {
            method {
                appendDoc(BasePrimitivesGenerator.incDecOperatorsDoc(name))
                annotations += "kotlin.internal.InlineOnly"
                signature {
                    isInline = true
                    isOperator = true
                    methodName = name
                    returnType = className
                }
                "$className(data.$methodName())".addAsSingleLineBody()
            }.modifyGeneratedUnaryOperation()
        }
    }

    internal open fun MethodBuilder.modifyGeneratedRangeTo(rangeType: String) {

    }

    private fun ClassBuilder.generateRangeTo() {
        val rangeElementType = maxByDomainCapacity(type, UnsignedType.UINT)
        val rangeType = rangeElementType.capitalized + "Range"
        fun convert(name: String) = if (rangeElementType == type) name else "$name.to${rangeElementType.capitalized}()"

        method {
            appendDoc("Creates a range from this value to the specified [other] value.")
            annotations += "kotlin.internal.InlineOnly"
            signature {
                isInline = true
                isOperator = true
                methodName = "rangeTo"
                returnType = rangeType
                parameter {
                    name = "other"
                    type = className
                }
            }
            "$rangeType(${convert("this")}, ${convert("other")})".addAsSingleLineBody()
        }.modifyGeneratedRangeTo(rangeType)
    }

    internal open fun MethodBuilder.modifyGeneratedRangeUntil() {}

    private fun ClassBuilder.generateRangeUntil() {
        val rangeElementType = maxByDomainCapacity(type, UnsignedType.UINT)
        val rangeType = rangeElementType.capitalized + "Range"
        fun convert(name: String) = if (rangeElementType == type) name else "$name.to${rangeElementType.capitalized}()"

        method {
            appendDoc(
                """
                   Creates a range from this value up to but excluding the specified [other] value.
                   
                   If the [other] value is less than or equal to `this` value, then the returned range is empty.
               """.trimIndent()
            )
            annotations += listOf("SinceKotlin(\"1.9\")", "WasExperimental(ExperimentalStdlibApi::class)", "kotlin.internal.InlineOnly")

            signature {
                isInline = true
                isOperator = true
                methodName = "rangeUntil"
                returnType = rangeType
                parameter {
                    name = "other"
                    type = className
                }
            }
            "${convert("this")} until ${convert("other")}".addAsSingleLineBody()
        }.modifyGeneratedRangeUntil()
    }

    internal open fun MethodBuilder.modifyGeneratedBitShiftOperators(operatorName: String) {}

    private fun ClassBuilder.generateBitShiftOperators() {
        fun ClassBuilder.generateShiftOperator(operatorName: String, implementation: String = operatorName) {
            val doc = BasePrimitivesGenerator.shiftOperators[implementation]!!
            val detail = BasePrimitivesGenerator.shiftOperatorsDocDetail(type.asSigned)
            method {
                appendDoc(doc + END_LINE + END_LINE + detail)
                annotations += "kotlin.internal.InlineOnly"
                signature {
                    isInfix = true
                    isInline = true
                    methodName = operatorName
                    returnType = className
                    parameter {
                        name = "bitCount"
                        type = PrimitiveType.INT.capitalized
                    }
                }
                "$className(data $implementation bitCount)".addAsSingleLineBody()
            }.modifyGeneratedBitShiftOperators(operatorName)
        }

        generateShiftOperator("shl")
        generateShiftOperator("shr", "ushr")
    }

    internal open fun MethodBuilder.modifyGeneratedBitwiseOperators(operatorName: String) {}

    private fun ClassBuilder.generateBitwiseOperators() {
        for ((operatorName, doc) in BasePrimitivesGenerator.bitwiseOperators) {
            method {
                appendDoc(doc)
                annotations += "kotlin.internal.InlineOnly"
                signature {
                    isInfix = true
                    isInline = true
                    methodName = operatorName
                    returnType = className
                    parameter {
                        name = "other"
                        type = className
                    }
                }
                "$className(this.data $operatorName other.data)".addAsSingleLineBody()
            }.modifyGeneratedBitwiseOperators(operatorName)
        }

        method {
            appendDoc("Inverts the bits in this value.")
            annotations += "kotlin.internal.InlineOnly"
            signature {
                isInline = true
                methodName = "inv"
                returnType = className
            }
            "$className(data.inv())".addAsSingleLineBody()
        }.modifyGeneratedBitwiseOperators("inv")
    }

    private fun lsb(count: Int) = "least significant $count bits"
    private fun msb(count: Int) = "most significant $count bits"

    internal open fun MethodBuilder.modifyGeneratedConversions(signedType: PrimitiveType) {}
    internal open fun MethodBuilder.modifyGeneratedConversions(unsignedType: UnsignedType) {}

    private fun ClassBuilder.generateMemberConversions() {
        for (otherType in UnsignedType.values()) {
            val signedType = otherType.asSigned
            val signed = signedType.capitalized
            val docTitle = "Converts this [$className] value to [$signed]."
            val docDescription = when {
                otherType < type ->
                    """
                        If this value is less than or equals to [$signed.MAX_VALUE], the resulting `$signed` value represents
                        the same numerical value as this `$className`.
                        
                        The resulting `$signed` value is represented by the ${lsb(otherType.bitSize)} of this `$className` value.
                        Note that the resulting `$signed` value may be negative.
                    """.trimIndent()
                otherType == type -> {
                    """
                        If this value is less than or equals to [$signed.MAX_VALUE], the resulting `$signed` value represents
                        the same numerical value as this `$className`. Otherwise the result is negative.
                        
                        The resulting `$signed` value has the same binary representation as this `$className` value.
                    """.trimIndent()
                }
                else -> {
                    """
                        The resulting `$signed` value represents the same numerical value as this `$className`.
                        
                        The ${lsb(type.bitSize)} of the resulting `$signed` value are the same as the bits of this `$className` value,
                        whereas the ${msb(otherType.bitSize - type.bitSize)} are filled with zeros.
                    """.trimIndent()
                }
            }

            method {
                appendDoc("$docTitle\n\n$docDescription")
                annotations += "kotlin.internal.InlineOnly"
                signature {
                    isInline = true
                    methodName = "to$signed"
                    returnType = signed
                }

                when {
                    otherType < type -> "data.to$signed()"
                    otherType == type -> "data"
                    else -> "data.to$signed() and ${type.mask}"
                }.addAsSingleLineBody()
            }.modifyGeneratedConversions(signedType)
        }

        for (otherType in UnsignedType.values()) {
            val name = otherType.capitalized

            val docs = if (type == otherType)
                "Returns this value."
            else {
                val title = "Converts this [$className] value to [$name]."
                val description = when {
                    otherType < type -> {
                        """
                        If this value is less than or equals to [$name.MAX_VALUE], the resulting `$name` value represents
                        the same numerical value as this `$className`.
                        
                        The resulting `$name` value is represented by the ${lsb(otherType.bitSize)} of this `$className` value.
                        """.trimIndent()
                    }
                    else -> {
                        """
                        The resulting `$name` value represents the same numerical value as this `$className`.
                        
                        The ${lsb(type.bitSize)} of the resulting `$name` value are the same as the bits of this `$className` value,
                        whereas the ${msb(otherType.bitSize - type.bitSize)} are filled with zeros.
                        """.trimIndent()
                    }
                }

                "$title\n\n$description"
            }

            method {
                appendDoc(docs)
                annotations += "kotlin.internal.InlineOnly"
                signature {
                    isInline = true
                    methodName = "to$name"
                    returnType = name
                }
                when {
                    otherType > type -> "${otherType.capitalized}(data.to${otherType.asSigned.capitalized}() and ${type.mask})"
                    otherType == type -> "this"
                    else -> "data.to${otherType.capitalized}()"
                }.addAsSingleLineBody()
            }.modifyGeneratedConversions(otherType)
        }
    }

    internal open fun MethodBuilder.modifyGeneratedFloatingConversions(primitiveType: PrimitiveType) {}

    private fun ClassBuilder.generateFloatingConversions() {
        for (otherType in PrimitiveType.floatingPoint) {
            val otherName = otherType.capitalized
            val docTitle = "Converts this [$className] value to [$otherName]."
            val docDescription = if (type == UnsignedType.ULONG || type == UnsignedType.UINT && otherType == PrimitiveType.FLOAT) {
                """
                The resulting value is the closest `$otherName` to this `$className` value.
                In case when this `$className` value is exactly between two `$otherName`s,
                the one with zero at least significant bit of mantissa is selected.
                """.trimIndent()
            } else {
                "The resulting `$otherName` value represents the same numerical value as this `$className`."
            }

            method {
                appendDoc("$docTitle\n\n$docDescription")
                annotations += "kotlin.internal.InlineOnly"
                signature {
                    isInline = true
                    methodName = "to$otherName"
                    returnType = otherName
                }

                when (type) {
                    UnsignedType.UINT, UnsignedType.ULONG ->
                        if (otherType == PrimitiveType.FLOAT) "this.toDouble().toFloat()" else className.lowercase() + "ToDouble(data)"
                    else ->
                        "this.toInt().to$otherName()"
                }.addAsSingleLineBody()
            }.modifyGeneratedFloatingConversions(otherType)
        }
    }

    internal open fun MethodBuilder.modifyGeneratedExtensionConversion(fromType: PrimitiveType) {}
    internal open fun MethodBuilder.modifyGeneratedExtensionConversion(fromType: UnsignedType) {}

    private fun FileBuilder.generateExtensionConversions() {
        for (otherType in UnsignedType.values()) {
            val otherSigned = otherType.asSigned.capitalized
            val thisSigned = type.asSigned.capitalized

            val docTitle = "Converts this [$otherSigned] value to [$className]."
            val docDescription = when {
                otherType < type -> {
                    """
                    If this value is positive, the resulting `$className` value represents the same numerical value as this `$otherSigned`.
                    
                    The ${lsb(otherType.bitSize)} of the resulting `$className` value are the same as the bits of this `$otherSigned` value,
                    whereas the ${msb(type.bitSize - otherType.bitSize)} are filled with the sign bit of this value.
                    """.trimIndent()
                }
                otherType == type -> {
                    """
                    If this value is positive, the resulting `$className` value represents the same numerical value as this `$otherSigned`.
                    
                    The resulting `$className` value has the same binary representation as this `$otherSigned` value.
                    """.trimIndent()
                }
                else -> {
                    """
                    If this value is positive and less than or equals to [$className.MAX_VALUE], the resulting `$className` value represents
                    the same numerical value as this `$otherSigned`.
                    
                    The resulting `$className` value is represented by the ${lsb(type.bitSize)} of this `$otherSigned` value.
                    """.trimIndent()
                }
            }

            method {
                appendDoc("$docTitle\n\n$docDescription")
                annotations += listOf(
                    "SinceKotlin(\"1.5\")",
                    "WasExperimental(ExperimentalUnsignedTypes::class)",
                    "kotlin.internal.InlineOnly"
                )
                signature {
                    isInline = true
                    extensionReceiver = otherSigned
                    methodName = "to$className"
                    returnType = className
                }

                when (otherType) {
                    type -> "$className(this)"
                    else -> "$className(this.to$thisSigned())"
                }.addAsSingleLineBody()
            }.modifyGeneratedExtensionConversion(otherType)
        }

        if (type == UnsignedType.UBYTE || type == UnsignedType.USHORT)
            return // conversion from UByte/UShort to Float/Double is not allowed

        for (otherType in PrimitiveType.floatingPoint) {
            val otherName = otherType.capitalized

            method {
                appendDoc("""
                 Converts this [$otherName] value to [$className].
                 
                 The fractional part, if any, is rounded down towards zero.
                 Returns zero if this `$otherName` value is negative or `NaN`, [$className.MAX_VALUE] if it's bigger than `$className.MAX_VALUE`.
                """.trimIndent())

                annotations += listOf(
                    "SinceKotlin(\"1.5\")",
                    "WasExperimental(ExperimentalUnsignedTypes::class)",
                    "kotlin.internal.InlineOnly"
                )

                signature {
                    isInline = true
                    extensionReceiver = otherName
                    methodName = "to$className"
                    returnType = className
                }

                val conversion = if (otherType == PrimitiveType.DOUBLE) "" else ".toDouble()"
                "doubleTo$className(this$conversion)".addAsSingleLineBody()
            }.modifyGeneratedExtensionConversion(otherType)
        }
    }

    internal open fun MethodBuilder.modifyGeneratedToStringHashCode() {}

    private fun ClassBuilder.generateToStringHashCode() {
        method {
            signature {
                isOverride = true
                methodName = "toString"
                returnType = "String"
            }

            when (type) {
                UnsignedType.UBYTE, UnsignedType.USHORT -> "toInt().toString()"
                UnsignedType.UINT -> "toLong().toString()"
                UnsignedType.ULONG -> "ulongToString(data)"
            }.addAsSingleLineBody()
        }.modifyGeneratedToStringHashCode()
    }


    private fun maxByDomainCapacity(type1: UnsignedType, type2: UnsignedType): UnsignedType =
        if (type1.ordinal > type2.ordinal) type1 else type2


    private fun getOperatorReturnType(type1: UnsignedType, type2: UnsignedType): UnsignedType {
        return maxByDomainCapacity(maxByDomainCapacity(type1, type2), UnsignedType.UINT)
    }

}


class UnsignedArrayGenerator(val type: UnsignedType, out: PrintWriter) : BuiltInsSourceGenerator(out) {
    val elementType = type.capitalized
    val arrayType = elementType + "Array"
    val arrayTypeOf = elementType.lowercase() + "ArrayOf"
    val storageElementType = type.asSigned.capitalized
    val storageArrayType = storageElementType + "Array"
    override fun generateBody() {
        out.println("import kotlin.jvm.*")
        out.println()

        out.println("@SinceKotlin(\"1.3\")")
        out.println("@ExperimentalUnsignedTypes")
        out.println("@JvmInline")
        out.println("public value class $arrayType")
        out.println("@PublishedApi")
        out.println("internal constructor(@PublishedApi internal val storage: $storageArrayType) : Collection<$elementType> {")
        out.println(
            """
    /** Creates a new array of the specified [size], with all elements initialized to zero. */
    public constructor(size: Int) : this($storageArrayType(size))

    /**
     * Returns the array element at the given [index]. This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException] except in Kotlin/JS
     * where the behavior is unspecified.
     */
    public operator fun get(index: Int): $elementType = storage[index].to$elementType()

    /**
     * Sets the element at the given [index] to the given [value]. This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException] except in Kotlin/JS
     * where the behavior is unspecified.
     */
    public operator fun set(index: Int, value: $elementType) {
        storage[index] = value.to$storageElementType()
    }

    /** Returns the number of elements in the array. */
    public override val size: Int get() = storage.size

    /** Creates an iterator over the elements of the array. */
    public override operator fun iterator(): kotlin.collections.Iterator<$elementType> = Iterator(storage)

    private class Iterator(private val array: $storageArrayType) : kotlin.collections.Iterator<${elementType}> {
        private var index = 0
        override fun hasNext() = index < array.size
        override fun next() = if (index < array.size) array[index++].to$elementType() else throw NoSuchElementException(index.toString())
    }

    override fun contains(element: $elementType): Boolean {
        // TODO: Eliminate this check after KT-30016 gets fixed.
        // Currently JS BE does not generate special bridge method for this method.
        @Suppress("USELESS_CAST")
        if ((element as Any?) !is $elementType) return false

        return storage.contains(element.to$storageElementType())
    }

    override fun containsAll(elements: Collection<$elementType>): Boolean {
        return (elements as Collection<*>).all { it is $elementType && storage.contains(it.to$storageElementType()) }
    }

    override fun isEmpty(): Boolean = this.storage.size == 0"""
        )

        out.println("}")

        // TODO: Make inline constructor, like in ByteArray
        out.println("""
/**
 * Creates a new array of the specified [size], where each element is calculated by calling the specified
 * [init] function.
 *
 * The function [init] is called for each array element sequentially starting from the first one.
 * It should return the value for an array element given its index.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun $arrayType(size: Int, init: (Int) -> $elementType): $arrayType {
    return $arrayType($storageArrayType(size) { index -> init(index).to$storageElementType() })
}

@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun $arrayTypeOf(vararg elements: $elementType): $arrayType = elements"""
        )
    }
}

class UnsignedRangeGenerator(val type: UnsignedType, out: PrintWriter) : BuiltInsSourceGenerator(out) {
    val elementType = type.capitalized
    val signedType = type.asSigned.capitalized
    val stepType = signedType
    val stepMinValue = "$stepType.MIN_VALUE"

    override fun getPackage(): String = "kotlin.ranges"

    override fun generateBody() {
        fun hashCodeConversion(name: String, isSigned: Boolean = false) =
            if (type == UnsignedType.ULONG) "($name xor ($name ${if (isSigned) "u" else ""}shr 32))" else name

        out.println(
            """

import kotlin.internal.*

/**
 * A range of values of type `$elementType`.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalUnsignedTypes::class)
public class ${elementType}Range(start: $elementType, endInclusive: $elementType) : ${elementType}Progression(start, endInclusive, 1), ClosedRange<${elementType}>, OpenEndRange<${elementType}> {
    override val start: $elementType get() = first
    override val endInclusive: $elementType get() = last
    
    @Deprecated("Can throw an exception when it's impossible to represent the value with $elementType type, for example, when the range includes MAX_VALUE. It's recommended to use 'endInclusive' property that doesn't throw.")
    @SinceKotlin("1.9")
    @WasExperimental(ExperimentalStdlibApi::class)
    override val endExclusive: $elementType get() {
        if (last == $elementType.MAX_VALUE) error("Cannot return the exclusive upper bound of a range that includes MAX_VALUE.")
        return last + 1u
    }

    override fun contains(value: $elementType): Boolean = first <= value && value <= last

    /** 
     * Checks if the range is empty.
     
     * The range is empty if its start value is greater than the end value.
     */
    override fun isEmpty(): Boolean = first > last

    override fun equals(other: Any?): Boolean =
        other is ${elementType}Range && (isEmpty() && other.isEmpty() ||
                first == other.first && last == other.last)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * ${hashCodeConversion("first")}.toInt() + ${hashCodeConversion("last")}.toInt())

    override fun toString(): String = "${'$'}first..${'$'}last"

    companion object {
        /** An empty range of values of type $elementType. */
        public val EMPTY: ${elementType}Range = ${elementType}Range($elementType.MAX_VALUE, $elementType.MIN_VALUE)
    }
}

/**
 * A progression of values of type `$elementType`.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalUnsignedTypes::class)
public open class ${elementType}Progression
internal constructor(
    start: $elementType,
    endInclusive: $elementType,
    step: $stepType
) : Iterable<$elementType> {
    init {
        if (step == 0.to$stepType()) throw kotlin.IllegalArgumentException("Step must be non-zero.")
        if (step == $stepMinValue) throw kotlin.IllegalArgumentException("Step must be greater than $stepMinValue to avoid overflow on negation.")
    }

    /**
     * The first element in the progression.
     */
    public val first: $elementType = start

    /**
     * The last element in the progression.
     */
    public val last: $elementType = getProgressionLastElement(start, endInclusive, step)

    /**
     * The step of the progression.
     */
    public val step: $stepType = step

    final override fun iterator(): Iterator<$elementType> = ${elementType}ProgressionIterator(first, last, step)

    /** 
     * Checks if the progression is empty.
     
     * Progression with a positive step is empty if its first element is greater than the last element.
     * Progression with a negative step is empty if its first element is less than the last element.
     */
    public open fun isEmpty(): Boolean = if (step > 0) first > last else first < last

    override fun equals(other: Any?): Boolean =
        other is ${elementType}Progression && (isEmpty() && other.isEmpty() ||
                first == other.first && last == other.last && step == other.step)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * (31 * ${hashCodeConversion("first")}.toInt() + ${hashCodeConversion("last")}.toInt()) + ${hashCodeConversion("step", isSigned = true)}.toInt())

    override fun toString(): String = if (step > 0) "${'$'}first..${'$'}last step ${'$'}step" else "${'$'}first downTo ${'$'}last step ${'$'}{-step}"

    companion object {
        /**
         * Creates ${elementType}Progression within the specified bounds of a closed range.

         * The progression starts with the [rangeStart] value and goes toward the [rangeEnd] value not excluding it, with the specified [step].
         * In order to go backwards the [step] must be negative.
         *
         * [step] must be greater than `$stepMinValue` and not equal to zero.
         */
        public fun fromClosedRange(rangeStart: $elementType, rangeEnd: $elementType, step: $stepType): ${elementType}Progression = ${elementType}Progression(rangeStart, rangeEnd, step)
    }
}


/**
 * An iterator over a progression of values of type `$elementType`.
 * @property step the number by which the value is incremented on each step.
 */
@SinceKotlin("1.3")
private class ${elementType}ProgressionIterator(first: $elementType, last: $elementType, step: $stepType) : Iterator<${elementType}> {
    private val finalElement = last
    private var hasNext: Boolean = if (step > 0) first <= last else first >= last
    private val step = step.to$elementType() // use 2-complement math for negative steps
    private var next = if (hasNext) first else finalElement

    override fun hasNext(): Boolean = hasNext

    override fun next(): $elementType {
        val value = next
        if (value == finalElement) {
            if (!hasNext) throw kotlin.NoSuchElementException()
            hasNext = false
        } else {
            next += step
        }
        return value
    }
}
"""
        )
    }

}
