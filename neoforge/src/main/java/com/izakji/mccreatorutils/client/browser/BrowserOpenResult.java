package com.izakji.mccreatorutils.client.browser;

/**
 * Outcomes of asking the browser workspace to open a page or tab.
 *
 * @author Izakji
 */
public enum BrowserOpenResult {
    OPENED,
    FOCUSED_EXISTING,
    CAPACITY,
    UNAVAILABLE,
    FAILED
}
