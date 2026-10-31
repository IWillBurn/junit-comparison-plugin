package org.atpfivt.comparison.intellij.console;

import com.intellij.execution.console.ConsoleViewWrapperBase;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.RunContentActionsContributor;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.actionSystem.AnAction;
import org.atpfivt.comparison.intellij.transpose.Marks;
import org.jetbrains.annotations.NotNull;

/**
 * The test console, attached to a transposing copy of the process instead of the process itself. The test console
 * contributes its actions to the run tab ({@link RunContentActionsContributor}); the wrapper passes them through.
 */
final class TransposingConsoleView extends ConsoleViewWrapperBase implements RunContentActionsContributor {

    private final Marks marks;

    TransposingConsoleView(@NotNull ConsoleView delegate, @NotNull Marks marks) {
        super(delegate);
        this.marks = marks;
    }

    @Override
    public void attachToProcess(@NotNull ProcessHandler processHandler) {
        super.attachToProcess(new TransposingProcessHandler(processHandler, marks));
    }

    @Override
    public AnAction @NotNull [] getActions() {
        return getDelegate() instanceof RunContentActionsContributor c ? c.getActions() : AnAction.EMPTY_ARRAY;
    }

    @Override
    public AnAction @NotNull [] getAdditionalActions() {
        return getDelegate() instanceof RunContentActionsContributor c ? c.getAdditionalActions() : AnAction.EMPTY_ARRAY;
    }

    @Override
    public void hideOriginalActions() {
        if (getDelegate() instanceof RunContentActionsContributor c) {
            c.hideOriginalActions();
        }
    }
}
