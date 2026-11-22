package com.greenbeanz.client.modules;

import com.greenbeanz.client.gui.modules.ContainerScanScreen;
import com.greenbeanz.client.modules.session.AbstractCoreProtectScanModule;
import com.greenbeanz.client.parser.CoreProtectLookupParser;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Scans container activity with CoreProtect and summarizes who took and added what. */
public final class ContainerScanModule extends AbstractCoreProtectScanModule {
    public ContainerScanModule() {
        super(
                "§8[§6SMLS - Containers§8] §7",
                "container_scan",
                "Container Scan",
                "SMLS.chat.containers",
                "/SMLS containers cancel",
                CoreProtectLookupParser.ScanKind.CONTAINER
        );
    }

    @Override
    public Component displayName() {
        return Component.translatable("SMLS.module.containers.name");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(Items.CHEST);
    }

    @Override
    public List<Component> description() {
        return List.of(
                Component.translatable("SMLS.module.containers.description.1"),
                Component.translatable("SMLS.module.containers.description.2")
        );
    }

    @Override
    public ModuleCategory category() {
        return ModuleCategory.GENERAL;
    }

    @Override
    public void openScreen(Minecraft client, Screen parent) {
        client.gui.setScreen(new ContainerScanScreen(parent, this));
    }
}
