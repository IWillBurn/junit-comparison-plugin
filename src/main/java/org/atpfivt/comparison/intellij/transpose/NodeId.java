package org.atpfivt.comparison.intellij.transpose;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A node id of IntelliJ's JUnit runner: a JUnit UniqueId ({@code [type:value]/[type:value]...}), optionally followed
 * by a suffix the runner appends in forked runs. Values never contain {@code ]}: JUnit encodes it.
 */
record NodeId(List<String> segments, String suffix) {

    static final String INVOCATION = "[class-template-invocation:";

    /** The parsed id, or null for ids that are not UniqueIds (e.g. the root "0"). */
    static NodeId parse(String text) {
        if (text == null) {
            return null;
        }
        List<String> segments = new ArrayList<>();
        int pos = 0;
        while (pos < text.length() && text.charAt(pos) == '[') {
            int close = text.indexOf(']', pos);
            if (close < 0) {
                return null;
            }
            segments.add(text.substring(pos, close + 1));
            pos = close + 1;
            if (!text.startsWith("/[", pos)) {
                break;
            }
            pos++;
        }
        return segments.isEmpty() ? null : new NodeId(List.copyOf(segments), text.substring(pos));
    }

    /** Positions of the {@code @ParameterizedClass} invocation segments, outermost first. */
    List<Integer> invocations() {
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            if (segments.get(i).startsWith(INVOCATION)) {
                positions.add(i);
            }
        }
        return positions;
    }

    /**
     * Positions of the invocation segments that are implementations for this node, outermost first: the invocations
     * of a marked class template, and the nearest invocation above a marked class or method on the path.
     */
    List<Integer> implementations(Marks marks) {
        List<Integer> positions = new ArrayList<>();
        String className = null;
        boolean markedTemplate = false;
        int nearest = -1;
        for (int i = 0; i < segments.size(); i++) {
            String segment = segments.get(i);
            int colon = segment.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String type = segment.substring(1, colon);
            String value = URLDecoder.decode(segment.substring(colon + 1, segment.length() - 1), StandardCharsets.UTF_8);
            boolean marked = false;
            switch (type) {
                case "class", "class-template", "nested-class", "nested-class-template" -> {
                    className = type.startsWith("nested-") ? className + "$" + value : value;
                    boolean template = type.endsWith("-template");
                    boolean annotated = marks.isMarked(className);
                    markedTemplate = template && annotated;
                    marked = !template && annotated;
                }
                case "class-template-invocation" -> {
                    nearest = i;
                    if (markedTemplate) {
                        positions.add(i);
                    }
                }
                case "method", "test-template", "test-factory" -> marked = className != null
                        && marks.isMarked(className, value);
                default -> {
                }
            }
            if (marked && nearest >= 0 && !positions.contains(nearest)) {
                positions.add(nearest);
            }
        }
        return positions;
    }

    boolean isInvocation() {
        return segments.getLast().startsWith(INVOCATION);
    }

    /** Whether this node is an invocation and one of the given implementation positions. */
    boolean isImplementation(List<Integer> implementations) {
        return isInvocation() && implementations.contains(segments.size() - 1);
    }

    /** The id of the ancestor (or this node) ending at the given segment. */
    String prefix(int lastSegment) {
        return String.join("/", segments.subList(0, lastSegment + 1)) + suffix;
    }

    /** The id without the invocation segments at the given positions. */
    String without(List<Integer> positions) {
        List<String> kept = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            if (!positions.contains(i)) {
                kept.add(segments.get(i));
            }
        }
        return String.join("/", kept) + suffix;
    }

    boolean isDescendantOf(NodeId ancestor) {
        return segments.size() > ancestor.segments.size()
                && segments.subList(0, ancestor.segments.size()).equals(ancestor.segments);
    }

    String text() {
        return String.join("/", segments) + suffix;
    }
}
