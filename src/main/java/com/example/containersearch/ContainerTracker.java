package com.example.containersearch;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks player interaction with containers and captures slot contents from opened container screens.
 */
public class ContainerTracker {

    private static BlockPos lastInteractedPos = null;
    private static long lastInteractedTime = 0;
    private static String lastInteractedDimension = "";

    /**
     * Records the coordinates and dimension of the block the player right-clicked.
     */
    public static void onBlockInteracted(Level level, BlockPos pos) {
        if (level == null || pos == null) return;
        lastInteractedPos = pos.immutable();
        lastInteractedTime = System.currentTimeMillis();
        lastInteractedDimension = level.dimension().identifier().toString();
    }

    /**
     * Records interaction with a container entity (e.g. chest minecart).
     */
    public static void onEntityInteracted(Level level, BlockPos pos) {
        onBlockInteracted(level, pos);
    }

    /**
     * Extracts items from slots of the opened container screen and saves them to the cache.
     */
    public static void updateContainerItems(AbstractContainerScreen<?> screen) {
        if (screen == null) return;

        // Ignore player's own inventory and creative inventory screens
        if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        BlockPos targetPos = null;
        String dimension = client.level.dimension().identifier().toString();

        // 1. Check last interacted block (within 5 seconds)
        if (lastInteractedPos != null && (System.currentTimeMillis() - lastInteractedTime < 5000)) {
            targetPos = lastInteractedPos;
            dimension = lastInteractedDimension;
        }
        // 2. Fallback: check the block the player is directly looking at
        else if (client.hitResult instanceof BlockHitResult hitResult) {
            targetPos = hitResult.getBlockPos();
        }

        if (targetPos == null) return;

        // Collect items from container slots (excluding player's 36 inventory slots)
        List<ItemInfo> items = new ArrayList<>();
        boolean foundContainerSlot = false;

        for (Slot slot : screen.getMenu().slots) {
            if (slot.container instanceof Inventory) {
                // This is player's personal inventory slot — skip
                continue;
            }
            foundContainerSlot = true;

            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                String fullId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                String pathId = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                String displayName = stack.getHoverName().getString();
                String translationKey = stack.getItem().getDescriptionId();

                items.add(new ItemInfo(fullId, pathId, displayName, translationKey, stack.getCount()));
            }
        }

        // Do not save if no container slots were found in the screen
        if (!foundContainerSlot) return;

        // Save contents for current coordinates
        ContainerCache.put(dimension, targetPos, items);

        // If this is a double chest, automatically mirror contents to the adjacent half
        BlockState state = client.level.getBlockState(targetPos);
        if (state.getBlock() instanceof ChestBlock) {
            Direction connected = ChestBlock.getConnectedDirection(state);
            if (connected != null) {
                BlockPos secondPos = targetPos.relative(connected);
                ContainerCache.put(dimension, secondPos, items);
            }
        }
    }
}
