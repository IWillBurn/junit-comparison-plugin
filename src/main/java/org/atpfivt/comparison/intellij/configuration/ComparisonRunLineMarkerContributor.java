package org.atpfivt.comparison.intellij.configuration;

import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.testIntegration.TestRunLineMarkerProvider;
import org.atpfivt.comparison.intellij.annotation.AnnotatedMarks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Adds {@link RunWithComparisonTreeAction} to the gutter icon of the test classes and methods that concern
 * {@code @ComparisonTree} ({@link AnnotatedMarks#concerns}): wherever the IDE's own test marker is, the IDE merges both
 * into one icon whose menu ends with "Run '... (comparison tree)'".
 */
public final class ComparisonRunLineMarkerContributor extends RunLineMarkerContributor {

    private final TestRunLineMarkerProvider tests = new TestRunLineMarkerProvider();

    @Override
    public @Nullable Info getInfo(@NotNull PsiElement element) {
        if (tests.getInfo(element) == null || !(element.getParent() instanceof PsiMember member)
                || !AnnotatedMarks.concerns(member)) {
            return null;
        }
        AnAction run = ActionManager.getInstance().getAction(RunWithComparisonTreeAction.ID);
        return new Info(AllIcons.RunConfigurations.TestState.Run, new AnAction[] { run },
            e -> "Run with comparison tree");
    }
}
