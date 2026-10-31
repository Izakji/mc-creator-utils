package com.izakji.mccreatorutils.client.agent.context;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formats structured log summaries for the agent.
 *
 * @author Izakji
 */
public final class LogSummaryReport {
    private static final int MAX_OUTPUT = 32 * 1024;
    private static final Pattern LEVEL = Pattern.compile("\\[(?:[^\\]]*/)?(FATAL|ERROR|WARN|INFO|DEBUG|TRACE)]");

    private LogSummaryReport() {}

    public static String format(String text, int maxGroups) {
        return format(LogSummary.parse(text, maxGroups));
    }

    public static String format(LogSummary summary) {
        StringBuilder report = new StringBuilder();
        report.append("Log summary (last ")
                .append(summary.totalLines())
                .append(" lines): FATAL ")
                .append(summary.fatal())
                .append(", ERROR ")
                .append(summary.errors())
                .append(", WARN ")
                .append(summary.warnings())
                .append('\n');
        if (summary.empty()) {
            report.append("No ERROR or WARN lines found.");
            return report.toString();
        }
        report.append("Top repeated problems:\n");
        for (LogSummary.LogGroup group : summary.groups()) {
            report.append("- ")
                    .append(group.count())
                    .append("x ")
                    .append(group.level())
                    .append(": ")
                    .append(group.message())
                    .append('\n');
        }
        if (!summary.newestProblemLines().isEmpty()) {
            report.append("Newest problem:\n");
            for (String line : summary.newestProblemLines()) {
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

    static String normalizedLevel(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String level = value.strip().toUpperCase(Locale.ROOT);
        return LEVEL.matcher("[" + level + "]").matches() ? level : "";
    }
}
