package com.greenbeanz.client.modules;

import com.greenbeanz.client.gui.modules.BackgroundsScreen;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Turns the optional animated SMLS menu backgrounds on and off. */
public final class BackgroundsModule extends SMLSModule {
    public BackgroundsModule() {
        super(StaffRank.HELPER);
    }

    @Override
    public Component displayName() {
        return Component.translatable("SMLS.module.backgrounds.name");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(Items.GLASS_PANE);
    }

    @Override
    public List<Component> description() {
        return List.of(
                Component.translatable("SMLS.module.backgrounds.description.1")
        );
    }

    @Override
    public ModuleCategory category() {
        return ModuleCategory.JOKE;
    }

    @Override
    public void openScreen(Minecraft client, Screen parent) {
        client.gui.setScreen(new BackgroundsScreen(parent, this));
    }

    @Override
    public void register() {
        // Nothing to hook: the background is drawn by the menu screens and reads BackgroundsSettings.
    }
}