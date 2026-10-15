package com.example.containersearch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;

/**
 * Utility class for rendering container highlights through obstacles via the Gizmos API in Minecraft 26.3.
 */
public class RenderUtils {

    /**
     * Hitbox expansion amount (0.02 blocks) to completely eliminate
     * Z-fighting and polygon/line flickering when used concurrently with ChestESP.
     */
    public static final double BOX_EXPAND = 0.02;

    /**
     * Renders a distinctive neon green outline through walls with translucent face fill.
     * The thickened stroke (3.2f) cleanly frames the container over outlines from other mods (including ChestESP).
     *
     * @param aabb the bounding box of the container in world coordinates
     */
    public static void renderHighlightGizmo(AABB aabb) {
        // Vivid neon green outline (0xFF00FF66) and soft glowing fill (0x3000FF66)
        int strokeColor = 0xFF00FF66;
        int fillColor   = 0x3000FF66;
        float strokeWidth = 3.2f;

        // setAlwaysOnTop() disables depth testing, ensuring through-wall visibility
        Gizmos.cuboid(aabb, GizmoStyle.strokeAndFill(strokeColor, strokeWidth, fillColor))
              .setAlwaysOnTop();
    }

    /**
     * Expands an AABB bounding box by BOX_EXPAND (0.02) in all directions.
     */
    public static AABB expand(AABB box) {
        return box.inflate(BOX_EXPAND);
    }

    /**
     * Expands an AABB bounding box by the specified amount in all directions.
     */
    public static AABB expand(AABB box, double amount) {
        return box.inflate(amount);
    }

    /**
     * Calculates the exact AABB bounding box of a container in world coordinates,
     * accounting for single/double chests, and applies an outer expansion (+0.02 blocks) to eliminate Z-fighting.
     */
    public static AABB getContainerBoundingBox(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        AABB box;

        // Regular chests and trapped chests (accounting for connected double chests)
        if (state.getBlock() instanceof ChestBlock) {
            double minX = 0.0625;
            double minY = 0.0;
            double minZ = 0.0625;
            double maxX = 0.9375;
            double maxY = 0.875;
            double maxZ = 0.9375;

            if (state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                Direction connected = ChestBlock.getConnectedDirection(state);
                if (connected == Direction.WEST) minX = 0.0;
                else if (connected == Direction.EAST) maxX = 1.0;
                else if (connected == Direction.NORTH) minZ = 0.0;
                else if (connected == Direction.SOUTH) maxZ = 1.0;
            }

            box = new AABB(
                    pos.getX() + minX, pos.getY() + minY, pos.getZ() + minZ,
                    pos.getX() + maxX, pos.getY() + maxY, pos.getZ() + maxZ
            );
        } else if (state.getBlock() instanceof EnderChestBlock) {
            // Ender chests (single chest volume)
            box = new AABB(
                    pos.getX() + 0.0625, pos.getY(), pos.getZ() + 0.0625,
                    pos.getX() + 0.9375, pos.getY() + 0.875, pos.getZ() + 0.9375
            );
        } else {
            // Shulker boxes, barrels, dispensers, droppers, hoppers, furnaces — standard 1x1x1 cube block
            box = new AABB(pos);
        }

        // Outer expansion (+0.02 blocks in all directions) to prevent Z-fighting and shimmering with ChestESP
        return box.inflate(BOX_EXPAND);
    }
}
