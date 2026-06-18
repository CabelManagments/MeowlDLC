package com.yourcheat.modules;

import com.yourcheat.gui.ClickGUI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;

public class AutoEatModule implements IModule {

    private boolean enabled = false;
    public float hungerThreshold = 15f; // едим если голод <= этого значения

    private boolean eating = false;
    private int savedSlot = -1;
    private int eatTicks  = 0;

    @Override public String getName()           { return "AutoEat"; }
    @Override public boolean isEnabled()        { return enabled; }
    @Override public void setEnabled(boolean v) {
        enabled = v;
        if (!v) stopEating();
    }

    @Override
    public List<ClickGUI.Setting> getSettings() {
        List<ClickGUI.Setting> list = new ArrayList<>();
        list.add(new ClickGUI.SliderSetting("Hunger Threshold", hungerThreshold, 1f, 20f, v -> hungerThreshold = v));
        return list;
    }

    public void tick() {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;

        int hunger = mc.player.getHungerManager().getFoodLevel();

        if (eating) {
            // Продолжаем держать предмет — едим
            eatTicks++;
            // Через ~32 тика (стандартное время еды) или если голод восстановился — стоп
            if (eatTicks > 35 || hunger >= 20) {
                stopEating();
            }
            return;
        }

        if (hunger > hungerThreshold) return;

        // Ищем еду в хотбаре
        int foodSlot = findFoodSlot(mc);
        if (foodSlot == -1) return;

        savedSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(foodSlot);
        mc.options.useKey.setPressed(true);
        eating  = true;
        eatTicks = 0;
    }

    private void stopEating() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null) mc.options.useKey.setPressed(false);
        if (savedSlot != -1 && mc.player != null) {
            mc.player.getInventory().setSelectedSlot(savedSlot);
            savedSlot = -1;
        }
        eating = false;
        eatTicks = 0;
    }

    private int findFoodSlot(MinecraftClient mc) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isFood(stack)) return i;
        }
        return -1;
    }

    private boolean isFood(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // Не едим вредную еду
        if (stack.isOf(Items.ROTTEN_FLESH) || stack.isOf(Items.SPIDER_EYE)
            || stack.isOf(Items.POISONOUS_POTATO) || stack.isOf(Items.PUFFERFISH)
            || stack.isOf(Items.CHORUS_FRUIT) || stack.isOf(Items.SUSPICIOUS_STEW)) {
            return false;
        }
        return stack.contains(DataComponentTypes.FOOD);
    }
}

