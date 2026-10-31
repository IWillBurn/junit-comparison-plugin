package org.atpfivt.comparison.intellij.configuration;

import com.intellij.execution.Executor;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.junit2.ui.properties.JUnitConsoleProperties;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.execution.testframework.sm.SMCustomMessagesParsing;
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter;
import org.jetbrains.annotations.NotNull;

/** The JUnit console properties plus the documented hook for a custom output parser. */
final class ComparisonConsoleProperties extends JUnitConsoleProperties implements SMCustomMessagesParsing {

    ComparisonConsoleProperties(JUnitConfiguration configuration, Executor executor) {
        super(configuration, executor);
    }

    @Override
    public OutputToGeneralTestEventsConverter createTestEventsConverter(@NotNull String testFrameworkName,
            @NotNull TestConsoleProperties consoleProperties) {
        return new TransposingEventsConverter(testFrameworkName, consoleProperties);
    }
}
