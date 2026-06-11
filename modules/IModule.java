package com.yourcheat.modules;

import com.yourcheat.gui.ClickGUI;
import java.util.List;

public interface IModule {
    boolean isEnabled();
    void setEnabled(boolean enabled);
    default void toggle() { setEnabled(!isEnabled()); }
    String getName();
    List<ClickGUI.Setting> getSettings();
}

