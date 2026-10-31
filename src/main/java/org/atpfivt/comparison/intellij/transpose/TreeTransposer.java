package org.atpfivt.comparison.intellij.transpose;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rewrites the service messages of IntelliJ's JUnit 5/6 runner while they stream, so that the test tree shows
 * {@code class > method > [argument set] > implementation} instead of
 * {@code class > implementation > method > [argument set]} for the tests marked with {@code @ComparisonTree}
 * ({@link Marks}, {@link NodeId#implementations}).
 *
 * <p>Node ids are JUnit UniqueIds. A node below an implementation ({@code [class-template-invocation:#k]}) is merged
 * with the same node of the other implementations under the id without the invocation segment; Jupiter accepts that id
 * and runs the node in every invocation, so re-running a merged node from the IDE works. A test below an implementation
 * becomes a leaf named after the implementation; it keeps its real id, so re-running it runs exactly that test. Merged
 * nodes stay open until their class finishes (or the run ends): Jupiter runs all of invocation #1 before #2.
 *
 * <p>Nested {@code @ParameterizedClass}es, both marked: the inner implementation becomes a level above the outer one.
 * Unmarked tests keep Jupiter's tree. A suite below an invocation is shown or merged only once a test
 * or output needs it, so a suite whose tests all went elsewhere does not stay behind empty. Messages that do not
 * involve invocations pass through unchanged. Not thread-safe; the runner prints sequentially.
 */
public final class TreeTransposer {

    private static final String ROOT = "0";

    private record Merged(NodeId id, String name, String parent) {
    }

    private record Leaf(String name, String parent) {
    }

    private final Marks marks;
    /** Every suite below an invocation (invocations included) -> its testSuiteStarted, printed only when needed. */
    private final Map<String, ServiceMessage> suites = new HashMap<>();
    /** The suites of {@link #suites} that keep their place in the tree and have been printed. */
    private final Set<String> shown = new HashSet<>();
    /** The nodes running in the original tree, innermost last. */
    private final List<String> running = new ArrayList<>();
    /** Id of a transposed test -> its name and parent in the transposed tree. */
    private final Map<String, Leaf> leaves = new HashMap<>();
    /** Merged nodes started and not finished yet, in the order they were started. */
    private final Map<String, Merged> open = new LinkedHashMap<>();

    public TreeTransposer(Marks marks) {
        this.marks = marks;
    }

    /** The lines to print instead of the given one; unrelated messages are returned unchanged. */
    public List<String> process(String line) {
        ServiceMessage message = ServiceMessage.parse(line);
        if (message == null) {
            List<String> result = new ArrayList<>(beforeOutput());
            result.add(line);
            return result;
        }
        String id = id(message);
        List<ServiceMessage> result = switch (message.name()) {
            case "testSuiteStarted" -> suiteStarted(message, id);
            case "testSuiteFinished" -> suiteFinished(message, id);
            case "testStarted" -> testStarted(message, id);
            case "testFinished", "testFailed", "testIgnored", "testStdOut", "testStdErr" -> testEvent(message, id);
            default -> null;
        };
        switch (message.name()) {
            case "testSuiteStarted", "testStarted" -> running.add(id);
            case "testSuiteFinished", "testFinished" -> {
                int index = running.lastIndexOf(id);
                if (index >= 0) {
                    running.remove(index);
                }
            }
            default -> {
            }
        }
        return result == null ? List.of(line) : format(result);
    }

    /**
     * The messages to print before output that is not a service message. The IDE gives such output to the running
     * node, so the innermost running suite gets its place in the tree first.
     */
    public List<String> beforeOutput() {
        String id = running.isEmpty() ? null : running.getLast();
        ServiceMessage suite = id == null ? null : suites.get(id);
        if (suite == null) {
            return List.of();
        }
        NodeId nodeId = NodeId.parse(id);
        List<Integer> implementations = nodeId.implementations(marks);
        List<ServiceMessage> result = new ArrayList<>();
        if (!nodeId.isImplementation(implementations)) {
            place(nodeId, suite, implementations, result);
        }
        return format(result);
    }

    /** Finishes the merged nodes that are still open; call when the output ends. */
    public List<String> finish() {
        List<String> result = new ArrayList<>();
        for (Merged merged : List.copyOf(open.values()).reversed()) {
            result.add(finished(merged).format());
        }
        open.clear();
        return result;
    }

    private List<ServiceMessage> suiteStarted(ServiceMessage message, String id) {
        NodeId nodeId = NodeId.parse(id);
        if (nodeId == null || nodeId.invocations().isEmpty()) {
            return null;
        }
        suites.put(id, message);
        return List.of(); // see place()
    }

    private List<ServiceMessage> suiteFinished(ServiceMessage message, String id) {
        if (suites.containsKey(id) && !shown.contains(id)) {
            return List.of(); // never printed, or merged: merged nodes finish with their class
        }
        NodeId nodeId = NodeId.parse(id);
        if (nodeId == null) {
            return null;
        }
        List<ServiceMessage> result = new ArrayList<>();
        for (Merged merged : List.copyOf(open.values()).reversed()) {
            if (merged.id().isDescendantOf(nodeId)) {
                result.add(finished(merged));
                open.remove(merged.id().text());
            }
        }
        result.add(message);
        return result;
    }

    private List<ServiceMessage> testStarted(ServiceMessage message, String id) {
        NodeId nodeId = NodeId.parse(id);
        if (nodeId == null || nodeId.invocations().isEmpty()) {
            return null;
        }
        List<Integer> implementations = nodeId.implementations(marks);
        List<ServiceMessage> result = new ArrayList<>();
        if (implementations.isEmpty()) {
            show(nodeId, result);
            result.add(message);
            return result;
        }
        // the runner reports a failed container as a "Class Configuration" test with the container's id
        ServiceMessage suite = suites.get(id);
        String name;
        String parent;
        if (suite != null && nodeId.isImplementation(implementations)) {
            name = suite.attribute("name");
            parent = placeParent(suite, implementations, result);
        }
        else {
            name = leafName(nodeId, implementations);
            parent = chain(nodeId, suite != null ? suite : message, implementations, result);
        }
        leaves.put(id, new Leaf(name, parent));
        result.add(message.with("name", name).with("parentNodeId", parent));
        return result;
    }

    private List<ServiceMessage> testEvent(ServiceMessage message, String id) {
        Leaf leaf = leaves.get(id);
        if (leaf != null) {
            return List.of(message.with("name", leaf.name()).with("parentNodeId", leaf.parent()));
        }
        NodeId nodeId = NodeId.parse(id);
        if (nodeId == null || nodeId.invocations().isEmpty()) {
            return null;
        }
        List<Integer> implementations = nodeId.implementations(marks);
        List<ServiceMessage> result = new ArrayList<>();
        if (implementations.isEmpty()) {
            show(nodeId, result);
            result.add(message);
            return result;
        }
        ServiceMessage suite = suites.get(id);
        boolean output = message.name().startsWith("testStd");
        if (suite == null || !output && !message.name().equals("testIgnored")) {
            return null;
        }
        boolean implementation = nodeId.isImplementation(implementations);
        String target = implementation
                ? placeParent(suite, implementations, result)
                : place(nodeId, suite, implementations, result);
        if (output) {
            result.add(retarget(message, target)); // e.g. a report entry of a container
            return result;
        }
        // a container skipped (or aborted) as a whole: one leaf for this implementation
        String name = implementation ? suite.attribute("name") : leafName(nodeId, implementations);
        leaves.put(id, new Leaf(name, target));
        result.add(new ServiceMessage("testStarted", Map.of()).with("id", id).with("name", name).with("nodeId", id)
                .with("parentNodeId", target));
        result.add(message.with("name", name).with("parentNodeId", target));
        result.add(new ServiceMessage("testFinished", Map.of()).with("id", id).with("name", name).with("nodeId", id)
                .with("parentNodeId", target));
        return result;
    }

    /**
     * Opens the merged node of the test and, for nested implementations, one level per inner one; returns the parent
     * of the leaf.
     */
    private String chain(NodeId test, ServiceMessage source, List<Integer> implementations,
            List<ServiceMessage> result) {
        String parent = place(test, source, implementations, result);
        for (int level = implementations.size() - 1; level > 0; level--) {
            String id = test.without(implementations.subList(0, level));
            open(id, source, invocationName(test, implementations.get(level)), parent, result);
            parent = id;
        }
        return parent;
    }

    /**
     * The id in the transposed tree of a node above a leaf with the given implementations: the node itself if no
     * implementation is above it (printed now if it is a suite), else its merged node, opened on first use. An
     * implementation has no node of its own, so it is its parent's.
     */
    private String place(NodeId node, ServiceMessage source, List<Integer> implementations,
            List<ServiceMessage> result) {
        List<Integer> above = implementations.stream().filter(p -> p < node.segments().size()).toList();
        if (above.isEmpty()) {
            show(node, result);
            return node.text();
        }
        String parent = placeParent(source, implementations, result);
        if (node.isImplementation(above)) {
            return parent;
        }
        String merged = node.without(above);
        open(merged, source, source.attribute("name"), parent, result);
        return merged;
    }

    private String placeParent(ServiceMessage source, List<Integer> implementations, List<ServiceMessage> result) {
        String parent = source.attribute("parentNodeId");
        if (parent == null) {
            return ROOT;
        }
        ServiceMessage suite = suites.get(parent);
        return suite == null
                ? parent // not below an invocation, or a leaf (a test with children)
                : place(NodeId.parse(parent), suite, implementations, result);
    }

    /** Prints the suites of the node's path (the node too, if it is one) that keep their place, outermost first. */
    private void show(NodeId node, List<ServiceMessage> result) {
        for (int i = 0; i < node.segments().size(); i++) {
            String id = node.prefix(i);
            ServiceMessage suite = suites.get(id);
            if (suite != null && shown.add(id)) {
                result.add(suite);
            }
        }
    }

    private void open(String id, ServiceMessage source, String name, String parent, List<ServiceMessage> result) {
        if (open.containsKey(id)) {
            return;
        }
        open.put(id, new Merged(NodeId.parse(id), name, parent));
        ServiceMessage started = new ServiceMessage("testSuiteStarted", Map.of())
                .with("id", id).with("name", name).with("nodeId", id).with("parentNodeId", parent);
        for (String key : List.of("locationHint", "metainfo")) {
            if (source.attribute(key) != null) {
                started = started.with(key, source.attribute(key));
            }
        }
        result.add(started);
    }

    private static ServiceMessage finished(Merged merged) {
        String id = merged.id().text();
        return new ServiceMessage("testSuiteFinished", Map.of())
                .with("id", id).with("name", merged.name()).with("nodeId", id).with("parentNodeId", merged.parent());
    }

    /** A leaf is named after the outermost implementation; the inner ones are levels above it. */
    private String leafName(NodeId nodeId, List<Integer> implementations) {
        return invocationName(nodeId, implementations.getFirst());
    }

    private String invocationName(NodeId nodeId, int position) {
        ServiceMessage invocation = suites.get(nodeId.prefix(position));
        return invocation != null ? invocation.attribute("name") : nodeId.segments().get(position);
    }

    private static ServiceMessage retarget(ServiceMessage message, String id) {
        ServiceMessage result = message;
        for (String key : List.of("id", "nodeId")) {
            if (message.attribute(key) != null) {
                result = result.with(key, id);
            }
        }
        return result;
    }

    private static List<String> format(List<ServiceMessage> messages) {
        return messages.stream().map(ServiceMessage::format).toList();
    }

    private static String id(ServiceMessage message) {
        String nodeId = message.attribute("nodeId");
        return nodeId != null ? nodeId : message.attribute("id");
    }
}
