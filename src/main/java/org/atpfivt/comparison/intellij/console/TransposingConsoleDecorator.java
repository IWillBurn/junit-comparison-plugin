package org.atpfivt.comparison.intellij.console;

import com.intellij.execution.Executor;
import com.intellij.execution.application.JavaConsoleDecorator;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView;
import com.intellij.execution.ui.ConsoleView;
import org.atpfivt.comparison.intellij.annotation.AnnotatedMarks;
import org.atpfivt.comparison.intellij.configuration.ComparisonJUnitConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * Approach (b): the ordinary JUnit configurations (gutter icons included). The IDE lets decorators wrap the test
 * console before it attaches the console to the process, so the console reads a transposed copy of the process
 * output; only marked tests change their place. Switched on and off for all JUnit runs by
 * {@link ToggleComparisonTreeAction}.
 */
public final class TransposingConsoleDecorator extends JavaConsoleDecorator {

    @Override
    protected @NotNull ConsoleView decorateConsole(@NotNull ConsoleView console,
            @NotNull RunConfigurationBase<?> configuration, @NotNull Executor executor) {
        boolean junit = configuration instanceof JUnitConfiguration
                && !(configuration instanceof ComparisonJUnitConfiguration); // approach (a) transposes by itself
        return junit && console instanceof SMTRunnerConsoleView && ComparisonTreeSettings.isEnabled()
                ? new TransposingConsoleView(console, new AnnotatedMarks(configuration.getProject()))
                : console;
    }
}
