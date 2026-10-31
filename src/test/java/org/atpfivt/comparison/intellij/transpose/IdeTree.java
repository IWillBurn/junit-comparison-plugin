package org.atpfivt.comparison.intellij.transpose;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A model of the id-based tree IntelliJ builds from the service messages: a node is attached to {@code parentNodeId},
 * which must exist and still run. It records protocol errors (unknown parent, duplicate id, finishing a suite with a
 * running child...) instead of guessing, and renders the tree as text.
 */
final class IdeTree {

    static final class Node {
        final String id;
        final String name;
        final boolean suite;
        final List<Node> children = new ArrayList<>();
        boolean finished;
        String status = "passed";

        Node(String id, String name, boolean suite) {
            this.id = id;
            this.name = name;
            this.suite = suite;
        }
    }

    final Node root = new Node("0", "root", true);
    final Map<String, Node> nodes = new HashMap<>(Map.of("0", root));
    final List<String> errors = new ArrayList<>();
    /** Lines that are not service messages, in order. */
    final List<String> output = new ArrayList<>();

    void accept(String line) {
        ServiceMessage message = ServiceMessage.parse(line);
        if (message == null) {
            if (!ServiceMessage.isServiceMessage(line)) {
                output.add(line);
            }
            return;
        }
        String id = message.attribute("nodeId");
        switch (message.name()) {
            case "testSuiteStarted", "testStarted" -> start(message, id);
            case "testFailed" -> running(id).status = "failed";
            case "testIgnored" -> running(id).status = "skipped";
            case "testFinished", "testSuiteFinished" -> finish(running(id));
            default -> {
            }
        }
    }

    private void start(ServiceMessage message, String id) {
        Node parent = nodes.get(message.attribute("parentNodeId"));
        if (parent == null || parent.finished) {
            errors.add("parent not running: " + message.format());
            return;
        }
        if (nodes.containsKey(id)) {
            errors.add("duplicate id: " + id);
            return;
        }
        Node node = new Node(id, message.attribute("name"), message.name().equals("testSuiteStarted"));
        parent.children.add(node);
        nodes.put(id, node);
    }

    private Node running(String id) {
        Node node = nodes.get(id);
        if (node == null || node.finished) {
            errors.add("not running: " + id);
            return new Node(id, "?", false);
        }
        return node;
    }

    private void finish(Node node) {
        for (Node child : node.children) {
            if (!child.finished) {
                errors.add("finished before its child: " + node.id + " / " + child.id);
            }
        }
        node.finished = true;
    }

    /** Every node finished. */
    List<String> unfinished() {
        return nodes.values().stream().filter(n -> n != root && !n.finished).map(n -> n.id).toList();
    }

    /** {@code name} per line, indented by two spaces per level; tests get {@code : status}. */
    String render() {
        StringBuilder sb = new StringBuilder();
        render(root, -1, sb);
        return sb.toString();
    }

    private static void render(Node node, int depth, StringBuilder sb) {
        if (depth >= 0) {
            sb.append("  ".repeat(depth)).append(node.name);
            if (!node.suite) {
                sb.append(": ").append(node.status);
            }
            sb.append('\n');
        }
        node.children.forEach(child -> render(child, depth + 1, sb));
    }
}
