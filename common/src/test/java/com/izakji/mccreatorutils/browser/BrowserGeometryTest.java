package com.izakji.mccreatorutils.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BrowserGeometryTest {

    @Test
    void centersWithinTheScreen() {
        assertEquals(new BrowserGeometry(300, 150, 400, 300), BrowserGeometry.centered(1000, 600, 400, 300));
    }

    @Test
    void clampsWindowToScreenAndMinimum() {
        assertEquals(
                new BrowserGeometry(0, 0, 320, 220),
                new BrowserGeometry(-50, -20, 100, 100).clampTo(1000, 600, 320, 220));
        assertEquals(
                new BrowserGeometry(680, 380, 320, 220),
                new BrowserGeometry(900, 500, 320, 220).clampTo(1000, 600, 320, 220));
    }

    @Test
    void fullscreenUsesMargin() {
        assertEquals(new BrowserGeometry(10, 10, 980, 580), BrowserGeometry.fullscreen(1000, 600, 10));
    }
}
