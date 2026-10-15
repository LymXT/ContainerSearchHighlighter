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
 * Утиліти для рендерингу підсвічування контейнерів скрізь стіни через Gizmos API у Minecraft 26.3.
 */
public class RenderUtils {

    /**
     * Величина зовнішнього розширення хитбокса (0.02 блока) для повного усунення
     * Z-fighting та мерехтіння полігонів/ліній при сумісному використанні з ChestESP.
     */
    public static final double BOX_EXPAND = 0.02;

    /**
     * Отрисовує виразний неоново-зелений контур крізь стіни із напівпрозорим заповненням граней.
     * Потовщений контур (3.2f) акуратно обрамляє сундук поверх контурів інших модів (зокрема ChestESP).
     *
     * @param aabb обмежувальний об'єм контейнера у світових координатах
     */
    public static void renderHighlightGizmo(AABB aabb) {
        // Яскравий насичений неоново-зелений контур (0xFF00FF66) та м'яке напівпрозоре заповнення (0x3000FF66)
        int strokeColor = 0xFF00FF66;
        int fillColor   = 0x3000FF66;
        float strokeWidth = 3.2f;

        // setAlwaysOnTop() відключає depth test, забезпечуючи видимість крізь будь-які перешкоди
        Gizmos.cuboid(aabb, GizmoStyle.strokeAndFill(strokeColor, strokeWidth, fillColor))
              .setAlwaysOnTop();
    }

    /**
     * Розширює AABB-бокс на величину BOX_EXPAND (0.02) в усі боки.
     */
    public static AABB expand(AABB box) {
        return box.inflate(BOX_EXPAND);
    }

    /**
     * Розширює AABB-бокс на задану величину в усі боки.
     */
    public static AABB expand(AABB box, double amount) {
        return box.inflate(amount);
    }

    /**
     * Обчислює точний AABB-об'єм контейнера у світових координатах з урахуванням одинарних/подвійних сундуків
     * та додає зовнішнє розширення (+0.02 блока) для усунення Z-fighting з іншими модами.
     */
    public static AABB getContainerBoundingBox(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        AABB box;

        // Звичайні сундуки та сундуки-ловушки (з урахуванням подвійних сундуків)
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
            // Ендер-сундуки (одинарний об'єм)
            box = new AABB(
                    pos.getX() + 0.0625, pos.getY(), pos.getZ() + 0.0625,
                    pos.getX() + 0.9375, pos.getY() + 0.875, pos.getZ() + 0.9375
            );
        } else {
            // Для шалкерів, бочок, роздавачів, воронок, пічок — стандартний блок 1x1x1
            box = new AABB(pos);
        }

        // Зовнішнє розширення (+0.02 блока в усі боки) для запобігання Z-fighting та мерехтінню з ChestESP
        return box.inflate(BOX_EXPAND);
    }
}
