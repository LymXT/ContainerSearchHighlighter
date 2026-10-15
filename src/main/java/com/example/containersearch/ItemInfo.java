package com.example.containersearch;

import java.util.Locale;

/**
 * Зберігає метадані предмета для швидкого та нечутливого до регістру пошуку.
 */
public record ItemInfo(
        String fullId,         // наприклад: "minecraft:diamond"
        String pathId,         // наприклад: "diamond"
        String displayName,    // відображуване ім'я (наприклад: "Алмаз", "Diamond")
        String translationKey, // наприклад: "item.minecraft.diamond"
        int count              // кількість у стеку
) {
    /**
     * Перевіряє, чи відповідає предмет пошуковому запиту (нечутливо до регістру).
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
