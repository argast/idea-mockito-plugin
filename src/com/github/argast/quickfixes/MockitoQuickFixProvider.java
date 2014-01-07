package com.github.argast.quickfixes;

import com.intellij.codeInsight.daemon.QuickFixActionRegistrar;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightMethodUtil;
import com.intellij.codeInsight.quickfix.UnresolvedReferenceQuickFixProvider;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiReferenceExpression;
import org.jetbrains.annotations.NotNull;

public class MockitoQuickFixProvider extends UnresolvedReferenceQuickFixProvider<PsiJavaCodeReferenceElement> {

    @Override
    public void registerFixes(@NotNull PsiJavaCodeReferenceElement ref, @NotNull QuickFixActionRegistrar registrar) {
        if (ref instanceof PsiReferenceExpression) {
            TextRange fixRange = HighlightMethodUtil.getFixRange(ref);
            PsiReferenceExpression refExpr = (PsiReferenceExpression)ref;
            registrar.register(fixRange, new CreateLocalMockQuickFix(refExpr), null);
            registrar.register(fixRange, new CreateFieldMockQuickFix(refExpr), null);
        }
    }

    @NotNull
    @Override
    public Class getReferenceClass() {
        return PsiJavaCodeReferenceElement.class;
    }
}

