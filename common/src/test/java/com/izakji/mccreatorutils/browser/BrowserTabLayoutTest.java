package com.izakji.mccreatorutils.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BrowserTabLayoutTest {

    @Test
    void laysOutEveryTabInsideTheMinimumWidthStrip() {
        BrowserGeometry strip = new BrowserGeometry(10, 20, BrowserLayout.MIN_WINDOW_WIDTH, BrowserLayout.TAB_HEIGHT);

        int previousRight = strip.x();
        for (int index = 0; index < BrowserTabRules.MAX_TABS; index++) {
            BrowserGeometry tab = BrowserTabLayout.tabBounds(strip, index, BrowserTabRules.MAX_TABS);

            assertTrue(tab.width() > 0, "tab " + index + " has no width");
            assertTrue(tab.x() >= previousRight, "tab " + index + " overlaps the previous tab");
            assertTrue(
                    tab.right() <= BrowserTabLayout.plusBounds(strip).x(),
                    "tab " + index + " overlaps the plus button");
            assertEquals(strip.y() + 2, tab.y());
            assertEquals(strip.height() - 4, tab.height());
            previousRight = tab.right();
        }
    }

    @Test
    void extraWidthIsSharedEvenlyAcrossTabs() {
        BrowserGeometry strip = new BrowserGeometry(0, 0, 400, BrowserLayout.TAB_HEIGHT);
        BrowserGeometry first = BrowserTabLayout.tabBounds(strip, 0, 4);
        BrowserGeometry last = BrowserTabLayout.tabBounds(strip, 3, 4);

        assertEquals(first.width(), last.width());
        assertEquals(first.x() + 3 * (first.width() + BrowserTabLayout.TAB_GAP), last.x());
    }

    @Test
    void closeZoneIsInsideTheTabAndOnItsRightEdge() {
        BrowserGeometry strip = new BrowserGeometry(0, 0, 400, BrowserLayout.TAB_HEIGHT);
        BrowserGeometry tab = BrowserTabLayout.tabBounds(strip, 0, 4);

        assertFalse(BrowserTabLayout.isCloseHit(tab, tab.x()));
        assertTrue(BrowserTabLayout.isCloseHit(tab, tab.right() - 1));
        assertTrue(BrowserTabLayout.isCloseHit(tab, tab.right() - BrowserTabLayout.TAB_CLOSE_WIDTH));
    }

    @Test
    void zeroTabsProduceNoTabWidth() {
        BrowserGeometry strip = new BrowserGeometry(0, 0, 400, BrowserLayout.TAB_HEIGHT);
        assertEquals(0, BrowserTabLayout.tabWidth(strip, 0));
        assertTrue(BrowserTabLayout.plusBounds(strip).x() > strip.x());
    }
}
