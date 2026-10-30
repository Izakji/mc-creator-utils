package com.izakji.mccreatorutils.client.browser;

/**
 * Outcomes of asking the browser workspace to open a page or tab.
 *
 * @author Izakji
 */
public enum BrowserOpenResult {
    /** A new tab was created. */
    OPENED,
    /** An existing tab with the same normalized URL was focused. */
    FOCUSED_EXISTING,
    /** The workspace already has the maximum number of tabs. */
    CAPACITY,
    /** Rinku is not installed. */
    UNAVAILABLE,
    /** The URL was invalid or the browser could not create the tab. */
    FAILED
}
