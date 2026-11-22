package com.greenbeanz.client.gui.modules;

import com.greenbeanz.client.gui.SMLSMenuScreen;
import com.greenbeanz.client.modules.ChatAlertsModule;
import com.greenbeanz.client.utils.AlertWordlist;
import com.greenbeanz.client.utils.SMLSConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Settings local to the Chat Alerts module. */
public final class ChatAlertsScreen extends SMLSMenuScreen {
    private final ChatAlertsModule module;
    private Component wordlistStatus;

    public ChatAlertsScreen(Screen parent, ChatAlertsModule module) {
        super(Component.translatable("SMLS.module.chat_alerts.name"), parent);
        this.module = module;
        wordlistStatus = wordlistStatusText();
    }

    @Override
    protected void init() {
        configureScrollableContent(module, scaled(78));
        int controlWidth = scaled(200);
        int x = width / 2 - controlWidth / 2;
        addScrollableChild(CycleButton.builder((Boolean value) -> Component.translatable(value ? "SMLS.option.on" : "SMLS.option.off")
                        .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED), SMLSConfig.alertsEnabled()).withValues(true, false)
                .create(x, contentY(0), controlWidth, STANDARD_BUTTON_HEIGHT, Component.translatable("SMLS.module.chat_alerts.toggle"),
                        (button, value) -> {
                            if (!SMLSConfig.setAlertsEnabled(value)) {
                                button.setValue(SMLSConfig.alertsEnabled());
                                wordlistStatus = Component.translatable("SMLS.validation.config.save_failed");
                            } else {
                                wordlistStatus = wordlistStatusText();
                            }
                        }), 0);
        addScrollableChild(Button.builder(Component.translatable("SMLS.module.chat_alerts.reload"), button -> {
            wordlistStatus = wordlistStatusText(ChatAlertsModule.reloadWordlistResult());
        }).bounds(x, contentY(scaled(30)), controlWidth, STANDARD_BUTTON_HEIGHT).build(), scaled(30));
        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> onClose())
                .bounds(width / 2 - scaled(75), footerButtonY(), scaled(150), STANDARD_BUTTON_HEIGHT).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        renderMenuBackground(context);
        renderModuleHeader(context, module);
        beginContentScissor(context);
        int statusY = contentY(scaled(62));
        if (isContentVisible(statusY, font.lineHeight)) {
            context.centeredText(font, wordlistStatus, width / 2, statusY, 0xFFAAAAAA);
        }
        endContentScissor(context);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private Component wordlistStatusText() {
        return wordlistStatusText(ChatAlertsModule.lastWordlistLoadResult());
    }

    private Component wordlistStatusText(AlertWordlist.LoadResult result) {
        int count = result.wordCount();
        if (result.status() == AlertWordlist.LoadStatus.FAILED) {
            return Component.translatable("SMLS.module.chat_alerts.reload_failed", count);
        }
        return Component.translatable(count == 1 ? "SMLS.module.chat_alerts.words.one" : "SMLS.module.chat_alerts.words.many", count);
    }
}
