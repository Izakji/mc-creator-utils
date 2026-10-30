package com.izakji.agent.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class SecretRedactionTest {

    @Test
    void redactsBearerKeysAndConfiguredSecrets() {
        Redactor redactor = new Redactor(Set.of("sk-secret", "mcp-token"));
        String output = redactor.redact("Authorization: Bearer sk-secret api_key=mcp-token safe=value");
        assertFalse(output.contains("sk-secret"));
        assertFalse(output.contains("mcp-token"));
        assertTrue(output.contains("safe=value"));
    }
}
