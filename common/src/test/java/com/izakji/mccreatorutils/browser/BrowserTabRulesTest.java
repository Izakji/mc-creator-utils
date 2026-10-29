package com.izakji.mccreatorutils.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class BrowserTabRulesTest {

    @Test
    void limitsTabsAndFindsExistingUrl() {
        assertTrue(BrowserTabRules.canOpenNewTab(7));
        assertFalse(BrowserTabRules.canOpenNewTab(8));
        assertEquals(
                OptionalInt.of(1),
                BrowserTabRules.findOpenTab(List.of("about:blank", "https://a.example"), "https://a.example"));
    }

    @Test
    void treatsRootTrailingSlashAsSamePage() {
        assertEquals(
                OptionalInt.of(0), BrowserTabRules.findOpenTab(List.of("https://a.example/"), "https://a.example"));
        assertEquals(
                OptionalInt.empty(),
                BrowserTabRules.findOpenTab(List.of("https://a.example/foo/"), "https://a.example/foo"));
    }

    @Test
    void choosesNearestActiveIndexAfterRemoval() {
        assertEquals(0, BrowserTabRules.nextActiveIndex(0, 0, 3));
        assertEquals(1, BrowserTabRules.nextActiveIndex(2, 0, 3));
        assertEquals(1, BrowserTabRules.nextActiveIndex(2, 2, 3));
        assertEquals(1, BrowserTabRules.nextActiveIndex(1, 2, 3));
    }
}
