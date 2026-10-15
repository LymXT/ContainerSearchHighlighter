package com.example.containersearch;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Thread-safe persistent container content cache.
 * Saves cached container data to disk as JSON (config/containersearch/cache_<world_id>.json)
 * isolated per singleplayer world or multiplayer server.
 */
public class ContainerCache {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor();

    // Structure: dimensionId -> (BlockPos -> list of items)
    private static final Map<String, Map<BlockPos, List<ItemInfo>>> CACHE = new ConcurrentHashMap<>();

    private static String currentWorldId = "";

    /**
     * DTO models for JSON serialization.
     */
    public static class CacheFileDto {
        public int schemaVersion = 1;
        public String worldId = "";
        public Map<String, List<ContainerEntryDto>> dimensions = new HashMap<>();
    }

    public static class ContainerEntryDto {
        public int x;
        public int y;
        public int z;
        public List<ItemInfoDto> items = new ArrayList<>();

        public ContainerEntryDto() {}

        public ContainerEntryDto(int x, int y, int z, List<ItemInfoDto> items) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.items = items;
        }
    }

    public static class ItemInfoDto {
        public String fullId;
        public String pathId;
        public String displayName;
        public String translationKey;
        public int count;

        public ItemInfoDto() {}

        public ItemInfoDto(String fullId, String pathId, String displayName, String translationKey, int count) {
            this.fullId = fullId;
            this.pathId = pathId;
            this.displayName = displayName;
            this.translationKey = translationKey;
            this.count = count;
        }
    }

    /**
     * Resolves a unique identifier for the world or server to isolate cache files.
     */
    public static String getCurrentWorldId(Minecraft client) {
        if (client == null) return "default";
        try {
            if (client.hasSingleplayerServer() && client.getSingleplayerServer() != null) {
                String levelName = client.getSingleplayerServer().getWorldData().getLevelName();
                return "sp_" + sanitize(levelName);
            }
            if (client.getCurrentServer() != null) {
                String ip = client.getCurrentServer().ip;
                return "mp_" + sanitize(ip);
            }
        } catch (Exception e) {
            // fallback
        }
        return "default";
    }

    private static String sanitize(String input) {
        if (input == null || input.isBlank()) return "world";
        return input.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }

    /**
     * Called upon connecting to a world or server (ClientPlayConnectionEvents.JOIN).
     * Loads the saved JSON cache into memory.
     */
    public static void onJoin(Minecraft client) {
        currentWorldId = getCurrentWorldId(client);
        loadFromDisk();
    }

    /**
     * Called upon disconnecting from a world or server (ClientPlayConnectionEvents.DISCONNECT).
     * Flushes the cache to disk and clears memory.
     */
    public static void onDisconnect() {
        saveToDiskSync();
        CACHE.clear();
        currentWorldId = "";
    }

    /**
     * Stores or updates the item contents of a container at the given position and synchronizes to disk.
     */
    public static void put(String dimension, BlockPos pos, List<ItemInfo> items) {
        if (dimension == null || pos == null || items == null) return;
        CACHE.computeIfAbsent(dimension, d -> new ConcurrentHashMap<>())
             .put(pos.immutable(), List.copyOf(items));
        saveToDisk();
    }

    /**
     * Removes a container from the cache (e.g. when broken) and updates the disk cache file.
     */
    public static void remove(String dimension, BlockPos pos) {
        if (dimension == null || pos == null) return;
        Map<BlockPos, List<ItemInfo>> dimMap = CACHE.get(dimension);
        if (dimMap != null) {
            dimMap.remove(pos);
            saveToDisk();
        }
    }

    /**
     * Returns the container map for the specified dimension.
     */
    public static Map<BlockPos, List<ItemInfo>> getContainersForDimension(String dimension) {
        if (dimension == null) return Collections.emptyMap();
        Map<BlockPos, List<ItemInfo>> dimMap = CACHE.get(dimension);
        return (dimMap != null) ? dimMap : Collections.emptyMap();
    }

    /**
     * Returns the total count of cached containers across all dimensions in the current world.
     */
    public static int getContainerCount() {
        int total = 0;
        for (Map<BlockPos, List<ItemInfo>> map : CACHE.values()) {
            total += map.size();
        }
        return total;
    }

    /**
     * Counts the number of containers in the specified dimension that contain at least one item matching the search query.
     */
    public static int countMatchingContainers(String dimension, String query) {
        if (query == null || query.isBlank() || dimension == null) return 0;
        Map<BlockPos, List<ItemInfo>> dimMap = CACHE.get(dimension);
        if (dimMap == null) return 0;

        int count = 0;
        for (List<ItemInfo> items : dimMap.values()) {
            for (ItemInfo item : items) {
                if (item.matches(query)) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    /**
     * Clears all cached container data from memory.
     */
    public static void clear() {
        CACHE.clear();
    }

    /**
     * Loads cached container data from disk (config/containersearch/cache_<world_id>.json).
     */
    public static void loadFromDisk() {
        if (currentWorldId == null || currentWorldId.isBlank()) return;

        try {
            Path configDir = FabricLoader.getInstance().getConfigDir().resolve("containersearch");
            Path cacheFile = configDir.resolve("cache_" + currentWorldId + ".json");

            if (!Files.exists(cacheFile)) {
                return;
            }

            try (Reader reader = Files.newBufferedReader(cacheFile, StandardCharsets.UTF_8)) {
                CacheFileDto dto = GSON.fromJson(reader, CacheFileDto.class);
                if (dto != null && dto.dimensions != null) {
                    CACHE.clear();
                    for (Map.Entry<String, List<ContainerEntryDto>> dimEntry : dto.dimensions.entrySet()) {
                        String dim = dimEntry.getKey();
                        Map<BlockPos, List<ItemInfo>> posMap = new ConcurrentHashMap<>();

                        for (ContainerEntryDto entry : dimEntry.getValue()) {
                            BlockPos pos = new BlockPos(entry.x, entry.y, entry.z);
                            List<ItemInfo> items = new ArrayList<>();
                            if (entry.items != null) {
                                for (ItemInfoDto itemDto : entry.items) {
                                    items.add(new ItemInfo(
                                            itemDto.fullId,
                                            itemDto.pathId,
                                            itemDto.displayName,
                                            itemDto.translationKey,
                                            itemDto.count
                                    ));
                                }
                            }
                            posMap.put(pos, List.copyOf(items));
                        }
                        CACHE.put(dim, posMap);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ContainerSearch] Failed to load persistent cache: " + e.getMessage());
        }
    }

    /**
     * Asynchronously writes the cache state to disk on a background executor without blocking render/game ticks.
     */
    public static void saveToDisk() {
        if (currentWorldId == null || currentWorldId.isBlank()) return;
        final String worldIdToSave = currentWorldId;
        final CacheFileDto dto = buildDtoSnapshot(worldIdToSave);

        IO_EXECUTOR.submit(() -> writeDtoToFile(worldIdToSave, dto));
    }

    /**
     * Synchronously writes the cache state to disk (used during client disconnect).
     */
    public static void saveToDiskSync() {
        if (currentWorldId == null || currentWorldId.isBlank()) return;
        final String worldIdToSave = currentWorldId;
        final CacheFileDto dto = buildDtoSnapshot(worldIdToSave);
        writeDtoToFile(worldIdToSave, dto);
    }

    private static CacheFileDto buildDtoSnapshot(String worldId) {
        CacheFileDto dto = new CacheFileDto();
        dto.worldId = worldId;
        dto.schemaVersion = 1;

        for (Map.Entry<String, Map<BlockPos, List<ItemInfo>>> dimEntry : CACHE.entrySet()) {
            String dim = dimEntry.getKey();
            List<ContainerEntryDto> list = new ArrayList<>();

            for (Map.Entry<BlockPos, List<ItemInfo>> entry : dimEntry.getValue().entrySet()) {
                BlockPos pos = entry.getKey();
                List<ItemInfoDto> itemDtos = new ArrayList<>();

                for (ItemInfo info : entry.getValue()) {
                    itemDtos.add(new ItemInfoDto(
                            info.fullId(),
                            info.pathId(),
                            info.displayName(),
                            info.translationKey(),
                            info.count()
                    ));
                }
                list.add(new ContainerEntryDto(pos.getX(), pos.getY(), pos.getZ(), itemDtos));
            }
            dto.dimensions.put(dim, list);
        }
        return dto;
    }

    private static void writeDtoToFile(String worldId, CacheFileDto dto) {
        try {
            Path configDir = FabricLoader.getInstance().getConfigDir().resolve("containersearch");
            Files.createDirectories(configDir);
            Path cacheFile = configDir.resolve("cache_" + worldId + ".json");
            Path tempFile = configDir.resolve("cache_" + worldId + ".json.tmp");

            try (Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                GSON.toJson(dto, writer);
            }
            Files.move(tempFile, cacheFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            System.err.println("[ContainerSearch] Failed to save persistent cache: " + e.getMessage());
        }
    }
}
