package com.yourcheat.modules;

import com.yourcheat.gui.ClickGUI;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

/**
 * TimeChanger — меняет время суток на клиенте.
 * Работает через подмену ClientWorld.getTimeOfDay() в tick.
 * Сервер НЕ затрагивается — чисто визуально.
 */
public class TimeChangerModule implements IModule {

    private boolean enabled = false;

    // Пресеты времени
    public static final int TIME_DAY     = 6000;  // полдень
    public static final int TIME_SUNSET  = 12000; // закат
    public static final int TIME_NIGHT   = 18000; // ночь
    public static final int TIME_SUNRISE = 23000; // рассвет

    public float time = 6000f; // 0..24000

    @Override public String getName()           { return "TimeChanger"; }
    @Override public boolean isEnabled()        { return enabled; }
    @Override public void setEnabled(boolean v) { enabled = v; }

    @Override
    public List<ClickGUI.Setting> getSettings() {
        List<ClickGUI.Setting> list = new ArrayList<>();
        list.add(new ClickGUI.SliderSetting("Time (0=midnight, 6000=day)", time, 0f, 24000f, v -> time = v));
        // Пресеты через bool-кнопки
        list.add(new ClickGUI.BoolSetting("Day (6000)",    false, v -> { if(v) time=6000f;  }));
        list.add(new ClickGUI.BoolSetting("Sunset (12000)",false, v -> { if(v) time=12000f; }));
        list.add(new ClickGUI.BoolSetting("Night (18000)", false, v -> { if(v) time=18000f; }));
        list.add(new ClickGUI.BoolSetting("Sunrise (23000)",false,v -> { if(v) time=23000f; }));
        return list;
    }

    public void tick() {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        // Подменяем время в ClientWorld через reflection
        try {
            // В Fabric 1.21.4: net.minecraft.client.world.ClientWorld
            // поле timeOfDay (mapped: field_3671 / timeOfDay)
            var worldClass = mc.world.getClass().getSuperclass().getSuperclass(); // World
            java.lang.reflect.Field f = null;
            for (java.lang.reflect.Field field : worldClass.getDeclaredFields()) {
                if (field.getType() == long.class) {
                    f = field;
                    // Ищем поле которое содержит time (~6000..24000)
                    field.setAccessible(true);
                    long val = (long) field.get(mc.world);
                    if (val >= 0 && val < 30000) break; // нашли подходящее
                }
            }
            if (f != null) {
                f.setAccessible(true);
                f.set(mc.world, (long) time);
            }
        } catch (Exception e) {
            // Если reflection не сработал — используем Properties API
            // В 1.21.4 можно попробовать через WorldProperties
        }
    }
}

