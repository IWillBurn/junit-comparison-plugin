package org.atpfivt.comparison.intellij;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.swing.Icon;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.NopProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.testframework.sm.runner.GeneralIdBasedToSMTRunnerEventsConvertor;
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter;
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import org.atpfivt.comparison.intellij.annotation.AnnotatedMarks;
import org.atpfivt.comparison.intellij.configuration.TransposingEventsConverter;
import org.atpfivt.comparison.intellij.console.TransposingProcessHandler;
import org.jetbrains.annotations.NotNull;

/**
 * The recorded runner output through IntelliJ's own parser ({@link OutputToGeneralTestEventsConverter}) and tree
 * builder ({@link GeneralIdBasedToSMTRunnerEventsConvertor}), the classes behind the test results tree: once as
 * approach (a) wires them, once as approach (b) does. The marks come from the code of the project, like in a run:
 * {@code MulTest} is marked, of {@code LifecycleTest} only the method {@code mul}.
 */
public class IdeLevelTest extends JavaProjectTestCase {

    private static final String MUL_TEST = """
            mul(int, int, int)
              [1] 2, 3, 6
                Good: passed
                Buggy: passed
              [2] 4, 5, 20
                Good: passed
                Buggy: failed
            mulFourByFive()
              Good: passed
              Buggy: failed
            """;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.addClass("package examples; @org.atpfivt.comparison.ComparisonTree class MulTest {}");
        myFixture.addClass("""
                package examples;

                import org.atpfivt.comparison.ComparisonTree;

                class LifecycleTest {
                    @ComparisonTree
                    void mul(int a, int b, int expected) {
                    }

                    void mulTwoByTwo() {
                    }
                }
                """);
        myFixture.addClass("""
                package examples;

                class MarkedTestTest {
                    void mulTwoByTwo() {
                    }

                    @org.atpfivt.comparison.ComparisonTree
                    void mulFourByFive() {
                    }
                }
                """);
    }

    public void testWithoutThePluginTheImplementationIsOnTop() {
        SMTestProxy.SMRootTestProxy root = new SMTestProxy.SMRootTestProxy();
        OutputToGeneralTestEventsConverter converter = connect(new OutputToGeneralTestEventsConverter("JUnit",
            properties()), root);
        feedLines(converter, lines("class-MulTest"));
        assertEquals("""
                Good
                  mul(int, int, int)
                    [1] 2, 3, 6: passed
                    [2] 4, 5, 20: passed
                  mulFourByFive(): passed
                Buggy
                  mul(int, int, int)
                    [1] 2, 3, 6: passed
                    [2] 4, 5, 20: failed
                  mulFourByFive(): failed
                """, render(root));
    }

    /** Approach (a): the console of the own configuration type parses with {@link TransposingEventsConverter}. */
    public void testOwnConfigurationType() {
        SMTestProxy.SMRootTestProxy root = new SMTestProxy.SMRootTestProxy();
        feedLines(connect(new TransposingEventsConverter("JUnit", properties()), root), lines("class-MulTest"));
        assertEquals(MUL_TEST, render(root));
        assertRerunIds(root);
    }

    /** Only the marked test moves; the merged node re-runs it with every implementation. */
    public void testMarkedTest() {
        SMTestProxy.SMRootTestProxy root = new SMTestProxy.SMRootTestProxy();
        feedLines(connect(new TransposingEventsConverter("JUnit", properties()), root), lines("class-MarkedTestTest"));
        assertEquals("""
                Good
                  mulTwoByTwo(): passed
                mulFourByFive()
                  Good: passed
                  Buggy: failed
                Buggy
                  mulTwoByTwo(): passed
                """, render(root));
        assertEquals("[engine:junit-jupiter]/[class-template:examples.MarkedTestTest]/[method:mulFourByFive()]",
            root.getChildren().get(1).getUserData(SMTestProxy.NODE_ID));
        assertTrue(root.isDefect());
    }

    /** Approach (b): the stock parser reads a transposed copy of the process; the output comes in odd chunks. */
    public void testDecoratedConsole() {
        SMTestProxy.SMRootTestProxy root = new SMTestProxy.SMRootTestProxy();
        OutputToGeneralTestEventsConverter converter = connect(new OutputToGeneralTestEventsConverter("JUnit",
            properties()), root);
        NopProcessHandler process = new NopProcessHandler();
        TransposingProcessHandler copy = new TransposingProcessHandler(process, new AnnotatedMarks(getProject()));
        copy.addProcessListener(new ProcessListener() {
            @Override
            public void startNotified(@NotNull ProcessEvent event) {
                converter.startTesting();
            }

            @Override
            public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                converter.process(event.getText(), outputType);
            }

            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                converter.flushBufferOnProcessTermination(event.getExitCode());
                converter.finishTesting();
            }
        });
        process.startNotify();
        String output = String.join("\r\n", lines("package-examples")) + "\r\n";
        for (int start = 0; start < output.length(); start += 37) {
            process.notifyTextAvailable(output.substring(start, Math.min(output.length(), start + 37)),
                ProcessOutputTypes.STDOUT);
        }
        process.destroyProcess();
        process.waitFor();
        assertTrue(copy.isProcessTerminated());
        assertEquals("""
                MulTest
                  mul(int, int, int)
                    [1] 2, 3, 6
                      Good: passed
                      Buggy: passed
                    [2] 4, 5, 20
                      Good: passed
                      Buggy: failed
                  mulFourByFive()
                    Good: passed
                    Buggy: failed
                LifecycleTest
                  mul(int, int, int)
                    [1] 2, 3, 6
                      Good: passed
                      Buggy: passed
                    [2] 4, 5, 20
                      Good: passed
                      Buggy: failed
                  Good
                    mulTwoByTwo(): passed
                  Buggy
                    mulTwoByTwo(): passed
                """, render(root));
    }

    /** What the IDE re-runs (it passes the node id as a UniqueId): leaves exactly, merged nodes in every invocation. */
    private static void assertRerunIds(SMTestProxy.SMRootTestProxy root) {
        SMTestProxy argumentSet = root.getChildren().getFirst().getChildren().get(1);
        SMTestProxy buggy = argumentSet.getChildren().get(1);
        assertEquals("[engine:junit-jupiter]/[class-template:examples.MulTest]/[test-template:mul(int, int, int)]"
                + "/[test-template-invocation:#2]", argumentSet.getUserData(SMTestProxy.NODE_ID));
        assertEquals("[engine:junit-jupiter]/[class-template:examples.MulTest]/[class-template-invocation:#2]"
                + "/[test-template:mul(int, int, int)]/[test-template-invocation:#2]", buggy.getUserData(SMTestProxy.NODE_ID));
    }

    private OutputToGeneralTestEventsConverter connect(OutputToGeneralTestEventsConverter converter,
            SMTestProxy.SMRootTestProxy root) {
        // as SMTestRunnerConnectionUtil does
        converter.setTestingStartedHandler(() -> converter.setProcessor(
            new GeneralIdBasedToSMTRunnerEventsConvertor(getProject(), root, "JUnit")));
        converter.setupProcessor();
        Disposer.register(getTestRootDisposable(), converter);
        return converter;
    }

    private static void feedLines(OutputToGeneralTestEventsConverter converter, List<String> lines) {
        converter.startTesting();
        lines.forEach(line -> converter.process(line + "\n", ProcessOutputTypes.STDOUT));
        converter.flushBufferOnProcessTermination(0);
        converter.finishTesting();
    }

    private SMTRunnerConsoleProperties properties() {
        SMTRunnerConsoleProperties properties = new SMTRunnerConsoleProperties(getProject(), new RunProfile() {
            @Override
            public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
                return null;
            }

            @Override
            public @NotNull String getName() {
                return "replay";
            }

            @Override
            public Icon getIcon() {
                return null;
            }
        }, "JUnit", DefaultRunExecutor.getRunExecutorInstance());
        properties.setIdBasedTestTree(true);
        Disposer.register(getTestRootDisposable(), properties);
        return properties;
    }

    private static String render(SMTestProxy root) {
        StringBuilder sb = new StringBuilder();
        root.getChildren().forEach(child -> render(child, 0, sb));
        return sb.toString();
    }

    private static void render(SMTestProxy node, int depth, StringBuilder sb) {
        assertFalse(node.getName() + " still running", node.isInProgress());
        sb.append("  ".repeat(depth)).append(node.getName());
        if (!node.isSuite()) {
            sb.append(": ").append(node.isDefect() ? "failed" : node.isIgnored() ? "skipped" : "passed");
        }
        sb.append('\n');
        node.getChildren().forEach(child -> render(child, depth + 1, sb));
    }

    private static List<String> lines(String log) {
        try (InputStream in = IdeLevelTest.class.getResourceAsStream("/logs/" + log + ".txt")) {
            assertNotNull(log, in);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
