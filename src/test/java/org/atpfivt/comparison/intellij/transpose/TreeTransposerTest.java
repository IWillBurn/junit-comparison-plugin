package org.atpfivt.comparison.intellij.transpose;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Replays logs of IntelliJ's JUnit runner (recorded by {@code capture-logs.sh}) through {@link TreeTransposer} into
 * {@link IdeTree}: the tree the IDE would show, and no protocol errors.
 */
class TreeTransposerTest {

    private static final String JUPITER = "[engine:junit-jupiter]/";

    @Test
    void mulTest() {
        IdeTree tree = replay("class-MulTest");
        assertEquals("""
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
                """, tree.render());
    }

    @Test
    void leavesKeepTheirIdsAndMergedNodesDropTheInvocation() {
        IdeTree tree = replay("class-MulTest");
        String merged = JUPITER + "[class-template:examples.MulTest]/[test-template:mul(int, int, int)]"
                + "/[test-template-invocation:#2]";
        String buggy = JUPITER + "[class-template:examples.MulTest]/[class-template-invocation:#2]"
                + "/[test-template:mul(int, int, int)]/[test-template-invocation:#2]";
        assertEquals("[2] 4, 5, 20", tree.nodes.get(merged).name);
        assertEquals("Buggy", tree.nodes.get(buggy).name);
        assertTrue(tree.nodes.get(merged).children.contains(tree.nodes.get(buggy)));
    }

    @Test
    void severalClassesCloseTheirMergedNodesBeforeTheClassFinishes() {
        IdeTree tree = replay("package-examples");
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
                  mulTwoByTwo()
                    Good: passed
                    Buggy: passed
                """, tree.render());
    }

    @Test
    void outputStaysInPlace() {
        List<String> original = lines("package-examples");
        List<String> transposed = transpose(original);
        List<String> outputBefore = original.stream().filter(l -> !ServiceMessage.isServiceMessage(l)).toList();
        List<String> outputAfter = transposed.stream().filter(l -> !ServiceMessage.isServiceMessage(l)).toList();
        assertEquals(outputBefore, outputAfter);
        // the output of a test stays between its testStarted and testFinished
        int line = transposed.indexOf("@BeforeEach  instance #4, Buggy");
        assertTrue(transposed.get(line - 1).startsWith("##teamcity[testStarted"));
        assertTrue(transposed.get(line - 1).contains("name='Buggy'"));
    }

    @Test
    void nestedClassStaysALevel() {
        assertEquals("""
                mulTwoByTwo()
                  Good: passed
                  Buggy: passed
                Inner
                  mulFourByFive()
                    Good: passed
                    Buggy: failed
                """, replay("class-NestedFixture").render());
    }

    @Test
    void nestedParameterizedClassBecomesALevelAboveTheImplementations() {
        assertEquals("""
                Inner
                  mulByFive()
                    factor 2
                      Good: passed
                      Buggy: passed
                    factor 4
                      Good: passed
                      Buggy: failed
                """, replay("class-NestedParameterizedFixture").render());
    }

    @Test
    void failedInvocationIsALeafUnderTheClass() {
        assertEquals("""
                test()
                  Good: passed
                Buggy: failed
                """, replay("class-InvocationFailureFixture").render());
    }

    @Test
    void skippedTestsAndContainers() {
        assertEquals("""
                enabled()
                  Good: passed
                  Buggy: passed
                disabled()
                  Good: skipped
                  Buggy: skipped
                Later
                  test()
                    Good: skipped
                    Buggy: skipped
                """, replay("class-DisabledFixture").render());
    }

    /** Known limitation: dynamic tests are paired by index, Buggy's only test was "4 * 5". */
    @Test
    void dynamicTestsArePairedByIndex() {
        assertEquals("""
                products()
                  2 * 2
                    Good: passed
                    Buggy: passed
                  4 * 5
                    Good: passed
                """, replay("class-DynamicFixture").render());
    }

    @Test
    void messagesWithoutInvocationsPassUnchanged() {
        String line = "##teamcity[testStarted id='|[engine:junit-jupiter|]/|[class:A|]/|[method:t()|]' name='t()' "
                + "nodeId='|[engine:junit-jupiter|]/|[class:A|]/|[method:t()|]' parentNodeId='0'locationHint='x']";
        assertEquals(List.of(line), new TreeTransposer(Marks.ALL).process(line));
        assertEquals(List.of("plain output"), new TreeTransposer(Marks.ALL).process("plain output"));
    }

    @Test
    void withoutMarksTheOutputIsTheRunnersOutput() {
        for (String log : List.of("package-examples", "class-NestedParameterizedFixture", "class-DisabledFixture",
            "class-InvocationFailureFixture", "class-DynamicFixture")) {
            assertEquals(normalized(lines(log)), normalized(transpose(lines(log), marked())), log);
        }
    }

    /** Service messages formatted alike: the runner writes some without a space between attributes. */
    private static List<String> normalized(List<String> lines) {
        return lines.stream().map(line -> {
            ServiceMessage message = ServiceMessage.parse(line);
            return message != null ? message.format() : line;
        }).toList();
    }

    @Test
    void onlyMarkedClassesAreTransposed() {
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
                  Good
                    mul(int, int, int)
                      [1] 2, 3, 6: passed
                      [2] 4, 5, 20: passed
                    mulTwoByTwo(): passed
                  Buggy
                    mul(int, int, int)
                      [1] 2, 3, 6: passed
                      [2] 4, 5, 20: failed
                    mulTwoByTwo(): passed
                """, replay("package-examples", marked("examples.MulTest")).render());
    }

    @Test
    void aMarkedTestIsTransposedAndTheOthersStay() {
        IdeTree tree = replay("class-MulTest", marked("examples.MulTest#mulFourByFive()"));
        assertEquals("""
                Good
                  mul(int, int, int)
                    [1] 2, 3, 6: passed
                    [2] 4, 5, 20: passed
                mulFourByFive()
                  Good: passed
                  Buggy: failed
                Buggy
                  mul(int, int, int)
                    [1] 2, 3, 6: passed
                    [2] 4, 5, 20: failed
                """, tree.render());
        assertTrue(tree.nodes.containsKey(JUPITER + "[class-template:examples.MulTest]/[method:mulFourByFive()]"));
    }

    @Test
    void aMarkedParameterizedTestIsFoundByItsParameterTypes() {
        assertEquals("""
                mul(int, int, int)
                  [1] 2, 3, 6
                    Good: passed
                    Buggy: passed
                  [2] 4, 5, 20
                    Good: passed
                    Buggy: failed
                Good
                  mulFourByFive(): passed
                Buggy
                  mulFourByFive(): failed
                """, replay("class-MulTest", marked("examples.MulTest#mul(int, int, int)")).render());
    }

    @Test
    void anUnmarkedNestedParameterizedClassStaysALevelBelowTheMarkedOne() {
        assertEquals("""
                Inner
                  factor 2
                    mulByFive()
                      Good: passed
                      Buggy: passed
                  factor 4
                    mulByFive()
                      Good: passed
                      Buggy: failed
                """, replay("class-NestedParameterizedFixture", marked("fixtures.NestedParameterizedFixture")).render());
    }

    @Test
    void onlyTheNestedParameterizedClassMarked() {
        assertEquals("""
                Good
                  Inner
                    mulByFive()
                      factor 2: passed
                      factor 4: passed
                Buggy
                  Inner
                    mulByFive()
                      factor 2: passed
                      factor 4: failed
                """, replay("class-NestedParameterizedFixture", marked("fixtures.NestedParameterizedFixture$Inner"))
                .render());
    }

    @Test
    void aMarkedNestedClass() {
        assertEquals("""
                Good
                  mulTwoByTwo(): passed
                Inner
                  mulFourByFive()
                    Good: passed
                    Buggy: failed
                Buggy
                  mulTwoByTwo(): passed
                """, replay("class-NestedFixture", marked("fixtures.NestedFixture$Inner")).render());
    }

    @Test
    void serviceMessageRoundTrip() {
        String line = "##teamcity[testFailed name='|[2|] it|'s' nodeId='a|nb' details='x||y']";
        ServiceMessage message = ServiceMessage.parse(line);
        assertNotNull(message);
        assertEquals(Map.of("name", "[2] it's", "nodeId", "a\nb", "details", "x|y"), message.attributes());
        assertEquals(line, message.format());
        assertEquals("rootName", ServiceMessage.parse("##teamcity[rootName name = 'MulTest' comment = 'e']").name());
    }

    private static IdeTree replay(String log) {
        return replay(log, Marks.ALL);
    }

    private static IdeTree replay(String log, Marks marks) {
        IdeTree tree = new IdeTree();
        transpose(lines(log), marks).forEach(tree::accept);
        assertEquals(List.of(), tree.errors);
        assertEquals(List.of(), tree.unfinished());
        return tree;
    }

    /** Marks by name: {@code a.Class} or {@code a.Class#method(types)} as in UniqueIds. */
    private static Marks marked(String... names) {
        Set<String> marked = Set.of(names);
        return new Marks() {

            @Override
            public boolean isMarked(String className) {
                return marked.contains(className);
            }

            @Override
            public boolean isMarked(String className, String method) {
                return marked.contains(className + "#" + method);
            }
        };
    }

    private static List<String> transpose(List<String> lines) {
        return transpose(lines, Marks.ALL);
    }

    private static List<String> transpose(List<String> lines, Marks marks) {
        TreeTransposer transposer = new TreeTransposer(marks);
        List<String> result = new java.util.ArrayList<>();
        lines.forEach(line -> result.addAll(transposer.process(line)));
        result.addAll(transposer.finish());
        return result;
    }

    private static List<String> lines(String log) {
        try (InputStream in = TreeTransposerTest.class.getResourceAsStream("/logs/" + log + ".txt")) {
            assertNotNull(in, log);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
