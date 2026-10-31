package com.izakji.mccreatorutils.client.agent.context;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;

/**
 * Captures F3 debug text and exact JVM, GC, and operating-system data.
 *
 * @author Izakji
 */
final class DebugInfoCollector {
    private static final int MAX_OUTPUT = 24 * 1024;
    private static final int MAX_F3_LINES = 200;
    private static final Pattern FORMATTING = Pattern.compile("§.");
    private static final Pattern GC_FLAG = Pattern.compile(
            "^-Xm[sx].*|^-XX:.*(GC|Heap|Metaspace|DirectMemory|Tenuring|Survivor|PreTouch).*",
            Pattern.CASE_INSENSITIVE);

    private DebugInfoCollector() {}

    static String capture() {
        StringBuilder report = new StringBuilder();
        appendGc(report);
        appendJvm(report);
        appendOs(report);
        appendMemory(report);
        appendF3(report);
        String output = report.toString();
        return output.length() <= MAX_OUTPUT ? output : output.substring(0, MAX_OUTPUT);
    }

    private static void appendGc(StringBuilder report) {
        report.append("Garbage collectors:\n");
        List<GarbageCollectorMXBean> collectors = ManagementFactory.getGarbageCollectorMXBeans();
        if (collectors.isEmpty()) {
            report.append("- unavailable\n");
        } else {
            for (GarbageCollectorMXBean collector : collectors) {
                report.append("- ")
                        .append(collector.getName())
                        .append(": ")
                        .append(collector.getCollectionCount())
                        .append(" collections, ")
                        .append(collector.getCollectionTime())
                        .append(" ms total\n");
            }
        }
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        List<String> flags = new ArrayList<>();
        for (String argument : runtime.getInputArguments()) {
            if (GC_FLAG.matcher(argument).matches()) {
                flags.add(argument);
            }
        }
        report.append("GC/JVM flags: ")
                .append(flags.isEmpty() ? "none detected" : String.join(", ", flags))
                .append('\n');
    }

    private static void appendJvm(StringBuilder report) {
        report.append("JVM: ")
                .append(System.getProperty("java.vm.name", "unknown"))
                .append(' ')
                .append(System.getProperty("java.vm.version", ""))
                .append('\n');
        report.append("Java: ")
                .append(System.getProperty("java.version", "unknown"))
                .append(" (")
                .append(System.getProperty("java.vendor", "unknown"))
                .append(", spec ")
                .append(System.getProperty("java.specification.version", "unknown"))
                .append(")\n");
    }

    private static void appendOs(StringBuilder report) {
        report.append("OS: ")
                .append(System.getProperty("os.name", "unknown"))
                .append(' ')
                .append(System.getProperty("os.version", ""))
                .append(' ')
                .append(System.getProperty("os.arch", ""))
                .append(", ")
                .append(Runtime.getRuntime().availableProcessors())
                .append(" processors\n");
        if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean system) {
            report.append("Physical memory: ")
                    .append(String.format(
                            Locale.ROOT,
                            "%.1f / %.1f GiB free",
                            system.getFreeMemorySize() / 1073741824.0,
                            system.getTotalMemorySize() / 1073741824.0))
                    .append('\n');
        }
    }

    private static void appendMemory(StringBuilder report) {
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        MemoryUsage nonHeap = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        report.append("Heap: ")
                .append(mib(heap.getUsed()))
                .append(" / ")
                .append(heap.getMax() <= 0 ? "unlimited" : mib(heap.getMax()))
                .append(" MiB, committed ")
                .append(mib(heap.getCommitted()))
                .append(" MiB\n");
        report.append("Non-heap: ").append(mib(nonHeap.getUsed())).append(" MiB\n");
        report.append("Memory pools:\n");
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            MemoryUsage usage = pool.getUsage();
            report.append("- ")
                    .append(pool.getName())
                    .append(" (")
                    .append(pool.getType() == MemoryType.HEAP ? "heap" : "non-heap")
                    .append("): ")
                    .append(mib(usage.getUsed()))
                    .append(" / ")
                    .append(usage.getMax() <= 0 ? "unlimited" : mib(usage.getMax()))
                    .append(" MiB\n");
        }
    }

    private static void appendF3(StringBuilder report) {
        DebugScreenOverlay overlay = Minecraft.getInstance().getDebugOverlay();
        report.append("\nF3 game information:\n");
        appendLines(report, invokeLines(overlay, "collectGameInformationText"));
        report.append("\nF3 system information:\n");
        appendLines(report, invokeLines(overlay, "collectSystemInformationText"));
    }

    @SuppressWarnings("unchecked")
    private static List<String> invokeLines(DebugScreenOverlay overlay, String name) {
        try {
            Method method = DebugScreenOverlay.class.getDeclaredMethod(name);
            method.setAccessible(true);
            Object value = method.invoke(overlay);
            return value instanceof List<?> lines ? (List<String>) lines : List.of("unavailable");
        } catch (ReflectiveOperationException | RuntimeException failure) {
            return List.of("unavailable: " + failure.getClass().getSimpleName());
        }
    }

    private static void appendLines(StringBuilder report, List<String> lines) {
        int shown = Math.min(lines.size(), MAX_F3_LINES);
        for (int index = 0; index < shown; index++) {
            String line = FORMATTING.matcher(lines.get(index)).replaceAll("");
            if (!line.isBlank()) {
                report.append(line).append('\n');
            }
        }
        if (lines.size() > shown) {
            report.append("... ").append(lines.size() - shown).append(" more lines\n");
        }
    }

    private static long mib(long bytes) {
        return bytes / (1024L * 1024L);
    }
}
