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
 * Відстежує взаємодію гравця з контейнерами та отримує їх вміст із відкритих екранів.
 */
public class ContainerTracker {

    private static BlockPos lastInteractedPos = null;
    private static long lastInteractedTime = 0;
    private static String lastInteractedDimension = "";

    /**
     * Фіксує координати та вимір блоку, на який гравець натиснув правою кнопкою миші.
     */
    public static void onBlockInteracted(Level level, BlockPos pos) {
        if (level == null || pos == null) return;
        lastInteractedPos = pos.immutable();
        lastInteractedTime = System.currentTimeMillis();
        lastInteractedDimension = level.dimension().identifier().toString();
    }

    /**
     * Фіксує взаємодію із сутністю-контейнером (наприклад, вагонетка із сундуком).
     */
    public static void onEntityInteracted(Level level, BlockPos pos) {
        onBlockInteracted(level, pos);
    }

    /**
     * Витягує предмети із слотів відкритого меню контейнера та зберігає їх у кеш.
     */
    public static void updateContainerItems(AbstractContainerScreen<?> screen) {
        if (screen == null) return;

        // Ігноруємо власний інвентар гравця та режим креативу
        if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        BlockPos targetPos = null;
        String dimension = client.level.dimension().identifier().toString();

        // 1. Перевіряємо останній натиснутий блок (в межах 5 секунд)
        if (lastInteractedPos != null && (System.currentTimeMillis() - lastInteractedTime < 5000)) {
            targetPos = lastInteractedPos;
            dimension = lastInteractedDimension;
        }
        // 2. Fallback: перевіряємо блок, на який спрямований погляд гравця
        else if (client.hitResult instanceof BlockHitResult hitResult) {
            targetPos = hitResult.getBlockPos();
        }

        if (targetPos == null) return;

        // Збираємо предмети зі слотів відкритого контейнера (виключаючи 36 слотів інвентаря гравця)
        List<ItemInfo> items = new ArrayList<>();
        boolean foundContainerSlot = false;

        for (Slot slot : screen.getMenu().slots) {
            if (slot.container instanceof Inventory) {
                // Це слот особистого інвентаря гравця — пропускаємо
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

        // Якщо в екрані взагалі немає слотів контейнера — не зберігаємо
        if (!foundContainerSlot) return;

        // Зберігаємо вміст для поточної координати
        ContainerCache.put(dimension, targetPos, items);

        // Якщо це подвійний сундук — автоматично дублюємо вміст для суміжної половинки
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
