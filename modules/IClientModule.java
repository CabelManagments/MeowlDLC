package com.yourcheat.modules;

import com.yourcheat.gui.ClickGUI;
import java.util.List;

/**
 * Интерфейс для всех модулей клиента.
 * Каждый модуль обязан вернуть список Setting для отображения в ClickGUI.
 */
public interface IClientModule {
    boolean isEnabled();
    void setEnabled(boolean enabled);
    default void toggle() { setEnabled(!isEnabled()); }
    List<ClickGUI.Setting> getSettings();
}

