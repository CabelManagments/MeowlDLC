package com.yourcheat.modules;

import com.yourcheat.gui.ClickGUI;

import java.util.ArrayList;
import java.util.List;

/**
 * NoFluid — убирает синий/оранжевый оверлей при нахождении в воде/лаве.
 * Реализуется через mixin на InGameHud.renderOverlay, который мы
 * подключаем условно через статический флаг здесь.
 */
public class NoFluidModule implements IModule {

    private boolean enabled = false;
    public static boolean ACTIVE = false; // читается из mixin

    @Override public String getName()           { return "NoFluid"; }
    @Override public boolean isEnabled()        { return enabled; }
    @Override public void setEnabled(boolean v) { enabled = v; ACTIVE = v; }
    @Override public List<ClickGUI.Setting> getSettings() { return new ArrayList<>(); }
}

