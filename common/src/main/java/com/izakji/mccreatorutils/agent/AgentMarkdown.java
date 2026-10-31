package com.izakji.mccreatorutils.agent;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic inline Markdown parsing for chat transcript lines.
 *
 * @author Izakji
 */
public final class AgentMarkdown {
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
        StringBuilder plain = new StringBuilder();
        int index = 0;
        while (index < line.length()) {
            if (line.startsWith("**", index)) {
                int end = line.indexOf("**", index + 2);
                if (end > index + 2) {
                    flush(spans, plain);
                    spans.add(new Span(line.substring(index + 2, end), true, false, false));
                    index = end + 2;
                    continue;
                }
                plain.append("**");
                index += 2;
                continue;
            }
            char current = line.charAt(index);
            if (current == '`') {
                int end = line.indexOf('`', index + 1);
                if (end > index + 1) {
                    flush(spans, plain);
                    spans.add(new Span(line.substring(index + 1, end), false, false, true));
                    index = end + 1;
                    continue;
                }
            } else if (current == '*') {
                int end = line.indexOf('*', index + 1);
                if (end > index + 1) {
                    flush(spans, plain);
                    spans.add(new Span(line.substring(index + 1, end), false, true, false));
                    index = end + 1;
                    continue;
                }
            }
            plain.append(current);
            index++;
        }
        flush(spans, plain);
        return List.copyOf(spans);
    }

    private static void flush(List<Span> spans, StringBuilder plain) {
        if (!plain.isEmpty()) {
            spans.add(new Span(plain.toString(), false, false, false));
            plain.setLength(0);
        }
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
