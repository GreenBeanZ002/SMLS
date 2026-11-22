package com.greenbeanz.client.gui.modules;

import com.greenbeanz.client.gui.SMLSMenuScreen;
import com.greenbeanz.client.gui.widgets.DropdownWidget;
import com.greenbeanz.client.modules.DonorPetModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Form for giving a donor their pet permission. */
public final class DonorPetScreen extends SMLSMenuScreen {
    private final DonorPetModule module;
    private EditBox ignField;
    private String pet = DonorPetModule.pets().get(0);
    private Button submitButton;
    private Component validationMessage = Component.empty();

    public DonorPetScreen(Screen parent, DonorPetModule module) {
        super(Component.translatable("SMLS.module.donor_pet.name"), parent);
        this.module = module;
    }

    @Override
    protected void init() {
        configureScrollableContent(module, scaled(94));
        int formWidth = Math.min(scaled(360), width - scaled(48));
        int formX = (width - formWidth) / 2;
        ignField = addScrollableChild(new EditBox(font, formX, contentY(scaled(14)), formWidth, STANDARD_BUTTON_HEIGHT,
                Component.translatable("SMLS.field.player_ign")), scaled(14));
        ignField.setMaxLength(16);
        ignField.setSuggestion(Component.translatable("SMLS.placeholder.player_name").getString());
        ignField.setResponder(value -> {
            ignField.setSuggestion(value.isEmpty()
                    ? Component.translatable("SMLS.placeholder.player_name").getString() : null);
            validationMessage = Component.empty();
        });
        setInitialFocus(ignField);

        addScrollableDropdownChild(DropdownWidget.builder(
                        Component.translatable("SMLS.field.pet"), DonorPetModule.pets(), pet,
                        Component::literal, (dropdown, value) -> pet = value)
                .dimensions(formX, contentY(scaled(48)), formWidth, STANDARD_BUTTON_HEIGHT)
                .showOptionLabel(true)
                .build(), scaled(48));

        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> onClose())
                .bounds(leftPairedButtonX(), footerButtonY(), pairedButtonWidth(), STANDARD_BUTTON_HEIGHT).build());
        submitButton = registerCommandControl(addRenderableWidget(Button.builder(
                        Component.translatable("SMLS.button.give_pet"), button -> submit())
                .bounds(rightPairedButtonX(), footerButtonY(), pairedButtonWidth(), STANDARD_BUTTON_HEIGHT).build()));
    }

    private void submit() {
        String input = ignField.getValue().trim();
        if (input.isEmpty()) {
            validationMessage = Component.translatable("SMLS.validation.player_ign");
            return;
        }
        DonorPetModule.SubmitStatus status = module.submit(minecraft, input, pet);
        if (status == DonorPetModule.SubmitStatus.STARTED) {
            closeToGame();
            return;
        }
        validationMessage = switch (status) {
            case INVALID -> Component.translatable("SMLS.validation.player_ign");
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
            context.text(font, Component.translatable("SMLS.field.player_ign.label"), ignField.getX(), labelY, 0xFFCCCCCC);
        }
        int validationY = contentY(scaled(80));
        if (!validationMessage.getString().isEmpty() && isContentVisible(validationY, font.lineHeight)) {
            context.centeredText(font, validationMessage, width / 2, validationY, 0xFFFF5555);
        }
        endContentScissor(context);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }
}
