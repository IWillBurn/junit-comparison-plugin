package org.atpfivt.comparison.intellij;

import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.execution.application.JavaConsoleDecorator;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.testframework.sm.SMCustomMessagesParsing;
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil;
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.atpfivt.comparison.intellij.configuration.ComparisonConfigurationProducer;
import org.atpfivt.comparison.intellij.configuration.ComparisonConfigurationType;
import org.atpfivt.comparison.intellij.configuration.ComparisonJUnitConfiguration;
import org.atpfivt.comparison.intellij.configuration.TransposingEventsConverter;

/** The plugin is registered in the IDE and each approach hooks only where it should. */
public class WiringTest extends BasePlatformTestCase {

    private static final String SWITCH = "org.atpfivt.comparison.tree.junit";

    public void testOwnConfigurationTypeParsesWithTheTransposingConverter() {
        ComparisonJUnitConfiguration configuration = comparisonConfiguration();
        SMTRunnerConsoleProperties properties =
            configuration.createTestConsoleProperties(DefaultRunExecutor.getRunExecutorInstance());
        Disposer.register(getTestRootDisposable(), properties);
        // JUnit decides by the JUnit version of the module; this light project has none, so JUnit 4 (not id-based)
        SMTRunnerConsoleProperties junit = new JUnitConfiguration("plain", getProject())
            .createTestConsoleProperties(DefaultRunExecutor.getRunExecutorInstance());
        Disposer.register(getTestRootDisposable(), junit);
        assertEquals(junit.isIdBasedTestTree(), properties.isIdBasedTestTree());
        SMCustomMessagesParsing parsing = assertInstanceOf(properties, SMCustomMessagesParsing.class);
        assertInstanceOf(parsing.createTestEventsConverter("JUnit", properties), TransposingEventsConverter.class);
        assertNotNull(RunConfigurationProducer.getInstance(ComparisonConfigurationProducer.class));
    }

    public void testDecoratorWrapsOnlyOrdinaryJUnitConsolesWhileSwitchedOn() {
        JUnitConfiguration junit = new JUnitConfiguration("plain", getProject());
        assertTrue(decorate(junit).getClass().getSimpleName().equals("TransposingConsoleView"));
        assertFalse(decorate(comparisonConfiguration()).getClass().getSimpleName().equals("TransposingConsoleView"));
        PropertiesComponent.getInstance().setValue(SWITCH, false, true);
        try {
            assertFalse(decorate(junit).getClass().getSimpleName().equals("TransposingConsoleView"));
        }
        finally {
            PropertiesComponent.getInstance().unsetValue(SWITCH);
        }
    }

    public void testSwitchIsInTheRunMenu() {
        ActionManager actions = ActionManager.getInstance();
        assertNotNull(actions.getAction("org.atpfivt.comparison.tree.Toggle"));
        AnAction[] runMenu = ((DefaultActionGroup) actions.getAction("RunMenu")).getChildActionsOrStubs();
        assertEquals("org.atpfivt.comparison.tree.Toggle", actions.getId(runMenu[runMenu.length - 1])); // the last item
    }

    private ConsoleView decorate(JUnitConfiguration configuration) {
        SMTRunnerConsoleProperties properties = new SMTRunnerConsoleProperties(configuration, "JUnit",
            DefaultRunExecutor.getRunExecutorInstance());
        ConsoleView console = (ConsoleView) SMTestRunnerConnectionUtil.createConsole("JUnit", properties);
        Disposer.register(getTestRootDisposable(), console);
        return JavaConsoleDecorator.decorate(console, configuration, DefaultRunExecutor.getRunExecutorInstance());
    }

    private ComparisonJUnitConfiguration comparisonConfiguration() {
        ComparisonConfigurationType type = ConfigurationTypeUtil.findConfigurationType(ComparisonConfigurationType.class);
        return (ComparisonJUnitConfiguration) type.getConfigurationFactories()[0].createTemplateConfiguration(getProject());
    }
}
