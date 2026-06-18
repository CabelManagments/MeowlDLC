package com.yourcheat.modules;

import com.yourcheat.gui.ClickGUI;

import java.util.ArrayList;
import java.util.List;

/**
 * CustomModels — переключатель геометрии игрока. Сейчас доступна модель
 * "Crazy Rabbit". Структура готова для добавления Freddy/Demons позже —
 * просто добавь имя в MODEL_NAMES и обработай его в PlayerEntityRendererMixin.
 */
public class CustomModelsModule implements IModule {

    private boolean enabled = false;
    public static final String[] MODEL_NAMES = { "Crazy Rabbit" }; // позже: , "Freddy Bear", "White Demon", "Red Demon"

    public int selectedModel = 0; // индекс в MODEL_NAMES
    public boolean applyToFriends = true;

    @Override public String getName()           { return "CustomModels"; }
    @Override public boolean isEnabled()        { return enabled; }
    @Override public void setEnabled(boolean v) { enabled = v; }

    @Override
    public List<ClickGUI.Setting> getSettings() {
        List<ClickGUI.Setting> list = new ArrayList<>();
        // Простой переключатель модели через BoolSetting на каждую модель (т.к. ModeSetting у нас нет)
        for (int i = 0; i < MODEL_NAMES.length; i++) {
            final int idx = i;
            list.add(new ClickGUI.BoolSetting(MODEL_NAMES[i], selectedModel == idx,
                    v -> { if (v) selectedModel = idx; }));
        }
        list.add(new ClickGUI.BoolSetting("Apply to friends", applyToFriends, v -> applyToFriends = v));
        return list;
    }

    public String getCurrentModelName() {
        return MODEL_NAMES[selectedModel];
    }
}

