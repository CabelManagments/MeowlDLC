package com.yourcheat.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    private static final Identifier BG = Identifier.of("yourcheat", "textures/menu_bg.png");

    /**
     * Инджектим в render() — рисуем наш фон поверх дефолтного,
     * до того как кнопки отрисуются. Кнопки остаются на месте.
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void renderCustomBg(DrawContext ctx, int mx, int my, float delta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();

        // Рисуем наш чёрный фон
        try {
            ctx.drawTexture(
                net.minecraft.client.render.RenderLayer::getGuiTextured,
                BG, 0, 0, 0, 0, w, h, w, h
            );
        } catch (Exception ignored) {
            // Если текстура не загрузилась — чёрный фон
            ctx.fill(0, 0, w, h, 0xFF000000);
        }

        // Тёмный оверлей для контраста
        ctx.fill(0, 0, w, h, new Color(0, 0, 0, 140).getRGB());
    }

    /**
     * После отрисовки кнопок рисуем наш заголовок поверх ванильного "Minecraft".
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void renderCustomTitle(DrawContext ctx, int mx, int my, float delta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = mc.getWindow().getScaledWidth();

        // Заголовок MeowlDLC с Nexa шрифтом через FontRenderer
        String title = "MeowlDLC";
        long t = System.currentTimeMillis();
        float hue = 0.88f + (float)(Math.sin(t / 2000.0) * 0.04f);
        int color = Color.getHSBColor(hue, 0.7f, 0.95f).getRGB();

        FontRenderer font = FontRenderer.INSTANCE;
        if (font.isReady()) {
            int titleW = font.getWidth(title, 3f);
            font.drawString(ctx, title, (w - titleW) / 2f, 30, 3f, color);
        } else {
            // Fallback — ванильный шрифт x3
            var vfont = mc.textRenderer;
            int tw = vfont.getWidth(title) * 3;
            ctx.getMatrices().push();
            ctx.getMatrices().scale(3, 3, 1);
            ctx.drawText(vfont, title, (w - tw) / 6, 12, color, true);
            ctx.getMatrices().pop();
        }

        // Версия
        String ver = "v1.0 BETA";
        ctx.drawText(mc.textRenderer, ver,
                w - mc.textRenderer.getWidth(ver) - 4,
                mc.getWindow().getScaledHeight() - 12,
                new Color(140, 140, 140, 200).getRGB(), false);
    }
}

