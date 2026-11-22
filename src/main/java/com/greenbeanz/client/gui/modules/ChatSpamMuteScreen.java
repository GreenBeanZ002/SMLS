package com.greenbeanz.client.gui.modules;

import com.greenbeanz.client.gui.SMLSMenuScreen;
import com.greenbeanz.client.modules.ChatSpamMuteModule;
import com.greenbeanz.client.utils.SMLSConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Settings screen for the client-side chat spam filters. */
public final class ChatSpamMuteScreen extends SMLSMenuScreen {
    private final ChatSpamMuteModule module;
    private Component saveStatus = Component.empty();

    public ChatSpamMuteScreen(Screen parent, ChatSpamMuteModule module) {
        super(Component.translatable("SMLS.module.chat_spam.name"), parent);
        this.module = module;
    }

    @Override
    protected void init() {
        configureScrollableContent(module, scaled(104));
        int controlWidth = Math.min(scaled(240), width - scaled(48));
        int x = width / 2 - controlWidth / 2;
        addScrollableChild(CycleButton.builder((Boolean value) -> Component.translatable(value ? "SMLS.option.on" : "SMLS.option.off")
                        .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED), SMLSConfig.tradeChatMuted()).withValues(true, false)
                .create(x, contentY(0), controlWidth, STANDARD_BUTTON_HEIGHT, Component.translatable("SMLS.module.chat_spam.trade_chat_toggle"),
                        (button, value) -> {
                            if (!SMLSConfig.setTradeChatMuted(value)) {
                                button.setValue(SMLSConfig.tradeChatMuted());
                                saveStatus = Component.translatable("SMLS.validation.config.save_failed");
                            } else {
                                saveStatus = Component.empty();
                            }
                        }), 0);
        addScrollableChild(CycleButton.builder((Boolean value) -> Component.translatable(value ? "SMLS.option.on" : "SMLS.option.off")
                        .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED), SMLSConfig.serverMessagesMuted()).withValues(true, false)
                .create(x, contentY(scaled(30)), controlWidth, STANDARD_BUTTON_HEIGHT,
                        Component.translatable("SMLS.module.chat_spam.server_messages_toggle"),
                        (button, value) -> {
                            if (!SMLSConfig.setServerMessagesMuted(value)) {
                                button.setValue(SMLSConfig.serverMessagesMuted());
                                saveStatus = Component.translatable("SMLS.validation.config.save_failed");
                            } else {
                                saveStatus = Component.empty();
                            }
                        }), scaled(30));
        addScrollableChild(CycleButton.builder((Boolean value) -> Component.translatable(
                                value ? "SMLS.option.on" : "SMLS.option.off")
                        .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED),
                        SMLSConfig.staffCommandFeedbackMuted()).withValues(true, false)
                .create(x, contentY(scaled(60)), controlWidth, STANDARD_BUTTON_HEIGHT,
                        Component.translatable("SMLS.module.chat_spam.staff_command_feedback_toggle"),
                        (button, value) -> {
                            if (!SMLSConfig.setStaffCommandFeedbackMuted(value)) {
                                button.setValue(SMLSConfig.staffCommandFeedbackMuted());
                                saveStatus = Component.translatable("SMLS.validation.config.save_failed");
                            } else {
                                saveStatus = Component.empty();
                            }
                        }), scaled(60));
        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> onClose())
                .bounds(width / 2 - scaled(75), footerButtonY(), scaled(150), STANDARD_BUTTON_HEIGHT).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        renderMenuBackground(context);
        renderModuleHeader(context, module);
        beginContentScissor(context);
        int statusY = contentY(scaled(90));
        if (!saveStatus.getString().isEmpty() && isContentVisible(statusY, font.lineHeight)) {
            context.centeredText(font, saveStatus, width / 2, statusY, 0xFFFF5555);
        }
        endContentScissor(context);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }
}
