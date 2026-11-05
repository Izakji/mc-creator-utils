package com.izakji.mccreatorutils.client.explore;

/**
 * Resolves optional secrets without touching the config specification.
 *
 * @author Izakji
 */
final class ExploreModsSecrets {
    private ExploreModsSecrets() {}

    static String resolve(String environment, String configured) {
        if (environment != null && !environment.isBlank()) {
            return environment.strip();
        }
        return configured == null ? "" : configured.strip();
    }
}
