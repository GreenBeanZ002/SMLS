package com.greenbeanz.client.modules;

import com.greenbeanz.client.gui.modules.GriefScanScreen;
import com.greenbeanz.client.modules.session.AbstractCoreProtectScanModule;
import com.greenbeanz.client.parser.CoreProtectLookupParser;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Scans block activity with CoreProtect and summarizes who broke and placed what. */
public final class GriefScanModule extends AbstractCoreProtectScanModule {
    public GriefScanModule() {
        super(
                "§8[§6SMLS - Griefs§8] §7",
                "grief_scan",
                "Grief Scan",
                "SMLS.chat.griefs",
                "/SMLS griefs cancel",
                CoreProtectLookupParser.ScanKind.BLOCK
        );
    }

    @Override
    public Component displayName() {
        return Component.translatable("SMLS.module.griefs.name");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(Items.IRON_PICKAXE);
    }

    @Override
    public List<Component> description() {
        return List.of(
                Component.translatable("SMLS.module.griefs.description.1"),
                Component.translatable("SMLS.module.griefs.description.2")
        );
    }

    @Override
    public ModuleCategory category() {
        return ModuleCategory.GENERAL;
    }

    @Override
    public void openScreen(Minecraft client, Screen parent) {
        client.gui.setScreen(new GriefScanScreen(parent, this));
    }
}
