package com.example.containersearch.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ObjectSelectionList;

/**
 * Scrollable item selection list based on ObjectSelectionList.
 */
public class ItemSelectionList extends ObjectSelectionList<ItemEntry> {

    public ItemSelectionList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
        super(minecraft, width, height, y, itemHeight);
    }

    @Override
    public int getRowWidth() {
        return 280;
    }

    @Override
    protected int scrollBarX() {
        return this.width / 2 + 145;
    }

    public void clearList() {
        this.clearEntries();
    }

    public void addListEntry(ItemEntry entry) {
        this.addEntry(entry);
    }
}
