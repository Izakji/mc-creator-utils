package com.izakji.mccreatorutils.client.agent.context;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Structured ERROR and WARN summary of a log excerpt.
 *
 * @param fatal FATAL line count
 * @param errors ERROR line count
 * @param warnings WARN line count
 * @param totalLines lines scanned
 * @param groups ranked repeated problems
 * @param newestProblemLines newest problem line and stack sample
 * @author Izakji
 */
public record LogSummary(
        int fatal, int errors, int warnings, int totalLines, List<LogGroup> groups, List<String> newestProblemLines) {
    private static final int MAX_STACK_LINES = 12;
    private static final Pattern UUID =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern HEX = Pattern.compile("0x[0-9a-fA-F]+");
    private static final Pattern NUMBER = Pattern.compile("\\d+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public LogSummary {
        groups = List.copyOf(groups);
        newestProblemLines = List.copyOf(newestProblemLines);
    }

    /**
     * Parses one log excerpt.
     *
     * @param text log text
     * @param maxGroups maximum ranked groups
     * @return structured summary
     */
    public static LogSummary parse(String text, int maxGroups) {
        String[] lines = text.split("\\R", -1);
        int fatal = 0;
        int errors = 0;
        int warnings = 0;
        Map<String, Integer> grouped = new LinkedHashMap<>();
        int newestProblem = -1;
        for (int index = 0; index < lines.length; index++) {
            String level = LogSummaryReport.levelOf(lines[index]);
            if ("FATAL".equals(level)) {
                fatal++;
            } else if ("ERROR".equals(level)) {
                errors++;
            } else if ("WARN".equals(level)) {
                warnings++;
            } else {
                continue;
            }
            newestProblem = index;
            String key = level + ": " + normalize(message(lines[index]));
            grouped.merge(key, 1, Integer::sum);
        }
        List<Map.Entry<String, Integer>> ranked = new ArrayList<>(grouped.entrySet());
        ranked.sort(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                .reversed());
        int shown = Math.min(Math.max(1, maxGroups), ranked.size());
        List<LogGroup> groups = new ArrayList<>();
        for (int index = 0; index < shown; index++) {
            Map.Entry<String, Integer> entry = ranked.get(index);
            int separator = entry.getKey().indexOf(": ");
            String level = separator < 0 ? "" : entry.getKey().substring(0, separator);
            String value = separator < 0 ? entry.getKey() : entry.getKey().substring(separator + 2);
            groups.add(new LogGroup(level, value, entry.getValue()));
        }
        List<String> problemLines = new ArrayList<>();
        if (newestProblem >= 0) {
            problemLines.add(lines[newestProblem]);
            for (int index = newestProblem + 1;
                    index < lines.length && problemLines.size() <= MAX_STACK_LINES;
                    index++) {
                if (!isStackLine(lines[index])) {
                    break;
                }
                problemLines.add(lines[index]);
            }
        }
        return new LogSummary(fatal, errors, warnings, lines.length, groups, problemLines);
    }

    public boolean empty() {
        return groups.isEmpty();
    }

    private static String message(String line) {
        int marker = line.indexOf("]: ");
        return marker >= 0 ? line.substring(marker + 3) : line;
    }

    private static String normalize(String message) {
        String value = UUID.matcher(message).replaceAll("#");
        value = HEX.matcher(value).replaceAll("#");
        value = NUMBER.matcher(value).replaceAll("#");
        value = WHITESPACE.matcher(value).replaceAll(" ").strip();
        return value.length() <= 160 ? value : value.substring(0, 160) + "...";
    }

    private static boolean isStackLine(String line) {
        String stripped = line.stripLeading();
        return stripped.startsWith("at ")
                || stripped.startsWith("Caused by:")
                || stripped.startsWith("... ")
                || line.startsWith("\t");
    }

    /**
     * One repeated log problem.
     *
     * @param level log level
     * @param message normalized message
     * @param count occurrences
     */
    public record LogGroup(String level, String message, int count) {}
}
