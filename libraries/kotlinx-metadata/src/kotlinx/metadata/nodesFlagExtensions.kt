/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalStdlibApi::class)

package kotlinx.metadata

import kotlinx.metadata.internal.*
import kotlinx.metadata.internal.BooleanFlagDelegate
import kotlinx.metadata.internal.EnumFlagDelegate
import kotlinx.metadata.internal.classBooleanFlag
import kotlinx.metadata.internal.constructorBooleanFlag
import org.jetbrains.kotlin.metadata.deserialization.Flags as ProtoFlags

//todo: annotations, property acc

// --- CLASS ---

/**
 * Represents modality of a corresponding class
 */
var KmClass.modality: Modality by modalityDelegate(KmClass::flags)

/**
 * Represents visibility of a corresponding class
 */
var KmClass.visibility: Visibility by visibilityDelegate(KmClass::flags)

/**
 * Represents kind of a corresponding class
 */
var KmClass.kind: ClassKind by EnumFlagDelegate(
    KmClass::flags,
    ProtoFlags.CLASS_KIND,
    ClassKind.entries,
    ClassKind.entries.map { it.flag },
)

/**
 * Signifies that the corresponding class is `inner`.
 */
var KmClass.isInner: Boolean by classBooleanFlag(Flag(ProtoFlags.IS_INNER))

/**
 * Signifies that the corresponding class is `data`.
 */
var KmClass.isData: Boolean by classBooleanFlag(Flag(ProtoFlags.IS_DATA))

/**
 * Signifies that the corresponding class is `external`.
 */
var KmClass.isExternal: Boolean by classBooleanFlag(Flag(ProtoFlags.IS_EXTERNAL_CLASS))

/**
 * Signifies that the corresponding class is `expect`.
 */
var KmClass.isExpect: Boolean by classBooleanFlag(Flag(ProtoFlags.IS_EXPECT_CLASS))

/**
 * Signifies that the corresponding class is either a pre-Kotlin-1.5 `inline` class, or a 1.5+ `value` class.
 *
 * Note that it doesn't imply that the class has [JvmInline] annotation and will be inlined.
 * Currently, it is impossible to declare a value class without this annotation, but this can be changed in the future.
 */
var KmClass.isValue: Boolean by classBooleanFlag(Flag(ProtoFlags.IS_VALUE_CLASS))

/**
 * Signifies that the corresponding class is a functional interface, i.e. marked with the keyword `fun`.
 */
var KmClass.isFunInterface: Boolean by classBooleanFlag(Flag(ProtoFlags.IS_FUN_INTERFACE))

/**
 * Signifies that the corresponding enum class has synthetic ".entries" property in bytecode.
 * Always `false` for not enum classes.
 */
var KmClass.hasEnumEntries: Boolean by classBooleanFlag(Flag(ProtoFlags.HAS_ENUM_ENTRIES))

// --- CONSTRUCTOR ---
/**
 * TODO
 */
var KmConstructor.visibility: Visibility by visibilityDelegate(KmConstructor::flags)

/**
 * Signifies that the corresponding constructor is secondary, i.e. declared not in the class header, but in the class body.
 */
var KmConstructor.isSecondary: Boolean by constructorBooleanFlag(Flag(ProtoFlags.IS_SECONDARY))

/**
 * Signifies that the corresponding constructor has non-stable parameter names, i.e. cannot be called with named arguments.
 */
var KmConstructor.hasNonStableParameterNames: Boolean by constructorBooleanFlag(Flag(ProtoFlags.IS_CONSTRUCTOR_WITH_NON_STABLE_PARAMETER_NAMES))

// --- FUNCTION ---


/**
 * Represents kind of a corresponding function
 */
var KmFunction.kind: MemberKind by memberKindDelegate(KmFunction::flags)

/**
 * Represents visibility of a corresponding function
 */
var KmFunction.visibility: Visibility by visibilityDelegate(KmFunction::flags)

/**
 * Represents modality of a corresponding function.
 *
 * TODO it can't be sealed
 */
var KmFunction.modality: Modality by modalityDelegate(KmFunction::flags)

/**
 * Signifies that the corresponding function is `operator`.
 */
var KmFunction.isOperator: Boolean by functionBooleanFlag(Flag(ProtoFlags.IS_OPERATOR))

/**
 * Signifies that the corresponding function is `infix`.
 */
var KmFunction.isInfix: Boolean by functionBooleanFlag(Flag(ProtoFlags.IS_INFIX))

/**
 * Signifies that the corresponding function is `inline`.
 */
var KmFunction.isInline: Boolean by functionBooleanFlag(Flag(ProtoFlags.IS_INLINE))

/**
 * Signifies that the corresponding function is `tailrec`.
 */
var KmFunction.isTailrec: Boolean by functionBooleanFlag(Flag(ProtoFlags.IS_TAILREC))

/**
 * Signifies that the corresponding function is `external`.
 */
var KmFunction.isExternal: Boolean by functionBooleanFlag(Flag(ProtoFlags.IS_EXTERNAL_FUNCTION))

/**
 * Signifies that the corresponding function is `suspend`.
 */
var KmFunction.isSuspend: Boolean by functionBooleanFlag(Flag(ProtoFlags.IS_SUSPEND))

/**
 * Signifies that the corresponding function is `expect`.
 */
var KmFunction.isExpect: Boolean by functionBooleanFlag(Flag(ProtoFlags.IS_EXPECT_FUNCTION))

/**
 * Signifies that the corresponding function has non-stable parameter names, i.e. cannot be called with named arguments.
 */
var KmFunction.hasNonStableParameterNames: Boolean by functionBooleanFlag(Flag(ProtoFlags.IS_FUNCTION_WITH_NON_STABLE_PARAMETER_NAMES))

// --- PROPERTY ---

/**
 * Represents visibility of a corresponding property
 */
var KmProperty.visibility: Visibility by visibilityDelegate(KmProperty::flags)

/**
 * Represents modality of a corresponding property.
 *
 * TODO it can't be sealed
 */
var KmProperty.modality: Modality by modalityDelegate(KmProperty::flags)

/**
 * Represents kind of a corresponding property
 */
var KmProperty.kind: MemberKind by memberKindDelegate(KmProperty::flags)

/**
 * Signifies that the corresponding property is `var`.
 */
var KmProperty.isVar: Boolean by propertyBooleanFlag(Flag(ProtoFlags.IS_VAR))

/**
 * Signifies that the corresponding property has a getter.
 */
var KmProperty.hasGetter: Boolean by propertyBooleanFlag(Flag(ProtoFlags.HAS_GETTER))

/**
 * Signifies that the corresponding property has a setter.
 */
var KmProperty.hasSetter: Boolean by propertyBooleanFlag(Flag(ProtoFlags.HAS_SETTER))

/**
 * Signifies that the corresponding property is `const`.
 */
var KmProperty.isConst: Boolean by propertyBooleanFlag(Flag(ProtoFlags.IS_CONST))

/**
 * Signifies that the corresponding property is `lateinit`.
 */
var KmProperty.isLateinit: Boolean by propertyBooleanFlag(Flag(ProtoFlags.IS_LATEINIT))

/**
 * Signifies that the corresponding property has a constant value. On JVM, this flag allows an optimization similarly to
 * [F.HAS_ANNOTATIONS]: constant values of properties are written to the bytecode directly, and this flag can be used to avoid
 * reading the value from the bytecode in case there isn't one.
 */
var KmProperty.hasConstant: Boolean by propertyBooleanFlag(Flag(ProtoFlags.HAS_CONSTANT))

/**
 * Signifies that the corresponding property is `external`.
 */
var KmProperty.isExternal: Boolean by propertyBooleanFlag(Flag(ProtoFlags.IS_EXTERNAL_PROPERTY))

/**
 * Signifies that the corresponding property is a delegated property.
 */
var KmProperty.isDelegated: Boolean by propertyBooleanFlag(Flag(ProtoFlags.IS_DELEGATED))

/**
 * Signifies that the corresponding property is `expect`.
 */
var KmProperty.isExpect: Boolean by propertyBooleanFlag(Flag(ProtoFlags.IS_EXPECT_PROPERTY))

// --- TYPE & TYPE_PARAM
/**
 * Signifies that the corresponding type is marked as nullable, i.e. has a question mark at the end of its notation.
 */

var KmType.isNullable: Boolean by typeBooleanFlag(Flag(0, 1, 1))

/**
 * Signifies that the corresponding type is `suspend`.
 */

var KmType.isSuspend: Boolean by typeBooleanFlag(Flag(ProtoFlags.SUSPEND_TYPE.offset + 1, ProtoFlags.SUSPEND_TYPE.bitWidth, 1))

/**
 * Signifies that the corresponding type is [definitely non-null](https://kotlinlang.org/docs/whatsnew17.html#stable-definitely-non-nullable-types).
 */

var KmType.isDefinitelyNonNull: Boolean by typeBooleanFlag(
    Flag(
        ProtoFlags.DEFINITELY_NOT_NULL_TYPE.offset + 1,
        ProtoFlags.DEFINITELY_NOT_NULL_TYPE.bitWidth,
        1
    )
)


/**
 * Signifies that the corresponding type parameter is `reified`.
 */

var KmTypeParameter.isReified: Boolean by BooleanFlagDelegate(KmTypeParameter::flags, Flag(0, 1, 1))


// --- VALUE PARAMETER ---


/**
 * Signifies that the corresponding value parameter declares a default value. Note that the default value itself can be a complex
 * expression and is not available via metadata. Also note that in case of an override of a parameter with default value, the
 * parameter in the derived method does _not_ declare the default value, but the parameter is
 * still optional at the call site because the default value from the base method is used.
 */
var KmValueParameter.declaresDefaultValue: Boolean by valueParameterBooleanFlag(Flag(ProtoFlags.DECLARES_DEFAULT_VALUE))

/**
 * Signifies that the corresponding value parameter is `crossinline`.
 */
var KmValueParameter.isCrossinline: Boolean by valueParameterBooleanFlag(Flag(ProtoFlags.IS_CROSSINLINE))

/**
 * Signifies that the corresponding value parameter is `noinline`.
 */
var KmValueParameter.isNoinline: Boolean by valueParameterBooleanFlag(Flag(ProtoFlags.IS_NOINLINE))
