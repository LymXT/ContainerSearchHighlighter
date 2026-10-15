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
 * Client-side entry point for the Container Search Highlighter mod for Fabric 26.3.
 */
public class ContainerSearchClient implements ClientModInitializer {

    public static final String MOD_ID = "containersearch";

    /** Key mapping for opening the search GUI (bound to 'H' by default). */
    private static KeyMapping searchKey;

    /** Currently active search query string. */
    private static String searchQuery = "";

    /** Maximum search and highlight radius around the player (64 blocks). */
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
        // 1. Register key mapping 'H' to open the search screen
        searchKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.containersearch.open_search",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_H,
                KeyMapping.Category.MISC
        ));

        // 2. Handle key presses during the client tick loop
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (searchKey.consumeClick()) {
                client.setScreenAndShow(new SearchScreen());
            }
        });

        // 3. Track right-click interactions with container blocks
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (level.isClientSide()) {
                ContainerTracker.onBlockInteracted(level, hitResult.getBlockPos());
            }
            return InteractionResult.PASS;
        });

        // 4. Track right-click interactions with container entities (e.g. chest minecarts)
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide()) {
                ContainerTracker.onEntityInteracted(level, entity.blockPosition());
            }
            return InteractionResult.PASS;
        });

        // 5. Remove container coordinates from cache when broken
        ClientPlayerBlockBreakEvents.AFTER.register((level, player, pos, state) -> {
            ContainerCache.remove(level.dimension().identifier().toString(), pos);
        });

        // 6. Load saved persistent JSON cache from disk on world/server join
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

        // 7. Flush and reset state on server/world disconnect
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ContainerCache.onDisconnect();
            searchQuery = "";
        });

        // 8. ESP highlight rendering via LevelRenderEvents and Gizmos API
        LevelRenderEvents.BEFORE_GIZMOS.register(context -> {
            // If the search query is empty or reset, highlighting is disabled
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

                // Filter by radius (64 blocks)
                if (pos.distToCenterSqr(playerPos) > MAX_RADIUS_SQ) {
                    continue;
                }

                // Skip if the block in loaded chunk has become air (broken)
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

                // If item matches query, highlight with neon green through walls
                if (matches) {
                    AABB aabb = RenderUtils.getContainerBoundingBox(level, pos);
                    RenderUtils.renderHighlightGizmo(aabb);
                }
            }
        });
    }
}
