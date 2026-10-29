package com.izakji.mccreatorutils.client.explore;

import com.izakji.mccreatorutils.explore.InstalledMod;
import java.net.URL;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;

/**
 * Builds Explore Mods entries from the loaded mod list.
 *
 * @author Izakji
 */
public final class InstalledModCatalog {
    private static final Set<String> SYSTEM_MODS = Set.of("minecraft", "neoforge", "fml");

    private InstalledModCatalog() {}

    /**
     * Returns the loaded mods, excluding loader-internal entries.
     *
     * @return immutable entries sorted by display name
     */
    public static List<InstalledMod> load() {
        return ModList.get().getMods().stream()
                .filter(info -> !SYSTEM_MODS.contains(info.getModId()))
                .map(InstalledModCatalog::toInstalledMod)
                .sorted(Comparator.comparing(InstalledMod::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static InstalledMod toInstalledMod(IModInfo info) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        info.getConfig().<String>getConfigElement("displayURL").ifPresent(urls::add);
        IModFileInfo owningFile = info.getOwningFile();
        if (owningFile != null) {
            owningFile.getConfig().<String>getConfigElement("displayURL").ifPresent(urls::add);
        }
        info.getModURL().map(URL::toString).ifPresent(urls::add);
        return new InstalledMod(
                info.getModId(), info.getDisplayName(), info.getVersion().toString(), List.copyOf(urls));
    }
}
