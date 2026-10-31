package com.izakji.mccreatorutils.client.agent.context;

/**
 * One timestamped chart point.
 *
 * @param epochMillis sample time
 * @param value value
 * @author Izakji
 */
public record ReportPoint(long epochMillis, double value) {}
