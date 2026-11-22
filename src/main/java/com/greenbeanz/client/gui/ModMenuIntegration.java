package com.greenbeanz.client.gui;

import com.greenbeanz.client.SMLSClient;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new SMLSHomeScreen(SMLSClient.modules(), parent);
    }
}
