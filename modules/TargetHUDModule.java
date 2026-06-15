package com.yourcheat.modules;

import com.yourcheat.gui.ClickGUI;
import com.yourcheat.gui.RenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AirBlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TargetHUDModule implements IModule {

    private boolean enabled = false;
    public float x = 10, y = 45;

    // Анимации
    private float animScale   = 0f;
    private float animHealth  = 0f;
    private LivingEntity lastTarget = null;

    private static final int W = 120, H = 35;

    @Override public String getName()           { return "TargetHUD"; }
    @Override public boolean isEnabled()        { return enabled; }
    @Override public void setEnabled(boolean v) { enabled = v; }

    @Override
    public List<ClickGUI.Setting> getSettings() {
        return new ArrayList<>();
    }

    public void register() {
        HudRenderCallback.EVENT.register(this::render);
    }

    private void render(DrawContext ctx, RenderTickCounter counter) {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        // Ищем цель
        LivingEntity target = null;
        if (mc.targetedEntity instanceof LivingEntity le && le.isAlive()) {
            target = le;
        }

        // Анимация появления/исчезновения
        float targetScale = (target != null) ? 1f : 0f;
        animScale += (targetScale - animScale) * 0.15f;
        if (animScale < 0.01f && target == null) { lastTarget = null; return; }
        if (target != null) lastTarget = target;
        if (lastTarget == null) return;

        // Плавный HP
        float rawHealth = MathHelper.clamp(lastTarget.getHealth() / lastTarget.getMaxHealth(), 0f, 1f);
        animHealth += (rawHealth - animHealth) * 0.1f;

        // Применяем анимацию масштаба
        float pivotX = x + W / 2f;
        float pivotY = y + H / 2f;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(pivotX, pivotY, 0);
        ctx.getMatrices().scale(animScale, animScale, 1);
        ctx.getMatrices().translate(-pivotX, -pivotY, 0);

        int alpha = (int)(animScale * 255);
        int accentColor = com.yourcheat.gui.ClickGUI.ACCENT_COLOR;

        // Фон
        RenderUtil.drawRoundedRect(ctx, x, y, W, H, 3f,
                new Color(18, 14, 20, (int)(alpha * 0.9f)).getRGB());
        // Акцент-полоска
        RenderUtil.drawRoundedRect(ctx, x, y, W, 2, 1f,
                RenderUtil.withAlpha(accentColor, alpha));

        // Голова (аватар заглушка — цветной квадрат с первой буквой)
        String name = lastTarget.getName().getString();
        if (name.length() > 12) name = name.substring(0, 12) + "…";

        RenderUtil.drawRoundedRect(ctx, x + 4, y + 4, 27, 27, 3f,
                RenderUtil.withAlpha(accentColor, (int)(alpha * 0.4f)));
        // Первая буква имени
        String letter = name.substring(0, 1).toUpperCase();
        var font = mc.textRenderer;
        int lw = font.getWidth(letter);
        ctx.drawText(font, letter,
                (int)(x + 4 + (27 - lw) / 2f),
                (int)(y + 4 + (27 - 9) / 2f),
                RenderUtil.withAlpha(0xFFFFFFFF, alpha), true);

        // Имя
        ctx.drawText(font, name, (int)(x + 35), (int)(y + 6),
                RenderUtil.withAlpha(0xFFEEEEEE, alpha), false);

        // HP текст
        String hpText = String.format("%.0f HP", lastTarget.getHealth());
        int hpW = font.getWidth(hpText);
        ctx.drawText(font, hpText, (int)(x + W - hpW - 4), (int)(y + 6),
                RenderUtil.withAlpha(0xFFCCCCCC, alpha), false);

        // HP бар
        float barX = x + 34, barY = y + 26, barW = 82, barH = 5;
        RenderUtil.drawRoundedRect(ctx, barX, barY, barW, barH, 2f,
                new Color(44, 41, 42, alpha).getRGB());
        if (animHealth > 0) {
            // Цвет HP: зелёный → красный
            float hue = animHealth * 0.33f; // 0=красный, 0.33=зелёный
            int hpColor = Color.getHSBColor(hue, 0.8f, 0.9f).getRGB();
            RenderUtil.drawRoundedRect(ctx, barX, barY, barW * animHealth, barH, 2f,
                    RenderUtil.withAlpha(hpColor, alpha));
        }

        // Предметы в руках
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(lastTarget.getMainHandStack());
        stacks.add(lastTarget.getOffHandStack());
        stacks.removeIf(s -> s.isEmpty() || s.getItem() instanceof AirBlockItem);

        float itemX = x + 35;
        for (ItemStack stack : stacks) {
            ctx.getMatrices().push();
            ctx.getMatrices().translate(itemX, y + 14, 0);
            ctx.getMatrices().scale(0.6f, 0.6f, 1f);
            ctx.drawItem(stack, 0, 0, 0);
            ctx.getMatrices().pop();
            itemX += 16;
        }

        ctx.getMatrices().pop();
    }
}

