package com.example.containersearch.gui;

import com.example.containersearch.ContainerSearchClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * Елемент списку предметів із реальною іконкою та локалізованою назвою.
 */
public class ItemEntry extends ObjectSelectionList.Entry<ItemEntry> {

    private final SearchScreen screen;
    private final ItemStack itemStack;
    private final String displayName;
    private final String fullId;
    private final String pathId;
    private final String searchKey;

    public ItemEntry(SearchScreen screen, ItemStack itemStack, String displayName, String fullId, String pathId) {
        this.screen = screen;
        this.itemStack = itemStack;
        this.displayName = displayName;
        this.fullId = fullId;
        this.pathId = pathId;
        this.searchKey = (displayName + " " + pathId + " " + fullId).toLowerCase(Locale.ROOT);
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean matches(String query) {
        if (query == null || query.isBlank()) return true;
        return searchKey.contains(query.trim().toLowerCase(Locale.ROOT));
    }

    @Override
    public Component getNarration() {
        return Component.literal(displayName);
    }

    @Override
    public void extractContent(GuiGraphicsExtractor extractor, int mouseX, int mouseY, boolean hovered, float partialTick) {
        int x = getContentX();
        int y = getContentY();
        int right = getContentRight();
        int bottom = getContentBottom();

        // Підсвічування фону при наведенні курсору
        if (hovered) {
            extractor.fill(x, y, right, bottom, 0x40FFFFFF);
        }

        // Відображення іконки предмета (drawItem)
        int iconX = x + 4;
        int iconY = y + (getHeight() - 16) / 2;
        extractor.item(this.itemStack, iconX, iconY);

        // Відображення локалізованої назви предмета
        int textX = iconX + 22;
        int textY = y + (getHeight() - 9) / 2;
        int textColor = hovered ? 0xFFFFFF55 : 0xFFFFFFFF;
        extractor.text(Minecraft.getInstance().font, this.displayName, textX, textY, textColor);

        // Вспливаюча підказка (Tooltip) при наведенні
        if (hovered) {
            extractor.setTooltipForNextFrame(Minecraft.getInstance().font, this.itemStack, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) { // Лівий клік миші (LMB)
            // Автоматично обираємо цей предмет для пошуку та закриваємо меню
            ContainerSearchClient.setSearchQuery(this.displayName);
            screen.onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
