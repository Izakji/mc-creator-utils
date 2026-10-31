package com.izakji.mccreatorutils.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Basic inline Markdown parsing for chat transcript lines.
 *
 * @author Izakji
 */
public final class AgentMarkdown {
    private static final Pattern TOKEN = Pattern.compile("\\*\\*(.+?)\\*\\*|\\*(.+?)\\*|`([^`]+)`");

    private AgentMarkdown() {}

    /**
     * Parses bold, italic, and inline-code spans. Unmatched markers stay literal.
     *
     * @param line source line
     * @return spans in display order
     */
    public static List<Span> parseLine(String line) {
        if (line == null || line.isEmpty()) {
            return List.of();
        }
        List<Span> spans = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(line);
        int cursor = 0;
        while (matcher.find()) {
            if (matcher.start() > cursor) {
                spans.add(new Span(line.substring(cursor, matcher.start()), false, false, false));
            }
            if (matcher.group(1) != null) {
                spans.add(new Span(matcher.group(1), true, false, false));
            } else if (matcher.group(2) != null) {
                spans.add(new Span(matcher.group(2), false, true, false));
            } else {
                spans.add(new Span(matcher.group(3), false, false, true));
            }
            cursor = matcher.end();
        }
        if (cursor < line.length()) {
            spans.add(new Span(line.substring(cursor), false, false, false));
        }
        return List.copyOf(spans);
    }

    /**
     * One styled inline run.
     *
     * @param text displayed text
     * @param bold bold text
     * @param italic italic text
     * @param code monospace text
     */
    public record Span(String text, boolean bold, boolean italic, boolean code) {}
}
