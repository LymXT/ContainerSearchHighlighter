package com.example.containersearch;

import com.example.containersearch.gui.SearchScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

/**
 * Точка входу клієнтського мода Container Search Highlighter для Fabric 26.3.
 */
public class ContainerSearchClient implements ClientModInitializer {

    public static final String MOD_ID = "containersearch";

    /** Гаряча клавіша відкриття GUI пошуку (за замовчуванням клавіша 'H') */
    private static KeyMapping searchKey;

    /** Поточний активний пошуковий рядок */
    private static String searchQuery = "";

    /** Максимальний радіус підсвічування навколо гравця (64 блоки) */
    public static final double MAX_RADIUS = 64.0;
    public static final double MAX_RADIUS_SQ = MAX_RADIUS * MAX_RADIUS;

    public static String getSearchQuery() {
        return searchQuery;
    }

    public static void setSearchQuery(String query) {
        searchQuery = (query != null) ? query.trim() : "";

        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            if (searchQuery.isEmpty()) {
                client.player.sendSystemMessage(Component.literal("§6[ContainerSearch] §7Пошук скинуто. Підсвічування вимкнено."));
            } else {
                String dim = (client.level != null) ? client.level.dimension().identifier().toString() : "";
                int count = ContainerCache.countMatchingContainers(dim, searchQuery);
                client.player.sendSystemMessage(Component.literal(
                        "§6[ContainerSearch] §fПошук: §a\"" + searchQuery + "\" §7— знайдено контейнерів: §e" + count
                ));
            }
        }
    }

    @Override
    public void onInitializeClient() {
        // 1. Реєстрація гарячої клавіші 'H' для відкриття екрана пошуку
        searchKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.containersearch.open_search",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_H,
                KeyMapping.Category.MISC
        ));

        // 2. Обробка натискання клавіші в ігровому циклі
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (searchKey.consumeClick()) {
                client.setScreenAndShow(new SearchScreen());
            }
        });

        // 3. Відстеження натискання правою кнопкою миші по блоках (контейнерах)
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (level.isClientSide()) {
                ContainerTracker.onBlockInteracted(level, hitResult.getBlockPos());
            }
            return InteractionResult.PASS;
        });

        // 4. Відстеження натискання правою кнопкою по сутностях-контейнерах (вагонетки із сундуком)
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide()) {
                ContainerTracker.onEntityInteracted(level, entity.blockPosition());
            }
            return InteractionResult.PASS;
        });

        // 5. Видалення координат із кешу при руйнуванні блоку контейнера
        ClientPlayerBlockBreakEvents.AFTER.register((level, player, pos, state) -> {
            ContainerCache.remove(level.dimension().identifier().toString(), pos);
        });

        // 6. Завантаження збереженого JSON-кешу з диска при вході у світ або сервер
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ContainerCache.onJoin(client);
            if (client.player != null) {
                int count = ContainerCache.getContainerCount();
                if (count > 0) {
                    client.player.sendSystemMessage(Component.literal(
                            "§6[ContainerSearch] §7Завантажено дисковий кеш: §a" + count + " §7контейнерів."
                    ));
                }
            }
        });

        // 7. Збереження та очищення стану при відключенні від сервера/світу
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ContainerCache.onDisconnect();
            searchQuery = "";
        });

        // 8. ESP-підсвічування результатів пошуку через LevelRenderEvents та Gizmos API
        LevelRenderEvents.BEFORE_GIZMOS.register(context -> {
            // Якщо пошуковий рядок порожній або скинутий — підсвічування вимкнено
            if (searchQuery.isEmpty()) {
                return;
            }

            Minecraft client = Minecraft.getInstance();
            ClientLevel level = client.level;
            LocalPlayer player = client.player;
            if (level == null || player == null) return;

            String currentDim = level.dimension().identifier().toString();
            Map<BlockPos, List<ItemInfo>> containers = ContainerCache.getContainersForDimension(currentDim);
            if (containers.isEmpty()) return;

            Vec3 playerPos = player.position();

            for (Map.Entry<BlockPos, List<ItemInfo>> entry : containers.entrySet()) {
                BlockPos pos = entry.getKey();

                // Фільтрація за радіусом (64 блоки)
                if (pos.distToCenterSqr(playerPos) > MAX_RADIUS_SQ) {
                    continue;
                }

                // Якщо блок у завантаженому чанку вже став повітрям (зруйнований) — не підсвічуємо
                if (level.isLoaded(pos) && level.getBlockState(pos).isAir()) {
                    continue;
                }

                List<ItemInfo> items = entry.getValue();
                boolean matches = false;
                for (ItemInfo item : items) {
                    if (item.matches(searchQuery)) {
                        matches = true;
                        break;
                    }
                }

                // Якщо знайдено предмет за запитом — підсвічуємо неоново-зеленим крізь стіни
                if (matches) {
                    AABB aabb = RenderUtils.getContainerBoundingBox(level, pos);
                    RenderUtils.renderHighlightGizmo(aabb);
                }
            }
        });
    }
}
