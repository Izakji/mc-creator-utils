package com.izakji.mccreatorutils.client.agent.context;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Groups recent log warnings and errors for the agent.
 *
 * @author Izakji
 */
public final class LogSummaryReport {
    private static final int MAX_OUTPUT = 32 * 1024;
    private static final int MAX_STACK_LINES = 12;
    private static final Pattern LEVEL = Pattern.compile("\\[(?:[^\\]]*/)?(FATAL|ERROR|WARN|INFO|DEBUG|TRACE)]");
    private static final Pattern UUID =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern HEX = Pattern.compile("0x[0-9a-fA-F]+");
    private static final Pattern NUMBER = Pattern.compile("\\d+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private LogSummaryReport() {}

    public static String format(String text, int maxGroups) {
        List<String> lines = List.of(text.split("\\R"));
        int fatal = 0;
        int errors = 0;
        int warnings = 0;
        Map<String, Integer> groups = new LinkedHashMap<>();
        int newestProblem = -1;
        List<String> problemLines = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            String level = levelOf(line);
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
            String key = level + ": " + normalize(message(line));
            groups.merge(key, 1, Integer::sum);
        }
        if (newestProblem >= 0) {
            problemLines.add(lines.get(newestProblem));
            for (int index = newestProblem + 1;
                    index < lines.size() && problemLines.size() <= MAX_STACK_LINES;
                    index++) {
                String line = lines.get(index);
                if (!isStackLine(line)) {
                    break;
                }
                problemLines.add(line);
            }
        }
        StringBuilder report = new StringBuilder();
        report.append("Log summary (last ")
                .append(lines.size())
                .append(" lines): FATAL ")
                .append(fatal)
                .append(", ERROR ")
                .append(errors)
                .append(", WARN ")
                .append(warnings)
                .append('\n');
        if (groups.isEmpty()) {
            report.append("No ERROR or WARN lines found.");
            return report.toString();
        }
        List<Map.Entry<String, Integer>> ranked = new ArrayList<>(groups.entrySet());
        ranked.sort(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                .reversed());
        report.append("Top repeated problems:\n");
        int shown = Math.min(Math.max(1, maxGroups), ranked.size());
        for (int index = 0; index < shown; index++) {
            Map.Entry<String, Integer> entry = ranked.get(index);
            report.append("- ")
                    .append(entry.getValue())
                    .append("x ")
                    .append(entry.getKey())
                    .append('\n');
        }
        if (!problemLines.isEmpty()) {
            report.append("Newest problem:\n");
            for (String line : problemLines) {
                report.append(line).append('\n');
            }
        }
        String output = report.toString();
        return output.length() <= MAX_OUTPUT ? output : output.substring(0, MAX_OUTPUT);
    }

    static String levelOf(String line) {
        Matcher matcher = LEVEL.matcher(line);
        return matcher.find() ? matcher.group(1) : "";
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
                || (line.startsWith("\t"));
    }

    static String normalizedLevel(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String level = value.strip().toUpperCase(Locale.ROOT);
        return LEVEL.matcher("[" + level + "]").matches() ? level : "";
    }
}
