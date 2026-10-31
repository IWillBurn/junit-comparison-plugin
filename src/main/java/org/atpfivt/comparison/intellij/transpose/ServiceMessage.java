package org.atpfivt.comparison.intellij.transpose;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A TeamCity service message as IntelliJ's JUnit runner prints it: {@code ##teamcity[name key='value' ...]}. Only the
 * attribute form is supported; IntelliJ sometimes omits the space between attributes and puts spaces around
 * {@code =}, both are accepted.
 */
public record ServiceMessage(String name, Map<String, String> attributes) {

    static final String PREFIX = "##teamcity[";

    /** Whether the trimmed line looks like a service message (it may still fail to parse). */
    public static boolean isServiceMessage(String line) {
        return line.trim().startsWith(PREFIX);
    }

    /** The message, or null if the line is not a service message with attributes. */
    public static ServiceMessage parse(String line) {
        String text = line.trim();
        if (!text.startsWith(PREFIX) || !text.endsWith("]")) {
            return null;
        }
        int pos = PREFIX.length();
        int end = text.length() - 1;
        int nameEnd = pos;
        while (nameEnd < end && !Character.isWhitespace(text.charAt(nameEnd))) {
            nameEnd++;
        }
        String name = text.substring(pos, nameEnd);
        Map<String, String> attributes = new LinkedHashMap<>();
        pos = nameEnd;
        while (true) {
            pos = skipSpaces(text, pos, end);
            if (pos == end) {
                return name.isEmpty() ? null : new ServiceMessage(name, attributes);
            }
            int keyEnd = pos;
            while (keyEnd < end && text.charAt(keyEnd) != '=' && !Character.isWhitespace(text.charAt(keyEnd))) {
                keyEnd++;
            }
            String key = text.substring(pos, keyEnd);
            pos = skipSpaces(text, keyEnd, end);
            if (key.isEmpty() || pos == end || text.charAt(pos) != '=') {
                return null; // e.g. the single-value form ##teamcity[name 'value']
            }
            pos = skipSpaces(text, pos + 1, end);
            if (pos == end || text.charAt(pos) != '\'') {
                return null;
            }
            StringBuilder value = new StringBuilder();
            pos++;
            while (pos < end && text.charAt(pos) != '\'') {
                char c = text.charAt(pos);
                if (c == '|' && pos + 1 < end) {
                    pos = unescape(text, pos + 1, value);
                }
                else {
                    value.append(c);
                    pos++;
                }
            }
            if (pos == end) {
                return null;
            }
            attributes.put(key, value.toString());
            pos++;
        }
    }

    public String attribute(String key) {
        return attributes.get(key);
    }

    /** A copy with the attribute set (or added at the end). */
    public ServiceMessage with(String key, String value) {
        Map<String, String> copy = new LinkedHashMap<>(attributes);
        copy.put(key, value);
        return new ServiceMessage(name, copy);
    }

    public String format() {
        StringBuilder sb = new StringBuilder(PREFIX).append(name);
        attributes.forEach((key, value) -> sb.append(' ').append(key).append("='").append(escape(value)).append('\''));
        return sb.append(']').toString();
    }

    private static int skipSpaces(String text, int pos, int end) {
        while (pos < end && Character.isWhitespace(text.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    // pos points after '|'; returns the position after the escape sequence
    private static int unescape(String text, int pos, StringBuilder out) {
        char c = text.charAt(pos);
        switch (c) {
            case 'n' -> out.append('\n');
            case 'r' -> out.append('\r');
            case 'x' -> out.append('\u0085');
            case 'l' -> out.append(' ');
            case 'p' -> out.append(' ');
            case '0' -> {
                if (pos + 5 < text.length() && text.charAt(pos + 1) == 'x') {
                    out.append((char) Integer.parseInt(text.substring(pos + 2, pos + 6), 16));
                    return pos + 6;
                }
                out.append(c);
            }
            default -> out.append(c); // | ' [ ]
        }
        return pos + 1;
    }

    private static String escape(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            switch (c) {
                case '|' -> sb.append("||");
                case '\'' -> sb.append("|'");
                case '\n' -> sb.append("|n");
                case '\r' -> sb.append("|r");
                case '[' -> sb.append("|[");
                case ']' -> sb.append("|]");
                case '\u0085' -> sb.append("|x");
                case ' ' -> sb.append("|l");
                case ' ' -> sb.append("|p");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
