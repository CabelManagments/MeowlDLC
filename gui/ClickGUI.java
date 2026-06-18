package com.yourcheat.gui;

import com.yourcheat.CheatMod;
import com.yourcheat.modules.IModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ClickGUI в стиле двухколоночного списка: табы сверху (Visuals / HUD / Utilities),
 * full-width строки модулей с toggle-свитчем справа, активный ряд подсвечен,
 * search bar и color picker иконка сверху справа.
 */
public class ClickGUI extends Screen {

    public static int ACCENT_COLOR = new Color(124, 92, 255, 255).getRGB(); // фиолетовый
    public static int BG_PANEL     = new Color(10, 9, 14, 235).getRGB();
    public static int TEXT_PRIMARY = new Color(235, 235, 240, 255).getRGB();
    public static int TEXT_DIM     = new Color(120, 118, 130, 255).getRGB();

    private final float W = 900f, H = 560f;
    private float px, py;
    private final AnimationUtil alphaAnim = new AnimationUtil();

    public enum Category { VISUAL, HUD, UTILITIES, COMBAT, PLAYER, MISC }

    private Category activeTab = Category.VISUAL;
    private final Map<Category, List<ModuleRow>> columns = new EnumMap<>(Category.class);
    private float targetScroll = 0f, currentScroll = 0f;
    private final SearchBar search = new SearchBar();
    private boolean showColorPicker = false;

    public ClickGUI() {
        super(Text.literal("ClickGUI"));
        rebuildColumns();
        alphaAnim.set(0.0);
    }

    private void rebuildColumns() {
        columns.clear();
        Map<Category, List<IModule>> bycat = new EnumMap<>(Category.class);
        for (Category c : Category.values()) bycat.put(c, new ArrayList<>());

        bycat.get(Category.VISUAL).add(CheatMod.jumpCircle);
        bycat.get(Category.VISUAL).add(CheatMod.targetESP);
        bycat.get(Category.VISUAL).add(CheatMod.hitParticles);
        bycat.get(Category.VISUAL).add(CheatMod.chinaHat);
        bycat.get(Category.VISUAL).add(CheatMod.cape);
        bycat.get(Category.VISUAL).add(CheatMod.wings);
        bycat.get(Category.VISUAL).add(CheatMod.nimb);
        bycat.get(Category.VISUAL).add(CheatMod.fullBright);
        bycat.get(Category.VISUAL).add(CheatMod.crosshair);
        bycat.get(Category.VISUAL).add(CheatMod.customModels);

        bycat.get(Category.HUD).add(CheatMod.targetHUD);
        bycat.get(Category.HUD).add(CheatMod.watermark);

        bycat.get(Category.UTILITIES).add(CheatMod.zoom);
        bycat.get(Category.UTILITIES).add(CheatMod.autoEat);
        bycat.get(Category.UTILITIES).add(CheatMod.noFluid);
        bycat.get(Category.UTILITIES).add(CheatMod.timeChanger);

        bycat.get(Category.COMBAT).add(CheatMod.killAura);

        for (Category c : Category.values()) {
            columns.put(c, bycat.get(c).stream().map(ModuleRow::new).collect(Collectors.toList()));
        }
    }

    @Override
    protected void init() {
        super.init();
        px = width / 2f - W / 2f; py = height / 2f - H / 2f;
        search.setX(px + W - 230f); search.setY(py + 6f);
        alphaAnim.run(1.0, 0.2, AnimationUtil.Easing.CUBIC_OUT);
        targetScroll = 0f; currentScroll = 0f;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float pt) {
        alphaAnim.update();
        float alpha = alphaAnim.get();
        if (alpha <= 0.01f) return;

        currentScroll += (targetScroll - currentScroll) * 0.15f * (1f - pt);

        px = width / 2f - W / 2f; py = height / 2f - H / 2f;
        int pA = (int)(alpha * 235);

        // Главная панель
        RenderUtil.drawRoundedRect(ctx, px, py, W, H, 10f, RenderUtil.withAlpha(BG_PANEL, pA));

        renderHeader(ctx, mx, my, alpha);
        renderTabs(ctx, mx, my, alpha);
        renderModuleGrid(ctx, mx, my, pt, alpha);

        if (showColorPicker) drawColorPicker(ctx, alpha);

        super.render(ctx, mx, my, pt);
    }

    // ── Заголовок: табы + search + color picker иконка ─────────────
    private void renderHeader(DrawContext ctx, int mx, int my, float alpha) {
        var font = MinecraftClient.getInstance().textRenderer;
        int hA = (int)(alpha * 255);

        // Search bar
        search.draw(ctx, mx, my, 0, alpha);

        // Иконка настроек цвета (солнце/шестерёнка)
        float btnX = px + W - 36, btnY = py + 8, btnSize = 22;
        boolean hovBtn = mx >= btnX && mx <= btnX + btnSize && my >= btnY && my <= btnY + btnSize;
        RenderUtil.drawRoundedRect(ctx, btnX, btnY, btnSize, btnSize, 6f,
                RenderUtil.withAlpha(hovBtn ? ACCENT_COLOR : 0xFF2A2730, hA));
        ctx.drawText(font, "\u2699", (int)(btnX + 7), (int)(btnY + 6), 0xFFFFFFFF, false);
    }

    private void renderTabs(DrawContext ctx, int mx, int my, float alpha) {
        var font = MinecraftClient.getInstance().textRenderer;
        int hA = (int)(alpha * 255);
        float tabY = py + 40f;
        float tabX = px + 28f;

        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            Category c = cats[i];
            String name = capitalize(c.name());
            int tw = font.getWidth(name);
            boolean active = c == activeTab;
            boolean hov = mx >= tabX && mx <= tabX + tw && my >= tabY && my <= tabY + 12;

            int color = active ? RenderUtil.withAlpha(TEXT_PRIMARY, hA)
                                : RenderUtil.withAlpha(hov ? 0xFFB0AEBB : TEXT_DIM, hA);
            ctx.drawText(font, name, (int)tabX, (int)tabY, color, false);

            if (active) {
                RenderUtil.drawRoundedRect(ctx, tabX, tabY + 13, tw, 2, 1f,
                        RenderUtil.withAlpha(ACCENT_COLOR, hA));
            }

            tabX += tw + 12;
            if (i < cats.length - 1) {
                ctx.drawText(font, "/", (int)tabX, (int)tabY, RenderUtil.withAlpha(TEXT_DIM, hA), false);
                tabX += 14;
            }
        }

        // Разделительная линия
        RenderUtil.drawRoundedRect(ctx, px + 20, tabY + 24, W - 40, 1, 0,
                new Color(255,255,255,(int)(alpha*20)).getRGB());
    }

    // ── Сетка модулей: 2 колонки, full-width строки ──────────────────
    private void renderModuleGrid(DrawContext ctx, int mx, int my, float pt, float alpha) {
        var font = MinecraftClient.getInstance().textRenderer;
        List<ModuleRow> rows = columns.getOrDefault(activeTab, Collections.emptyList());

        String q = search.getText().toLowerCase();
        List<ModuleRow> filtered = q.isEmpty() ? rows :
                rows.stream().filter(r -> r.getModule().getName().toLowerCase().contains(q))
                    .collect(Collectors.toList());

        float gridY = py + 78f;
        float gridH = H - 78f - 16f;
        float colGap = 16f;
        float colW   = (W - 40f - colGap) / 2f;
        float rowH   = 50f;

        ctx.enableScissor((int)(px+18), (int)(gridY-4), (int)(px+W-18), (int)(py+H-8));

        for (int i = 0; i < filtered.size(); i++) {
            ModuleRow row = filtered.get(i);
            int col = i % 2;
            int line = i / 2;

            float rx = px + 20 + col * (colW + colGap);
            float ry = gridY + line * (rowH + 10) + currentScroll;

            row.setX(rx); row.setY(ry); row.setWidth(colW);
            row.drawFullWidthRow(ctx, mx, my, pt, alpha, rowH);
        }

        ctx.disableScissor();

        // Скролл-лимит
        int totalLines = (filtered.size() + 1) / 2;
        float totalH = totalLines * (rowH + 10);
        float maxScroll = Math.max(0, totalH - gridH);
        targetScroll = MathHelper_clamp(targetScroll, -maxScroll, 0);
    }

    private static float MathHelper_clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private void drawColorPicker(DrawContext ctx, float alpha) {
        float cpX = px + W - 170, cpY = py + 34, cpW = 150, cpH = 90;
        RenderUtil.drawRoundedRect(ctx, cpX, cpY, cpW, cpH, 8f, new Color(16,14,20,(int)(alpha*245)).getRGB());
        var font = MinecraftClient.getInstance().textRenderer;
        ctx.drawText(font, "Accent Color", (int)(cpX+8), (int)(cpY+6), TEXT_DIM, false);

        float[] hsb = Color.RGBtoHSB((ACCENT_COLOR>>16)&0xFF,(ACCENT_COLOR>>8)&0xFF,ACCENT_COLOR&0xFF,null);
        int[] hues = {0xFFFF0000,0xFFFFAA00,0xFFFFFF00,0xFF00FF00,0xFF00FFFF,0xFF0000FF,0xFFAA00FF,0xFFFF0000};
        float hw = (cpW-16)/(hues.length-1);
        for (int i = 0; i < hues.length-1; i++)
            ctx.fill((int)(cpX+8+i*hw),(int)(cpY+22),(int)(cpX+8+(i+1)*hw),(int)(cpY+32), hues[i]);

        float sw = cpW - 16;
        for (int i = 0; i < (int)sw; i++) {
            Color c = Color.getHSBColor(hsb[0], hsb[1], i/sw);
            ctx.fill((int)(cpX+8+i),(int)(cpY+44),(int)(cpX+9+i),(int)(cpY+54), c.getRGB()|0xFF000000);
        }
        RenderUtil.drawRoundedRect(ctx, cpX+cpW-26, cpY+60, 18, 14, 3f, ACCENT_COLOR|0xFF000000);
    }

    private static String capitalize(String s) {
        return s.charAt(0) + s.substring(1).toLowerCase();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (alphaAnim.get() < 0.9f) return false;

        // Таб-клики
        var font = MinecraftClient.getInstance().textRenderer;
        float tabY = py + 40f, tabX = px + 28f;
        for (Category c : Category.values()) {
            String name = capitalize(c.name());
            int tw = font.getWidth(name);
            if (mx >= tabX && mx <= tabX+tw && my >= tabY && my <= tabY+12) {
                activeTab = c; targetScroll = 0; currentScroll = 0; return true;
            }
            tabX += tw + 26;
        }

        // Color picker кнопка
        float btnX = px + W - 36, btnY = py + 8, btnSize = 22;
        if (mx >= btnX && mx <= btnX+btnSize && my >= btnY && my <= btnY+btnSize) {
            showColorPicker = !showColorPicker; return true;
        }

        if (showColorPicker) {
            float cpX = px+W-170, cpY = py+34, cpW = 150;
            float sx1 = cpX+8, sx2 = cpX+cpW-8;
            float[] hsb = Color.RGBtoHSB((ACCENT_COLOR>>16)&0xFF,(ACCENT_COLOR>>8)&0xFF,ACCENT_COLOR&0xFF,null);
            if (mx>=sx1&&mx<=sx2) {
                float t = MathHelper_clamp((float)((mx-sx1)/(sx2-sx1)), 0, 1);
                if (my>=cpY+22&&my<=cpY+32) { ACCENT_COLOR = Color.getHSBColor(t,hsb[1],hsb[2]).getRGB()|0xFF000000; return true; }
                if (my>=cpY+44&&my<=cpY+54) { ACCENT_COLOR = Color.getHSBColor(hsb[0],t,hsb[2]).getRGB()|0xFF000000; return true; }
            }
        }

        if (search.mouseClicked((float)mx,(float)my,btn)) return true;

        for (ModuleRow row : columns.getOrDefault(activeTab, Collections.emptyList()))
            if (row.mouseClicked((float)mx,(float)my,btn)) return true;

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        for (ModuleRow row : columns.getOrDefault(activeTab, Collections.emptyList()))
            row.mouseDragged(mx, my, btn);
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        for (ModuleRow row : columns.getOrDefault(activeTab, Collections.emptyList()))
            row.mouseReleased((float)mx,(float)my,btn);
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hd, double vd) {
        targetScroll += (float)(vd * 25);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key==GLFW.GLFW_KEY_ESCAPE||key==GLFW.GLFW_KEY_RIGHT_SHIFT) { startFadeOut(); return true; }
        if (search.keyPressed(key,scan,mods)) return true;
        for (ModuleRow row : columns.getOrDefault(activeTab, Collections.emptyList()))
            if (row.keyPressed(key,scan,mods)) return true;
        return super.keyPressed(key,scan,mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (search.charTyped(c,mods)) return true;
        return super.charTyped(c,mods);
    }

    @Override
    public void tick() {
        super.tick(); alphaAnim.update();
        if (alphaAnim.getToValue()==0.0 && !alphaAnim.isAlive()) client.setScreen(null);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float pt) {
        ctx.fill(0,0,width,height,new Color(0,0,0,(int)(alphaAnim.get()*150)).getRGB());
    }

    @Override public void close() { startFadeOut(); }
    public void startFadeOut() { alphaAnim.run(0.0,0.2,AnimationUtil.Easing.CUBIC_IN); }
    @Override public boolean shouldPause() { return false; }

    // ── Setting types ─────────────────────────────────────────────
    public abstract static class Setting {
        final String label;
        Setting(String label) { this.label = label; }
        abstract int getHeight();
        abstract void render(DrawContext ctx, int mx, int my, int px, int py);
        boolean mouseClicked(double mx, double my, int btn, int px, int py) { return false; }
        boolean mouseDragged(double mx, double my, int px, int py) { return false; }
        void mouseReleased() {}
    }

    public static class SliderSetting extends Setting {
        public float value, min, max; boolean dragging;
        final java.util.function.Consumer<Float> onChange;
        public SliderSetting(String l, float v, float mn, float mx, java.util.function.Consumer<Float> f) {
            super(l); value=v; min=mn; max=mx; onChange=f;
        }
        @Override public int getHeight() { return 22; }
        @Override public void render(DrawContext ctx, int mx2, int my2, int px2, int py2) {
            int sw = 140, sx = px2+8, sy = py2+4;
            ctx.drawText(MinecraftClient.getInstance().textRenderer,
                    label+": "+String.format("%.1f",value), sx, sy, TEXT_DIM, false);
            ctx.fill(sx, sy+10, sx+sw, sy+14, new Color(40,35,45,200).getRGB());
            int fw = (int)((value-min)/(max-min)*sw);
            if (fw>0) ctx.fill(sx, sy+10, sx+fw, sy+14, RenderUtil.withAlpha(ACCENT_COLOR,200));
            ctx.fill(sx+fw-3, sy+8, sx+fw+3, sy+16, 0xFFFFFFFF);
        }
        @Override public boolean mouseClicked(double mx2, double my2, int btn, int px2, int py2) {
            if (mx2>=px2+8&&mx2<=px2+148&&my2>=py2+12&&my2<=py2+18) { dragging=true; update(mx2,px2); return true; }
            return false;
        }
        @Override public boolean mouseDragged(double mx2, double my2, int px2, int py2) {
            if (!dragging) return false; update(mx2,px2); return true;
        }
        @Override public void mouseReleased() { dragging=false; }
        private void update(double mx2, int px2) {
            float t=Math.max(0,Math.min(1,(float)((mx2-px2-8)/140f)));
            value=min+t*(max-min); value=Math.round(value/0.05f)*0.05f; onChange.accept(value);
        }
    }

    public static class BoolSetting extends Setting {
        public boolean value; final java.util.function.Consumer<Boolean> onChange;
        public BoolSetting(String l, boolean v, java.util.function.Consumer<Boolean> f) { super(l); value=v; onChange=f; }
        @Override public int getHeight() { return 18; }
        @Override public void render(DrawContext ctx, int mx2, int my2, int px2, int py2) {
            ctx.drawText(MinecraftClient.getInstance().textRenderer, label, px2+8, py2+4, TEXT_DIM, false);
            int tx=px2+135, ty=py2+3;
            ctx.fill(tx,ty,tx+20,ty+12, value?RenderUtil.withAlpha(ACCENT_COLOR,200):new Color(50,45,55,200).getRGB());
            ctx.fill(value?tx+10:tx+1,ty+1,value?tx+19:tx+10,ty+11,0xFFFFFFFF);
        }
        @Override public boolean mouseClicked(double mx2, double my2, int btn, int px2, int py2) {
            if (mx2>=px2&&mx2<=px2+160&&my2>=py2&&my2<py2+18) { value=!value; onChange.accept(value); return true; }
            return false;
        }
    }

    public static class ColorSetting extends Setting {
        float hue; Color current; boolean expanded;
        final java.util.function.Consumer<Color> onChange;
        public ColorSetting(String l, Color init, java.util.function.Consumer<Color> f) {
            super(l); float[] hsb=Color.RGBtoHSB(init.getRed(),init.getGreen(),init.getBlue(),null);
            hue=hsb[0]; current=init; onChange=f;
        }
        @Override public int getHeight() { return expanded?34:18; }
        @Override public void render(DrawContext ctx, int mx2, int my2, int px2, int py2) {
            ctx.drawText(MinecraftClient.getInstance().textRenderer, label, px2+8, py2+4, TEXT_DIM, false);
            int pc=0xFF000000|(current.getRed()<<16)|(current.getGreen()<<8)|current.getBlue();
            RenderUtil.drawRoundedRect(ctx,px2+130,py2+2,18,12,3f,pc);
            if (expanded) {
                int[] hc={0xFFFF0000,0xFFFFFF00,0xFF00FF00,0xFF00FFFF,0xFF0000FF,0xFFFF00FF,0xFFFF0000};
                float segW=130f/6;
                for(int i=0;i<6;i++) ctx.fill((int)(px2+8+i*segW),(int)(py2+20),(int)(px2+8+(i+1)*segW),(int)(py2+30),hc[i]);
            }
        }
        @Override public boolean mouseClicked(double mx2, double my2, int btn, int px2, int py2) {
            if (mx2>=px2+130&&mx2<=px2+148&&my2>=py2+2&&my2<=py2+14) { expanded=!expanded; return true; }
            if (expanded&&mx2>=px2+8&&mx2<=px2+138&&my2>=py2+20&&my2<=py2+30) {
                hue=Math.max(0,Math.min(0.99f,(float)((mx2-px2-8)/130f)));
                current=Color.getHSBColor(hue,0.8f,0.9f); onChange.accept(current); return true;
            }
            return false;
        }
    }
}
