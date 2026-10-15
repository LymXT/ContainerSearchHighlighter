package com.example.containersearch;

import java.util.Locale;

/**
 * Stores item metadata for fast and case-insensitive search queries.
 */
public record ItemInfo(
        String fullId,         // e.g. "minecraft:diamond"
        String pathId,         // e.g. "diamond"
        String displayName,    // display name (e.g. "Diamond")
        String translationKey, // e.g. "item.minecraft.diamond"
        int count              // stack size
) {
    /**
     * Checks if the item matches the given search query (case-insensitive).
     */
    public boolean matches(String query) {
        if (query == null || query.isBlank()) {
            return false;
        }
        String q = query.trim().toLowerCase(Locale.ROOT);

        if (fullId != null && fullId.toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }
        if (pathId != null && pathId.toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }
        if (displayName != null && displayName.toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }
        if (translationKey != null && translationKey.toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }
        return false;
    }
}
