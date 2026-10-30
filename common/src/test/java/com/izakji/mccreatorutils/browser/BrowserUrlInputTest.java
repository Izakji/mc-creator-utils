package com.izakji.mccreatorutils.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BrowserUrlInputTest {

    @Test
    void addsHttpsWhenMissing() {
        assertEquals(
                "https://example.com/path",
                BrowserUrlInput.normalize("example.com/path").orElseThrow());
    }

    @Test
    void acceptsSupportedSchemes() {
        assertEquals(
                "http://example.com",
                BrowserUrlInput.normalize("http://example.com").orElseThrow());
        assertEquals("about:blank", BrowserUrlInput.normalize("about:blank").orElseThrow());
    }

    @Test
    void treatsHostAndPortAsMissingScheme() {
        assertEquals(
                "https://localhost:8080",
                BrowserUrlInput.normalize("localhost:8080").orElseThrow());
        assertEquals(
                "https://example.com:8443/path",
                BrowserUrlInput.normalize("example.com:8443/path").orElseThrow());
    }

    @Test
    void rejectsUnsafeOrMalformedInput() {
        for (String raw :
                List.of("javascript:alert(1)", "file:///etc/passwd", "data:text/html,hi", "http://", "", "   ")) {
            assertTrue(BrowserUrlInput.normalize(raw).isEmpty(), raw);
        }
    }
}
