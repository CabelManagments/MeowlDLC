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

    // Рисуем фон ПЕРЕД всем остальным
    @Inject(method = "render", at = @At("HEAD"))
    private void injectBg(DrawContext ctx, int mx, int my, float delta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        try {
            ctx.drawTexture(
                net.minecraft.client.render.RenderLayer::getGuiTextured,
                BG, 0, 0, 0, 0, w, h, w, h
            );
        } catch (Exception ignored) {
            ctx.fill(0, 0, w, h, 0xFF050508);
        }
        ctx.fill(0, 0, w, h, new Color(0,0,0,160).getRGB());
    }

    // Прячем ванильный логотип "Minecraft" — перехватываем drawBackground
    @Inject(
        method = "renderBackground",
        at = @At("TAIL"),
        cancellable = false
    )
    private void cancelVanillaBg(DrawContext ctx, int mx, int my, float delta, CallbackInfo ci) {
        // После drawBackground снова рисуем наш фон — он перекроет панораму
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        try {
            ctx.drawTexture(
                net.minecraft.client.render.RenderLayer::getGuiTextured,
                BG, 0, 0, 0, 0, w, h, w, h
            );
        } catch (Exception ignored) {
            ctx.fill(0, 0, w, h, 0xFF050508);
        }
        ctx.fill(0, 0, w, h, new Color(0,0,0,160).getRGB());
    }

    // Рисуем наш заголовок ПОСЛЕ кнопок
    @Inject(method = "render", at = @At("TAIL"))
    private void injectTitle(DrawContext ctx, int mx, int my, float delta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = mc.getWindow().getScaledWidth();
        var font = mc.textRenderer;
        String title = "MeowlDLC";

        float hue = 0.88f + (float)(Math.sin(System.currentTimeMillis() / 2000.0) * 0.04f);
        int color = Color.getHSBColor(hue, 0.75f, 0.95f).getRGB();

        // x3 размер
        int tw = font.getWidth(title) * 3;
        ctx.getMatrices().push();
        ctx.getMatrices().scale(3, 3, 1);
        // Тень
        ctx.drawText(font, title, (w - tw) / 6 + 1, 12, new Color(0,0,0,200).getRGB(), false);
        // Основной текст
        ctx.drawText(font, title, (w - tw) / 6, 11, color, false);
        ctx.getMatrices().pop();

        // "v1.0" снизу
        String ver = "v1.0 BETA";
        ctx.drawText(font, ver,
                w - font.getWidth(ver) - 4,
                mc.getWindow().getScaledHeight() - 12,
                new Color(120,120,130,200).getRGB(), false);
    }
}
