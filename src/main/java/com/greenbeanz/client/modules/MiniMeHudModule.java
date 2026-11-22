package com.greenbeanz.client.modules;

import com.greenbeanz.client.gui.modules.MiniMeHudScreen;
import com.greenbeanz.client.hud.MiniMeHudOverlay;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Configures the optional animated AdMinis shown on the in-game HUD. */
public final class MiniMeHudModule extends SMLSModule {
    public MiniMeHudModule() {
        super(StaffRank.HELPER);
    }

    @Override
    public Component displayName() {
        return Component.translatable("SMLS.module.mini_me_hud.name");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(Items.PLAYER_HEAD);
    }

    @Override
    public List<Component> description() {
        return List.of(
                Component.translatable("SMLS.module.mini_me_hud.description.1"),
                Component.translatable("SMLS.module.mini_me_hud.description.2")
        );
    }

    @Override
    public ModuleCategory category() {
        return ModuleCategory.JOKE;
    }

    @Override
    public void openScreen(Minecraft client, Screen parent) {
        client.gui.setScreen(new MiniMeHudScreen(parent, this));
    }

    @Override
    public void register() {
        MiniMeHudOverlay.register();
    }
}
