package com.yourcheat.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Главное меню в минималистичном стиле: крупный заголовок, время,
 * full-width тёмные кнопки, мягкий тёмно-фиолетовый градиентный фон.
 */
public class MainMenu extends Screen {

    private static final Identifier BG = Identifier.of("yourcheat", "textures/menu_bg.png");
    private static final String TITLE = "MeowlDLC";

    private MenuButton singleplayerBtn, multiplayerBtn, altManagerBtn;
    private CombinedButton optionsQuitBtn;
    private float fadeIn = 0f;

    public MainMenu() {
        super(Text.literal("MeowlDLC"));
    }

    @Override
    protected void init() {
        fadeIn = 0f;
        int bw = 400, bh = 46, gap = 14;
        int cx = width / 2 - bw / 2;
        int startY = height / 2 - 40;

        singleplayerBtn = new MenuButton("Singleplayer", cx, startY, bw, bh);
        multiplayerBtn  = new MenuButton("Multiplayer",  cx, startY + (bh+gap), bw, bh);
        altManagerBtn   = new MenuButton("AltManager",   cx, startY + (bh+gap)*2, bw, bh);
        optionsQuitBtn  = new CombinedButton(cx, startY + (bh+gap)*3, bw, bh, "Options", "Quit");
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        if (fadeIn < 1f) fadeIn = Math.min(1f, fadeIn + 0.03f);

        // Фон: пробуем нашу текстуру, иначе тёмно-фиолетовый градиент
        try {
            ctx.drawTexture(
                net.minecraft.client.render.RenderLayer::getGuiTextured,
                BG, 0, 0, 0, 0, width, height, width, height
            );
            ctx.fill(0, 0, width, height, new Color(0,0,0,120).getRGB());
        } catch (Exception e) {
            drawFallbackGradient(ctx);
        }

        var font = client.textRenderer;

        // Заголовок крупным шрифтом через FontRenderer (Nexa), фолбэк на ванильный x3
        float titleY = height * 0.28f;
        long t = System.currentTimeMillis();
        float hue = 0.0f + (float)(Math.sin(t / 3000.0) * 0.02f); // лёгкое розовое мерцание
        int titleColor = Color.getHSBColor(0.97f, 0.55f, 1f).getRGB();

        if (FontRenderer.INSTANCE.isReady()) {
            float scale = 3.2f;
            int tw = FontRenderer.INSTANCE.getWidth(TITLE, scale);
            FontRenderer.INSTANCE.drawString(ctx, TITLE, (width - tw) / 2f, titleY, scale,
                    RenderUtil.withAlpha(titleColor, (int)(fadeIn*255)));
        } else {
            int tw = font.getWidth(TITLE) * 3;
            ctx.getMatrices().push();
            ctx.getMatrices().scale(3,3,1);
            ctx.drawText(font, TITLE, (int)((width - tw)/2f/3), (int)(titleY/3),
                    RenderUtil.withAlpha(titleColor,(int)(fadeIn*255)), false);
            ctx.getMatrices().pop();
        }

        // Время
        String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        int tsw = font.getWidth(timeStr);
        ctx.drawText(font, timeStr, (width - tsw)/2, (int)(titleY + 46),
                RenderUtil.withAlpha(new Color(255,140,150,255).getRGB(), (int)(fadeIn*220)), false);

        // Кнопки
        singleplayerBtn.render(ctx, mx, my, fadeIn);
        multiplayerBtn.render(ctx, mx, my, fadeIn);
        altManagerBtn.render(ctx, mx, my, fadeIn);
        optionsQuitBtn.render(ctx, mx, my, fadeIn);

        // Версия
        String ver = "v1.0 BETA";
        int vw = font.getWidth(ver);
        ctx.drawText(font, ver, width - vw - 10, height - 14,
                RenderUtil.withAlpha(new Color(140,140,150,255).getRGB(), (int)(fadeIn*200)), false);

        super.render(ctx, mx, my, delta);
    }

    private void drawFallbackGradient(DrawContext ctx) {
        int top    = new Color(18, 14, 20, 255).getRGB();
        int bottom = new Color(35, 8, 16, 255).getRGB();
        ctx.fillGradient(0, 0, width, height, top, bottom);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (singleplayerBtn.isHovered(mx, my)) { client.setScreen(new SelectWorldScreen(this)); return true; }
        if (multiplayerBtn.isHovered(mx, my))  { client.setScreen(new MultiplayerScreen(this)); return true; }
        if (altManagerBtn.isHovered(mx, my))   { client.setScreen(new AltManager(this)); return true; }
        if (optionsQuitBtn.isOptionHovered(mx, my)) { client.setScreen(new OptionsScreen(this, client.options)); return true; }
        if (optionsQuitBtn.isQuitHovered(mx, my))   { client.scheduleStop(); return true; }
        return super.mouseClicked(mx, my, btn);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }

    // ── Full-width кнопка ────────────────────────────────────────
    private class MenuButton {
        final String name; int x, y, width, height;
        float hoverAnim = 0;

        MenuButton(String name, int x, int y, int w, int h) {
            this.name = name; this.x = x; this.y = y; this.width = w; this.height = h;
        }

        void render(DrawContext ctx, int mx, int my, float fa) {
            boolean hov = isHovered(mx, my);
            hoverAnim += ((hov ? 1f : 0f) - hoverAnim) * 0.15f;

            int bg = lerpColor(
                    new Color(22, 20, 26, (int)(220*fa)).getRGB(),
                    new Color(38, 30, 42, (int)(230*fa)).getRGB(),
                    hoverAnim);

            RenderUtil.drawRoundedRect(ctx, x, y, width, height, 8f, bg);
            if (hoverAnim > 0.01f) {
                RenderUtil.drawRoundedRect(ctx, x, y, width, height, 8f,
                        new Color(255,140,150,(int)(hoverAnim*40*fa)).getRGB());
            }

            var font = client.textRenderer;
            int tw = font.getWidth(name);
            ctx.drawText(font, name, x + width/2 - tw/2, y + height/2 - 4,
                    RenderUtil.withAlpha(0xFFEDEDED, (int)(fa*255)), false);
        }

        boolean isHovered(double mx, double my) {
            return mx >= x && mx <= x+width && my >= y && my <= y+height;
        }
    }

    // ── Двойная кнопка Options | Quit ──────────────────────────────
    private class CombinedButton {
        int x, y, width, height; final String leftName, rightName;
        float lHover = 0, rHover = 0;

        CombinedButton(int x, int y, int w, int h, String l, String r) {
            this.x=x; this.y=y; this.width=w; this.height=h; leftName=l; rightName=r;
        }

        void render(DrawContext ctx, int mx, int my, float fa) {
            int gap = 12;
            int bw = (width - gap) / 2;
            int lx = x, rx = x + bw + gap;

            boolean lH = isOptionHovered(mx,my), rH = isQuitHovered(mx,my);
            lHover += ((lH?1f:0f)-lHover)*0.15f;
            rHover += ((rH?1f:0f)-rHover)*0.15f;

            var font = client.textRenderer;

            int lBg = lerpColor(new Color(22,20,26,(int)(220*fa)).getRGB(),
                                 new Color(38,30,42,(int)(230*fa)).getRGB(), lHover);
            RenderUtil.drawRoundedRect(ctx, lx, y, bw, height, 8f, lBg);
            int ltw = font.getWidth(leftName);
            ctx.drawText(font, leftName, lx+bw/2-ltw/2, y+height/2-4,
                    RenderUtil.withAlpha(0xFFEDEDED,(int)(fa*255)), false);

            int rBg = lerpColor(new Color(22,20,26,(int)(220*fa)).getRGB(),
                                 new Color(38,30,42,(int)(230*fa)).getRGB(), rHover);
            RenderUtil.drawRoundedRect(ctx, rx, y, bw, height, 8f, rBg);
            int rtw = font.getWidth(rightName);
            ctx.drawText(font, rightName, rx+bw/2-rtw/2, y+height/2-4,
                    RenderUtil.withAlpha(0xFFEDEDED,(int)(fa*255)), false);
        }

        boolean isOptionHovered(double mx, double my) {
            int bw = (width-12)/2;
            return mx>=x && mx<=x+bw && my>=y && my<=y+height;
        }
        boolean isQuitHovered(double mx, double my) {
            int bw = (width-12)/2; int rx = x+bw+12;
            return mx>=rx && mx<=rx+bw && my>=y && my<=y+height;
        }
    }

    private static int lerpColor(int c1, int c2, float t) {
        int a1=(c1>>24)&0xFF,r1=(c1>>16)&0xFF,g1=(c1>>8)&0xFF,b1=c1&0xFF;
        int a2=(c2>>24)&0xFF,r2=(c2>>16)&0xFF,g2=(c2>>8)&0xFF,b2=c2&0xFF;
        return ((int)(a1+(a2-a1)*t)<<24)|((int)(r1+(r2-r1)*t)<<16)|((int)(g1+(g2-g1)*t)<<8)|(int)(b1+(b2-b1)*t);
    }
}
