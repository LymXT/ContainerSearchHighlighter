package com.example.containersearch.gui;

import com.example.containersearch.ContainerCache;
import com.example.containersearch.ContainerSearchClient;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Item selection and container search screen.
 * Contains a real-time filter text box and a scrollable list of items with rendered icons.
 */
public class SearchScreen extends Screen {

    private EditBox searchBox;
    private ItemSelectionList itemList;
    private final List<ItemEntry> allEntries = new ArrayList<>();

    public SearchScreen() {
        super(Component.translatable("gui.containersearch.title"));
    }

    @Override
    protected void init() {
        int boxWidth = 280;
        int boxX = (this.width - boxWidth) / 2;
        int boxY = 30;

        // 1. Real-time item input / filter text box
        searchBox = new EditBox(this.font, boxX, boxY, boxWidth, 20, Component.translatable("gui.containersearch.title"));
        searchBox.setMaxLength(64);
        searchBox.setValue(ContainerSearchClient.getSearchQuery());
        searchBox.setHint(Component.translatable("gui.containersearch.hint"));
        searchBox.setResponder(this::onFilterChanged);
        this.addRenderableWidget(searchBox);
        this.setInitialFocus(searchBox);

        // 2. Scrollable item selection list
        int listTop = 54;
        int listBottom = this.height - 34;
        int listHeight = Math.max(24, listBottom - listTop);

        itemList = new ItemSelectionList(this.minecraft, this.width, listHeight, listTop, 24);
        this.addRenderableWidget(itemList);

        // 3. Initialize registry item cache (BuiltInRegistries.ITEM)
        if (allEntries.isEmpty()) {
            for (Item item : BuiltInRegistries.ITEM) {
                if (item == Items.AIR) continue;
                ItemStack stack = new ItemStack(item);
                String displayName = stack.getHoverName().getString();
                String fullId = BuiltInRegistries.ITEM.getKey(item).toString();
                String pathId = BuiltInRegistries.ITEM.getKey(item).getPath();
                allEntries.add(new ItemEntry(this, stack, displayName, fullId, pathId));
            }
        }

        // 4. Populate list based on current query
        filterEntries(searchBox.getValue());

        // 5. Bottom control buttons
        int btnY = this.height - 28;
        int btnWidth = 135;

        // "Search" button
        Button searchButton = Button.builder(
                Component.translatable("gui.containersearch.search"),
                btn -> applySearch()
        ).bounds(this.width / 2 - btnWidth - 5, btnY, btnWidth, 20).build();
        this.addRenderableWidget(searchButton);

        // "Reset Highlight" button
        Button clearButton = Button.builder(
                Component.translatable("gui.containersearch.reset_highlight"),
                btn -> resetSearch()
        ).bounds(this.width / 2 + 5, btnY, btnWidth, 20).build();
        this.addRenderableWidget(clearButton);
    }

    private void onFilterChanged(String newFilter) {
        filterEntries(newFilter);
    }

    private void filterEntries(String query) {
        if (itemList == null) return;
        itemList.clearList();
        for (ItemEntry entry : allEntries) {
            if (entry.matches(query)) {
                itemList.addListEntry(entry);
            }
        }
        itemList.setScrollAmount(0);
    }

    private void applySearch() {
        if (searchBox != null) {
            ContainerSearchClient.setSearchQuery(searchBox.getValue());
        }
        this.onClose();
    }

    private void resetSearch() {
        ContainerSearchClient.setSearchQuery("");
        this.onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // [Enter] — apply text query from search box
        if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
            applySearch();
            return true;
        }
        // [ESC] — reset highlight and close screen
        if (event.key() == InputConstants.KEY_ESCAPE) {
            resetSearch();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        // Dimmed semi-transparent background
        extractor.fill(0, 0, this.width, this.height, 0xA0000000);

        // Screen header
        extractor.centeredText(
                this.font,
                Component.literal("§6§lВибір предмета для пошуку §e(Container Search)"),
                this.width / 2,
                6,
                0xFFFFFFFF
        );

        // Results and cached container count summary
        int matchingItems = (itemList != null) ? itemList.children().size() : 0;
        int cachedContainers = ContainerCache.getContainerCount();
        String status = "§7Предметів: §a" + matchingItems + "§7  •  Контейнерів у світі: §e" + cachedContainers;
        extractor.centeredText(this.font, Component.literal(status), this.width / 2, 18, 0xFFAAAAAA);

        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
