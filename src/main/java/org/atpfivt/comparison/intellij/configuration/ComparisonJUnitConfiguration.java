package org.atpfivt.comparison.intellij.configuration;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;

/** A JUnit configuration (same settings, same runner) whose test console transposes the tree. */
public final class ComparisonJUnitConfiguration extends JUnitConfiguration {

    public ComparisonJUnitConfiguration(String name, Project project, ConfigurationFactory factory) {
        super(name, project, factory);
    }

    @Override
    public @NotNull SMTRunnerConsoleProperties createTestConsoleProperties(@NotNull Executor executor) {
        SMTRunnerConsoleProperties standard = super.createTestConsoleProperties(executor);
        ComparisonConsoleProperties properties = new ComparisonConsoleProperties(this, executor);
        properties.setIdBasedTestTree(standard.isIdBasedTestTree());
        Disposer.dispose(standard);
        return properties;
    }
}
