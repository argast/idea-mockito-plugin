package com.github.argast.quickfixes;

import com.intellij.codeInsight.daemon.QuickFixBundle;
import com.intellij.codeInsight.daemon.impl.quickfix.CreateFieldFromUsageHelper;
import com.intellij.codeInsight.daemon.impl.quickfix.CreateFromUsageUtils;
import com.intellij.codeInsight.daemon.impl.quickfix.CreateVarFromUsageFix;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

@NonNls
public class CreateFieldMockQuickFix extends CreateVarFromUsageFix {

    public CreateFieldMockQuickFix(PsiReferenceExpression referenceElement) {
        super(referenceElement);
    }

    @Override
    public String getText(String varName) {
        return "Create Field Mock '" + varName + "'" ;
    }

    @Override
    protected boolean isAvailableImpl(int offset) {
        if (!super.isAvailableImpl(offset)) return false;
        if(myReferenceExpression.isQualified()) return false;
        PsiElement scope = PsiTreeUtil.getParentOfType(myReferenceExpression, PsiModifierListOwner.class);
        if (scope instanceof PsiAnonymousClass) {
            scope = PsiTreeUtil.getParentOfType(scope, PsiModifierListOwner.class, true);
        }
        // todo: check if mockito library is used in module
        return scope instanceof PsiMethod || scope instanceof PsiClassInitializer ||
                scope instanceof PsiLocalVariable;
    }

    @Override
    protected void invokeImpl(final PsiClass targetClass) {
        final Project project = myReferenceExpression.getProject();
        JVMElementFactory factory = JVMElementFactories.getFactory(targetClass.getLanguage(), project);
        if (factory == null) factory = JavaPsiFacade.getElementFactory(project);

        PsiMember enclosingContext = null;
        PsiClass parentClass;
        do {
            enclosingContext = PsiTreeUtil.getParentOfType(enclosingContext == null ? myReferenceExpression : enclosingContext, PsiMethod.class,
                    PsiField.class, PsiClassInitializer.class);
            parentClass = enclosingContext == null ? null : enclosingContext.getContainingClass();
        }
        while (parentClass instanceof PsiAnonymousClass);

        PsiType[] expectedTypes = CreateFromUsageUtils.guessType(myReferenceExpression, false);

        String fieldName = myReferenceExpression.getReferenceName();
        assert fieldName != null;

        PsiField field = factory.createField(fieldName, expectedTypes[0]);
        if (shouldCreateFinalMember(myReferenceExpression, targetClass)) {
            PsiUtil.setModifierProperty(field, PsiModifier.FINAL, true);
        }

        field.getModifierList().addAnnotation("org.mockito.Mock");
        field = CreateFieldFromUsageHelper.insertField(targetClass, field, myReferenceExpression);

        setupVisibility(parentClass, targetClass, field.getModifierList());
    }

    private static boolean shouldCreateFinalMember(@NotNull PsiReferenceExpression ref, @NotNull PsiClass targetClass) {
        if (!PsiTreeUtil.isAncestor(targetClass, ref, true)) {
            return false;
        }
        final PsiElement element = PsiTreeUtil.getParentOfType(ref, PsiClassInitializer.class, PsiMethod.class);
        if (element instanceof PsiClassInitializer) {
            return true;
        }

        if (element instanceof PsiMethod && ((PsiMethod)element).isConstructor()) {
            return true;
        }

        return false;
    }

    @Override
    protected boolean isAllowOuterTargetClass() {
        return false;
    }

    @Override
    @NotNull
    public String getFamilyName() {
        return QuickFixBundle.message("create.field.from.usage.family");

    }}
