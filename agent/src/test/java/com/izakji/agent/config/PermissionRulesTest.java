package com.izakji.agent.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PermissionRulesTest {

    @Test
    void copiesSetsAndExposesDefaults() {
        PermissionRules rules = new PermissionRules(
                true, new LinkedHashSet<>(Set.of("game_snapshot")), Set.of("run_command"), Set.of());
        assertTrue(rules.readOnlyAutoApprove());
        assertEquals(Set.of("game_snapshot"), rules.allowedTools());
        assertThrows(
                UnsupportedOperationException.class, () -> rules.allowedTools().add("x"));
    }
}
