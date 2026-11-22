package com.greenbeanz.client.gui.modules;

import com.greenbeanz.client.gui.SMLSMenuScreen;
import com.greenbeanz.client.modules.BackgroundsModule;
import com.greenbeanz.client.utils.SMLSConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Toggles for the animated SMLS menu backgrounds. */
public final class BackgroundsScreen extends SMLSMenuScreen {
    private final BackgroundsModule module;

    public BackgroundsScreen(Screen parent, BackgroundsModule module) {
        super(module.displayName(), parent);
        this.module = module;
    }

    @Override
    protected void init() {
        configureScrollableContent(module, scaled(60));
        int formWidth = Math.min(scaled(360), width - scaled(48));
        int formX = (width - formWidth) / 2;

        int rowStep = STANDARD_BUTTON_HEIGHT + scaled(6);
        int nodeY = scaled(14);
        int logoY = nodeY + rowStep;
        int dotsY = logoY + rowStep;

        addToggle(formX, formWidth, nodeY, "SMLS.backgrounds.node_network",
                SMLSConfig::nodeNetworkBackground, SMLSConfig::setNodeNetworkBackground);

        addToggle(formX, formWidth, logoY, "SMLS.backgrounds.bouncing_logo",
                SMLSConfig::bouncingLogoBackground, SMLSConfig::setBouncingLogoBackground);

        addToggle(formX, formWidth, dotsY, "SMLS.backgrounds.fleeing_dots",
                SMLSConfig::fleeingDotsBackground, SMLSConfig::setFleeingDotsBackground);

        addRenderableWidget(Button.builder(Component.translatable("SMLS.backgrounds.button.back"),
                        button -> minecraft.gui.setScreen(parent))
                .bounds(width / 2 - scaled(75), footerButtonY(), scaled(150), STANDARD_BUTTON_HEIGHT).build());
    }

    private void addToggle(int x, int w, int y, String key, BooleanSupplier getter, Consumer<Boolean> setter) {
        addScrollableChild(Button.builder(onOffLabel(key, getter.getAsBoolean()), button -> {
                    setter.accept(!getter.getAsBoolean());
                    button.setMessage(onOffLabel(key, getter.getAsBoolean()));
                })
                .bounds(x, contentY(y), w, STANDARD_BUTTON_HEIGHT).build(), y);
    }

    private static Component onOffLabel(String key, boolean enabled) {
        return Component.translatable(key,
                Component.translatable(enabled ? "SMLS.option.on" : "SMLS.option.off"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        renderMenuBackground(context);
        renderModuleHeader(context, module);
        beginContentScissor(context);
        endContentScissor(context);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }
}