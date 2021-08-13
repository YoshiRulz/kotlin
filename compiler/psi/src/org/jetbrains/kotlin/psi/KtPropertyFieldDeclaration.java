/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.AstLoadingFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinPropertyFieldDeclarationStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

import java.util.Collections;
import java.util.List;

public class KtPropertyFieldDeclaration extends KtDeclarationStub<KotlinPropertyFieldDeclarationStub>
        implements KtModifierListOwner, KtDeclarationWithInitializer {
    public KtPropertyFieldDeclaration(@NotNull ASTNode node) {
        super(node);
    }

    public KtPropertyFieldDeclaration(@NotNull KotlinPropertyFieldDeclarationStub stub) {
        super(stub, KtStubElementTypes.PROPERTY_FIELD_DECLARATION);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitPropertyFieldDeclaration(this, data);
    }

    @Nullable
    public PsiElement getEqualsToken() {
        return findChildByType(KtTokens.EQ);
    }

    @Nullable
    public KtTypeReference getReturnTypeReference() {
        return getStubOrPsiChild(KtStubElementTypes.TYPE_REFERENCE);
    }

    @NotNull
    public PsiElement getNamePlaceholder() {
        PsiElement get = findChildByType(KtTokens.GET_KEYWORD);
        if (get != null) {
            return get;
        }
        return findChildByType(KtTokens.SET_KEYWORD);
    }

    @Nullable
    @Override
    public KtExpression getInitializer() {
        return PsiTreeUtil.getNextSiblingOfType(getEqualsToken(), KtExpression.class);
    }

    @Override
    public boolean hasInitializer() {
        return getInitializer() != null;
    }

    @Override
    public int getTextOffset() {
        return getNamePlaceholder().getTextRange().getStartOffset();
    }
}
