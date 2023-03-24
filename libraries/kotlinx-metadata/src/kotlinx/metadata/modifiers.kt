/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata

import kotlinx.metadata.internal.BooleanFlagDelegate
import kotlinx.metadata.internal.EnumFlagDelegate
import org.jetbrains.kotlin.metadata.deserialization.Flags as ProtoFlags
import org.jetbrains.kotlin.metadata.ProtoBuf.Class.Kind as ProtoClassKind
import org.jetbrains.kotlin.metadata.ProtoBuf.Visibility as ProtoVisibility
import org.jetbrains.kotlin.metadata.ProtoBuf.Modality as ProtoModality
import org.jetbrains.kotlin.metadata.ProtoBuf.MemberKind as ProtoMemberKind

// Do not reorder any enums in this file!

/**
 * Represents visibility level (also known as access level) of a corresponding declaration.
 * Some of these visibilities may be non-denotable in Kotlin.
 */
enum class Visibility(kind: Int) {
    /**
     * A visibility flag, signifying that the corresponding declaration is `internal`.
     */
    INTERNAL(ProtoVisibility.INTERNAL_VALUE),

    /**
     * A visibility flag, signifying that the corresponding declaration is `private`.
     */
    PRIVATE(ProtoVisibility.PRIVATE_VALUE),

    /**
     * A visibility flag, signifying that the corresponding declaration is `protected`.
     */
    PROTECTED(ProtoVisibility.PROTECTED_VALUE),

    /**
     * A visibility flag, signifying that the corresponding declaration is `public`.
     */
    PUBLIC(ProtoVisibility.PUBLIC_VALUE),

    /**
     * A visibility flag, signifying that the corresponding declaration is "private-to-this", which is a non-denotable visibility of
     * private members in Kotlin which are callable only on the same instance of the declaring class.
     * Generally, this visibility is more restrictive that 'private', so for most use cases it can be treated the same.
     *
     * Example of 'PRIVATE_TO_THIS' declaration:
     * ```
     *  class A<in T>(t: T) {
     *      private val t: T = t // visibility for t is PRIVATE_TO_THIS
     *
     *      fun test() {
     *          val x: T = t // correct
     *          val y: T = this.t // also correct
     *      }
     *      fun foo(a: A<String>) {
     *         val x: String = a.t // incorrect, because a.t can be Any
     *      }
     *  }
     *  ```
     */
    PRIVATE_TO_THIS(ProtoVisibility.PRIVATE_TO_THIS_VALUE),

    /**
     * A visibility flag, signifying that the corresponding declaration is local, i.e. declared inside a code block
     * and not visible from the outside.
     */
    LOCAL(ProtoVisibility.LOCAL_VALUE)
    ;

    internal val flag = Flag(ProtoFlags.VISIBILITY, kind)
}

/**
 * Represents modality of a corresponding declaration.
 */
enum class Modality(kind: Int) {
    /**
     * A modality flag, signifying that the corresponding declaration is `final`.
     */
    FINAL(ProtoModality.FINAL_VALUE),

    /**
     * A modality flag, signifying that the corresponding declaration is `open`.
     */
    OPEN(ProtoModality.OPEN_VALUE),

    /**
     * A modality flag, signifying that the corresponding declaration is `abstract`.
     */
    ABSTRACT(ProtoModality.ABSTRACT_VALUE),

    /**
     * A modality flag, signifying that the corresponding declaration is `sealed`.
     */
    SEALED(ProtoModality.SEALED_VALUE)
    ;

    internal val flag = Flag(ProtoFlags.MODALITY, kind)
}

/**
 * Represents nature of a corresponding class
 */
enum class ClassKind(kind: Int) {
    /**
     * A class kind flag, signifying that the corresponding class is a usual `class`.
     */
    CLASS(ProtoClassKind.CLASS_VALUE),

    /**
     * A class kind flag, signifying that the corresponding class is an `interface`.
     */
    INTERFACE(ProtoClassKind.INTERFACE_VALUE),

    /**
     * A class kind flag, signifying that the corresponding class is an `enum class`.
     */
    ENUM_CLASS(ProtoClassKind.ENUM_CLASS_VALUE),

    /**
     * A class kind flag, signifying that the corresponding class is an enum entry.
     */
    ENUM_ENTRY(ProtoClassKind.ENUM_ENTRY_VALUE),

    /**
     * A class kind flag, signifying that the corresponding class is an `annotation class`.
     */
    ANNOTATION_CLASS(ProtoClassKind.ANNOTATION_CLASS_VALUE),

    /**
     * A class kind flag, signifying that the corresponding class is a non-companion `object`.
     */
    OBJECT(ProtoClassKind.OBJECT_VALUE),

    /**
     * A class kind flag, signifying that the corresponding class is a `companion object`.
     */
    COMPANION_OBJECT(ProtoClassKind.COMPANION_OBJECT_VALUE)
    ;

    internal val flag = Flag(ProtoFlags.CLASS_KIND, kind)
}

/**
 * Represents kind of a function or property.
 *
 * Kind denotes where this declaration is came from.
 * TODO: explain in detail
 */
enum class MemberKind(kind: Int) {
    /**
     * A member kind flag, signifying that the corresponding function is explicitly declared in the containing class.
     */
    DECLARATION(ProtoMemberKind.DECLARATION_VALUE),

    /**
     * A member kind flag, signifying that the corresponding function exists in the containing class because a function with a suitable
     * signature exists in a supertype.
     * This flag is not written by the Kotlin compiler and normally can't be encountered in binary metadata.
     * Its effects are unspecified.
     */
    FAKE_OVERRIDE(ProtoMemberKind.FAKE_OVERRIDE_VALUE),

    /**
     * A member kind flag, signifying that the corresponding function exists in the containing class because it has been produced
     * by interface delegation (delegation "by").
     *
     * Do not confuse with property delegation.
     */
    DELEGATION(ProtoMemberKind.DELEGATION_VALUE),

    /**
     * A member kind flag, signifying that the corresponding function exists in the containing class because it has been synthesized
     * by the compiler or compiler plugin and has no declaration in the source code.
     *
     * Example of such function can be component1() of a data class.
     */
    SYNTHESIZED(ProtoMemberKind.SYNTHESIZED_VALUE)
    ;

    internal val flag = Flag(ProtoFlags.MEMBER_KIND, kind)
}
