package com.github.argast.quickfixes;

import com.github.argast.MockitoPluginUtils;
import com.intellij.codeInsight.daemon.QuickFixBundle;
import com.intellij.codeInsight.daemon.impl.quickfix.CreateFromUsageUtils;
import com.intellij.codeInsight.daemon.impl.quickfix.CreateVarFromUsageFix;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

@NonNls
public class CreateLocalMockQuickFix extends CreateVarFromUsageFix {

    private static final Logger LOG = Logger.getInstance("#com.github.argast.quickfixes.CreateLocalMockQuickFix");

    public CreateLocalMockQuickFix(PsiReferenceExpression referenceElement) {
        super(referenceElement);
    }

    @Override
    public String getText(String varName) {
        return "Create Inline Mock '" + varName + "'" ;
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
        return scope instanceof PsiLocalVariable;
    }

    @Override
    protected void invokeImpl(final PsiClass targetClass) {
        if (CreateFromUsageUtils.isValidReference(myReferenceExpression, false)) {
            return;
        }
        // todo: check if mock is imported, add import otherwise

        final Project project = myReferenceExpression.getProject();
        PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();

        final PsiFile targetFile = targetClass.getContainingFile();

        MockitoPluginUtils.containsMockImport((PsiJavaFile) targetFile);

        PsiType[] expectedTypes = CreateFromUsageUtils.guessType(myReferenceExpression, false);
        PsiType type = expectedTypes[0];

        String varName = myReferenceExpression.getReferenceName();

        PsiExpression[] expressions = CreateFromUsageUtils.collectExpressions(myReferenceExpression, PsiMember.class, PsiFile.class);
        PsiStatement anchor = getAnchor(expressions);

        PsiExpression initializer = factory.createExpressionFromText("mock(" + type.getCanonicalText() + ".class)", null);
        PsiDeclarationStatement decl = factory.createVariableDeclarationStatement(varName, type, initializer);

        PsiVariable var = (PsiVariable)decl.getDeclaredElements()[0];
        boolean isFinal =
                CodeStyleSettingsManager.getSettings(project).GENERATE_FINAL_LOCALS && !CreateFromUsageUtils.isAccessedForWriting(expressions);
        PsiUtil.setModifierProperty(var, PsiModifier.FINAL, isFinal);

        anchor.getParent().addBefore(decl, anchor);
    }

    @Override
    protected boolean isAllowOuterTargetClass() {
        return false;
    }

    private static PsiStatement getAnchor(PsiExpression[] expressionOccurences) {
        PsiElement parent = expressionOccurences[0];
        int minOffset = expressionOccurences[0].getTextRange().getStartOffset();
        for (int i = 1; i < expressionOccurences.length; i++) {
            parent = PsiTreeUtil.findCommonParent(parent, expressionOccurences[i]);
            LOG.assertTrue(parent != null);
            minOffset = Math.min(minOffset, expressionOccurences[i].getTextRange().getStartOffset());
        }

        final PsiCodeBlock block = PsiTreeUtil.getParentOfType(parent, PsiCodeBlock.class, false);
        LOG.assertTrue(block != null && block.getStatements().length > 0, "block: " + block +"; parent: " + parent);
        PsiStatement[] statements = block.getStatements();
        for (int i = 1; i < statements.length; i++) {
            if (statements[i].getTextRange().getStartOffset() > minOffset) return statements[i-1];
        }
        return statements[statements.length - 1];
    }

    @Override
    @NotNull
    public String getFamilyName() {
        return QuickFixBundle.message("create.local.from.usage.family");
    }}
