/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.runners;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlinx.serialization.TestGeneratorKt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
public class SerializationFirBlackBoxTestGenerated extends AbstractSerializationFirBlackBoxTest {
    @Nested
    @TestMetadata("plugins/kotlinx-serialization/testData/boxIr")
    @TestDataPath("$PROJECT_ROOT")
    public class BoxIr {
        @Test
        public void testAllFilesPresentInBoxIr() throws Exception {
            KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("plugins/kotlinx-serialization/testData/boxIr"), Pattern.compile("^(.+)\\.kt$"), null, TargetBackend.JVM_IR, true);
        }

        @Test
        @TestMetadata("annotationsOnFile.kt")
        public void testAnnotationsOnFile() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/annotationsOnFile.kt");
        }

        @Test
        @TestMetadata("classSerializerAsObject.kt")
        public void testClassSerializerAsObject() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/classSerializerAsObject.kt");
        }

        @Test
        @TestMetadata("constValInSerialName.kt")
        public void testConstValInSerialName() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/constValInSerialName.kt");
        }

        @Test
        @TestMetadata("contextualByDefault.kt")
        public void testContextualByDefault() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/contextualByDefault.kt");
        }

        @Test
        @TestMetadata("contextualFallback.kt")
        public void testContextualFallback() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/contextualFallback.kt");
        }

        @Test
        @TestMetadata("contextualWithTypeParameters.kt")
        public void testContextualWithTypeParameters() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/contextualWithTypeParameters.kt");
        }

        @Test
        @TestMetadata("delegatedInterface.kt")
        public void testDelegatedInterface() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/delegatedInterface.kt");
        }

        @Test
        @TestMetadata("enumsAreCached.kt")
        public void testEnumsAreCached() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/enumsAreCached.kt");
        }

        @Test
        @TestMetadata("expectActual.kt")
        public void testExpectActual() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/expectActual.kt");
        }

        @Test
        @TestMetadata("externalSerialierJava.kt")
        public void testExternalSerialierJava() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/externalSerialierJava.kt");
        }

        @Test
        @TestMetadata("externalSerializerForClassWithNonSerializableType.kt")
        public void testExternalSerializerForClassWithNonSerializableType() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/externalSerializerForClassWithNonSerializableType.kt");
        }

        @Test
        @TestMetadata("generatedClassifiersViaLibraryDependency.kt")
        public void testGeneratedClassifiersViaLibraryDependency() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/generatedClassifiersViaLibraryDependency.kt");
        }

        @Test
        @TestMetadata("genericBaseClassMultiple.kt")
        public void testGenericBaseClassMultiple() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/genericBaseClassMultiple.kt");
        }

        @Test
        @TestMetadata("genericBaseClassSimple.kt")
        public void testGenericBaseClassSimple() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/genericBaseClassSimple.kt");
        }

        @Test
        @TestMetadata("generics.kt")
        public void testGenerics() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/generics.kt");
        }

        @Test
        @TestMetadata("inlineClasses.kt")
        public void testInlineClasses() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/inlineClasses.kt");
        }

        @Test
        @TestMetadata("intrinsicAnnotations.kt")
        public void testIntrinsicAnnotations() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/intrinsicAnnotations.kt");
        }

        @Test
        @TestMetadata("intrinsicsBox.kt")
        public void testIntrinsicsBox() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/intrinsicsBox.kt");
        }

        @Test
        @TestMetadata("intrinsicsNullable.kt")
        public void testIntrinsicsNullable() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/intrinsicsNullable.kt");
        }

        @Test
        @TestMetadata("intrinsicsStarProjections.kt")
        public void testIntrinsicsStarProjections() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/intrinsicsStarProjections.kt");
        }

        @Test
        @TestMetadata("metaSerializable.kt")
        public void testMetaSerializable() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/metaSerializable.kt");
        }

        @Test
        @TestMetadata("mpp.kt")
        public void testMpp() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/mpp.kt");
        }

        @Test
        @TestMetadata("multiFieldValueClasses.kt")
        public void testMultiFieldValueClasses() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/multiFieldValueClasses.kt");
        }

        @Test
        @TestMetadata("multimoduleInheritance.kt")
        public void testMultimoduleInheritance() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/multimoduleInheritance.kt");
        }

        @Test
        @TestMetadata("multipleGenericsPolymorphic.kt")
        public void testMultipleGenericsPolymorphic() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/multipleGenericsPolymorphic.kt");
        }

        @Test
        @TestMetadata("sealedInterfaces.kt")
        public void testSealedInterfaces() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/sealedInterfaces.kt");
        }

        @Test
        @TestMetadata("serialInfo.kt")
        public void testSerialInfo() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/serialInfo.kt");
        }

        @Test
        @TestMetadata("serializableOnPropertyType.kt")
        public void testSerializableOnPropertyType() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/serializableOnPropertyType.kt");
        }

        @Test
        @TestMetadata("starProjections.kt")
        public void testStarProjections() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/starProjections.kt");
        }

        @Test
        @TestMetadata("typealiasesTest.kt")
        public void testTypealiasesTest() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/typealiasesTest.kt");
        }

        @Test
        @TestMetadata("useSerializersChain.kt")
        public void testUseSerializersChain() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/useSerializersChain.kt");
        }

        @Test
        @TestMetadata("userDefinedSerializerInCompanion.kt")
        public void testUserDefinedSerializerInCompanion() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/boxIr/userDefinedSerializerInCompanion.kt");
        }
    }

    @Nested
    @TestMetadata("plugins/kotlinx-serialization/testData/firMembers")
    @TestDataPath("$PROJECT_ROOT")
    public class FirMembers {
        @Test
        @TestMetadata("abstractAndSealed.kt")
        public void testAbstractAndSealed() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/firMembers/abstractAndSealed.kt");
        }

        @Test
        public void testAllFilesPresentInFirMembers() throws Exception {
            KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("plugins/kotlinx-serialization/testData/firMembers"), Pattern.compile("^(.+)\\.kt$"), null, TargetBackend.JVM_IR, true);
        }

        @Test
        @TestMetadata("classWithCompanionObject.kt")
        public void testClassWithCompanionObject() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/firMembers/classWithCompanionObject.kt");
        }

        @Test
        @TestMetadata("classWithGenericParameters.kt")
        public void testClassWithGenericParameters() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/firMembers/classWithGenericParameters.kt");
        }

        @Test
        @TestMetadata("defaultProperties.kt")
        public void testDefaultProperties() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/firMembers/defaultProperties.kt");
        }

        @Test
        @TestMetadata("enums.kt")
        public void testEnums() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/firMembers/enums.kt");
        }

        @Test
        @TestMetadata("externalSerializers.kt")
        public void testExternalSerializers() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/firMembers/externalSerializers.kt");
        }

        @Test
        @TestMetadata("inlineClasses.kt")
        public void testInlineClasses() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/firMembers/inlineClasses.kt");
        }

        @Test
        @TestMetadata("metaSerializable.kt")
        public void testMetaSerializable() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/firMembers/metaSerializable.kt");
        }

        @Test
        @TestMetadata("multipleProperties.kt")
        public void testMultipleProperties() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/firMembers/multipleProperties.kt");
        }

        @Test
        @TestMetadata("privatePropertiesSerialization.kt")
        public void testPrivatePropertiesSerialization() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/firMembers/privatePropertiesSerialization.kt");
        }

        @Test
        @TestMetadata("serializableObject.kt")
        public void testSerializableObject() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/firMembers/serializableObject.kt");
        }

        @Test
        @TestMetadata("serializableWith.kt")
        public void testSerializableWith() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/firMembers/serializableWith.kt");
        }

        @Test
        @TestMetadata("serializableWithCompanion.kt")
        public void testSerializableWithCompanion() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/firMembers/serializableWithCompanion.kt");
        }

        @Test
        @TestMetadata("serializerInLocalClass.kt")
        public void testSerializerInLocalClass() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/firMembers/serializerInLocalClass.kt");
        }

        @Test
        @TestMetadata("serializerViaCompanion.kt")
        public void testSerializerViaCompanion() throws Exception {
            runTest("plugins/kotlinx-serialization/testData/firMembers/serializerViaCompanion.kt");
        }
    }
}
