package com.izakji.mccreatorutils.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BrowserSessionTest {
    @Test
    void savesAllTabsAndClampsActiveIndex() {
        var session = new BrowserSession();
        assertFalse(session.hasPages());

        var urls = new ArrayList<>(List.of("https://a.example", "https://b.example"));
        session.save(urls, 1);
        urls.add("https://c.example");

        assertTrue(session.hasPages());
        assertEquals(List.of("https://a.example", "https://b.example"), session.urls());
        assertEquals(1, session.activeIndex());
    }

    @Test
    void clampsInvalidActiveIndexAndClearsEmptySession() {
        var session = new BrowserSession();
        session.save(List.of("https://a.example"), 9);
        assertEquals(0, session.activeIndex());

        session.clear();
        assertFalse(session.hasPages());
        assertEquals(-1, session.activeIndex());
        assertTrue(session.urls().isEmpty());
    }
}
