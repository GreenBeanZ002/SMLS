package com.greenbeanz.client.gui.modules;

import com.greenbeanz.client.gui.SMLSMenuScreen;
import com.greenbeanz.client.modules.ActivityWaveModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Form for invoking an activity wave check. */
public final class ActivityWaveScreen extends SMLSMenuScreen {
    private final ActivityWaveModule module;
    private EditBox ignsField;
    private Button submitButton;
    private Component validationMessage = Component.empty();

    public ActivityWaveScreen(Screen parent, ActivityWaveModule module) {
        super(Component.translatable("SMLS.module.activity.name"), parent);
        this.module = module;
    }

    @Override
    protected void init() {
        configureScrollableContent(module, scaled(64));
        int formWidth = Math.min(scaled(360), width - scaled(48));
        int formX = (width - formWidth) / 2;
        ignsField = addScrollableChild(new EditBox(font, formX, contentY(scaled(14)), formWidth, STANDARD_BUTTON_HEIGHT,
                Component.translatable("SMLS.field.player_igns")), scaled(14));
        ignsField.setMaxLength(1024);
        ignsField.setSuggestion(Component.translatable("SMLS.placeholder.player_names_many").getString());
        ignsField.setResponder(value -> {
            ignsField.setSuggestion(value.isEmpty()
                    ? Component.translatable("SMLS.placeholder.player_names_many").getString() : null);
            validationMessage = Component.empty();
        });
        setInitialFocus(ignsField);

        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> onClose())
                .bounds(leftPairedButtonX(), footerButtonY(), pairedButtonWidth(), STANDARD_BUTTON_HEIGHT).build());
        submitButton = registerCommandControl(addRenderableWidget(Button.builder(
                        Component.translatable("SMLS.button.check_activity"), button -> submit())
                .bounds(rightPairedButtonX(), footerButtonY(), pairedButtonWidth(), STANDARD_BUTTON_HEIGHT).build()));
    }

    private void submit() {
        String input = ignsField.getValue().trim();
        if (input.isEmpty()) {
            validationMessage = Component.translatable("SMLS.validation.player_igns");
            return;
        }
        ActivityWaveModule.SubmitStatus status = module.submit(minecraft, input);
        if (status == ActivityWaveModule.SubmitStatus.STARTED) {
            closeToGame();
            return;
        }
        validationMessage = switch (status) {
            case INVALID -> Component.translatable("SMLS.validation.player_igns");
            case TOO_MANY -> Component.translatable("SMLS.chat.activity.too_many", ActivityWaveModule.MAX_PLAYERS);
            case BUSY -> Component.translatable("SMLS.validation.operation.busy");
            case BLOCKED -> Component.translatable("SMLS.validation.operation.blocked");
            case FAILED -> Component.translatable("SMLS.validation.operation.start_failed");
            case STARTED -> Component.empty();
        };
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        renderMenuBackground(context);
        renderModuleHeader(context, module);
        beginContentScissor(context);
        int labelY = contentY(0);
        if (isContentVisible(labelY, font.lineHeight)) {
            context.text(font, Component.translatable("SMLS.field.player_igns.label"), ignsField.getX(), labelY, 0xFFCCCCCC);
        }
        int validationY = contentY(scaled(48));
        if (!validationMessage.getString().isEmpty() && isContentVisible(validationY, font.lineHeight)) {
            context.centeredText(font, validationMessage, width / 2, validationY, 0xFFFF5555);
        }
        endContentScissor(context);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }
}
