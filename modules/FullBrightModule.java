package com.yourcheat.modules;

import com.yourcheat.gui.ClickGUI;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public class FullBrightModule implements IModule {

    private boolean enabled = false;
    private static final double FULL_BRIGHT_GAMMA = 9999.0;
    private double previousGamma = 1.0;

    @Override public String getName() { return "FullBright"; }
    @Override public boolean isEnabled() { return enabled; }

    @Override
    public void setEnabled(boolean v) {
        enabled = v;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null) return;
        if (v) {
            previousGamma = mc.options.getGamma().getValue();
            mc.options.getGamma().setValue(FULL_BRIGHT_GAMMA);
        } else {
            mc.options.getGamma().setValue(previousGamma);
        }
    }

    /** Вызывается каждый тик — поддерживает гамму на максимуме
     *  если что-то другое (F3 меню, ресет настроек) её сбросило. */
    public void tick() {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null) return;
        if (mc.options.getGamma().getValue() != FULL_BRIGHT_GAMMA) {
            mc.options.getGamma().setValue(FULL_BRIGHT_GAMMA);
        }
    }

    @Override public List<ClickGUI.Setting> getSettings() { return new ArrayList<>(); }
}

