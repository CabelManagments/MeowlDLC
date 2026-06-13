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

public class ClickGUI extends Screen {

    // ── Глобальные цвета (меняются через Color Picker в GUI) ───────
    public static int ACCENT_COLOR = new Color(100, 60, 180, 255).getRGB();
    public static int BG_PANEL     = new Color(20, 16, 22, 230).getRGB();
    public static int TEXT_PRIMARY = new Color(220, 220, 220, 255).getRGB();
    public static int TEXT_DIM     = new Color(180, 180, 180, 255).getRGB();

    private final float W = 820f, H = 295f;
    private float px, py;
    private final AnimationUtil alphaAnim = new AnimationUtil();
    private final float SCROLL_SPEED = 15f, SCROLL_LERP = 0.1f;

    public enum Category { COMBAT, MOVEMENT, VISUAL, PLAYER, MISC }

    private final Map<Category, List<ModuleRow>> columns = new EnumMap<>(Category.class);
    private final Map<Category, Float> targetScroll  = new EnumMap<>(Category.class);
    private final Map<Category, Float> currentScroll = new EnumMap<>(Category.class);
    private final SearchBar search = new SearchBar();
    private boolean showColorPicker = false;

    public ClickGUI() {
        super(Text.literal("ClickGUI"));
        rebuildColumns();
        alphaAnim.set(0.0);
    }

    private void rebuildColumns() {
        columns.clear(); targetScroll.clear(); currentScroll.clear();
        Map<Category, List<IModule>> bycat = new EnumMap<>(Category.class);
        for (Category c : Category.values()) bycat.put(c, new ArrayList<>());
        bycat.get(Category.VISUAL).add(CheatMod.jumpCircle);
        bycat.get(Category.VISUAL).add(CheatMod.targetESP);
        bycat.get(Category.VISUAL).add(CheatMod.hitParticles);
        for (Category c : Category.values()) {
            columns.put(c, bycat.get(c).stream().map(ModuleRow::new).collect(Collectors.toList()));
            targetScroll.put(c, 0f); currentScroll.put(c, 0f);
        }
    }

    @Override
    protected void init() {
        super.init();
        px = width / 2f - W / 2f; py = height / 2f - H / 2f;
        search.setX(px + W / 2f - 150f); search.setY(py + H + 8f);
        alphaAnim.run(1.0, 0.2, AnimationUtil.Easing.CUBIC_OUT);
        targetScroll.keySet().forEach(c -> { targetScroll.put(c,0f); currentScroll.put(c,0f); });
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float pt) {
        alphaAnim.update();
        float alpha = alphaAnim.get();
        if (alpha <= 0.01f) return;

        // плавный скролл
        columns.keySet().forEach(c -> {
            float t = targetScroll.getOrDefault(c, 0f);
            float cur = currentScroll.getOrDefault(c, 0f);
            cur = Math.abs(t - cur) < 0.01f ? t : cur + (t - cur) * SCROLL_LERP * (1f - pt);
            currentScroll.put(c, cur);
        });

        px = width / 2f - W / 2f; py = height / 2f - H / 2f;
        int pA = (int)(alpha*230), hA = (int)(alpha*255);
        int scrollC = new Color(255,255,255,(int)(alpha*120)).getRGB();

        Category[] cats = Category.values();
        float gutter = 5f, colW = (W - gutter*(cats.length-1) - 220) / cats.length;
        float startX = px + 20f, headerY = py - 10f;

        for (int i = 0; i < cats.length; i++) {
            Category c = cats[i];
            float colX = startX + i*(colW+gutter);
            float listY = headerY + 22f, listH = H - 70f;
            float contentStart = listY + 20f;
            float saX1 = colX+60f, saY1 = listY-6f, saX2 = saX1+colW, saY2 = saY1+listH+32f;
            float visH = saY2 - contentStart;

            // Панель
            RenderUtil.Blur.drawBlur(ctx, saX1, saY1, colW, listH+32f, 6f, 10, -1);
            RenderUtil.drawRoundedRect(ctx, saX1, saY1, colW, listH+32f, 6f, RenderUtil.withAlpha(BG_PANEL, pA));
            // Акцентная полоска
            RenderUtil.drawRoundedRect(ctx, saX1, saY1, colW, 3, 3f, RenderUtil.withAlpha(ACCENT_COLOR, hA));

            // Заголовок
            String name = c.name().charAt(0) + c.name().substring(1).toLowerCase();
            int tw = MinecraftClient.getInstance().textRenderer.getWidth(name);
            ctx.drawText(MinecraftClient.getInstance().textRenderer, name,
                    (int)(colX+115f-tw/2f), (int)(headerY+28), RenderUtil.withAlpha(TEXT_PRIMARY, hA), false);

            // Модули с фильтром поиска
            String q = search.getText().toLowerCase();
            List<ModuleRow> rows = columns.getOrDefault(c, Collections.emptyList());
            List<ModuleRow> filtered = q.isEmpty() ? rows :
                    rows.stream().filter(r -> r.getModule().getName().toLowerCase().contains(q)).collect(Collectors.toList());

            float totalH = filtered.stream().mapToFloat(r -> r.getHeight()+2).sum();
            float scrollOffset = currentScroll.getOrDefault(c, 0f);

            ctx.enableScissor((int)saX1, (int)(contentStart-4), (int)saX2, (int)saY2);
            float curY = contentStart + scrollOffset;
            for (ModuleRow row : filtered) {
                row.setX(colX+60f); row.setY(curY); row.setWidth(colW);
                row.draw(ctx, mx, my, pt, alpha);
                curY += row.getHeight() + 2;
            }
            ctx.disableScissor();

            // Скроллбар
            float maxScroll = Math.max(0, totalH-visH);
            if (maxScroll > 0) {
                float thumbH = Math.max(15f, visH*(visH/totalH));
                float pct = scrollOffset == 0 ? 0 : -scrollOffset/maxScroll;
                float thumbY = contentStart + (visH-thumbH)*pct;
                RenderUtil.drawRoundedRect(ctx, saX2-4f, thumbY, 2f, thumbH, 1.5f, scrollC);
            }
        }

        // Search bar
        search.draw(ctx, mx, my, pt, alpha);

        // Кнопка настроек цвета
        RenderUtil.drawRoundedRect(ctx, px+W-20, py-14, 18, 14, 3f,
                RenderUtil.withAlpha(ACCENT_COLOR, (int)(alpha*200)));
        ctx.drawText(MinecraftClient.getInstance().textRenderer, "Col",
                (int)(px+W-19), (int)(py-12), 0xFFFFFFFF, false);

        if (showColorPicker) drawColorPicker(ctx, alpha);

        super.render(ctx, mx, my, pt);
    }

    private void drawColorPicker(DrawContext ctx, float alpha) {
        float cpX = px+W-155, cpY = py-14, cpW = 133, cpH = 90;
        RenderUtil.Blur.drawBlur(ctx, cpX, cpY, cpW, cpH, 6f, 10, -1);
        RenderUtil.drawRoundedRect(ctx, cpX, cpY, cpW, cpH, 6f, new Color(20,16,22,(int)(alpha*240)).getRGB());
        ctx.drawText(MinecraftClient.getInstance().textRenderer, "Accent Color",
                (int)(cpX+6), (int)(cpY+5), TEXT_DIM, false);

        // Hue слайдер
        int[] hues = {0xFFFF0000,0xFFFFAA00,0xFFFFFF00,0xFF00FF00,0xFF00FFFF,0xFF0000FF,0xFFAA00FF,0xFFFF0000};
        float hw = (cpW-12)/(hues.length-1);
        for (int i = 0; i < hues.length-1; i++)
            ctx.fill((int)(cpX+6+i*hw),(int)(cpY+20),(int)(cpX+6+(i+1)*hw),(int)(cpY+30), hues[i]);
        ctx.drawText(MinecraftClient.getInstance().textRenderer,"Hue",(int)(cpX+6),(int)(cpY+32),TEXT_DIM,false);

        // Saturation слайдер
        float[] hsb = Color.RGBtoHSB((ACCENT_COLOR>>16)&0xFF,(ACCENT_COLOR>>8)&0xFF,ACCENT_COLOR&0xFF,null);
        float sw = cpW-12;
        for (int i = 0; i < (int)sw; i++) {
            Color c = Color.getHSBColor(hsb[0], i/sw, hsb[2]);
            ctx.fill((int)(cpX+6+i),(int)(cpY+45),(int)(cpX+7+i),(int)(cpY+55), c.getRGB()|0xFF000000);
        }
        ctx.drawText(MinecraftClient.getInstance().textRenderer,"Saturation",(int)(cpX+6),(int)(cpY+57),TEXT_DIM,false);

        // Brightness слайдер
        for (int i = 0; i < (int)sw; i++) {
            Color c = Color.getHSBColor(hsb[0], hsb[1], i/sw);
            ctx.fill((int)(cpX+6+i),(int)(cpY+67),(int)(cpX+7+i),(int)(cpY+77), c.getRGB()|0xFF000000);
        }
        ctx.drawText(MinecraftClient.getInstance().textRenderer,"Brightness",(int)(cpX+6),(int)(cpY+79),TEXT_DIM,false);

        // Превью
        RenderUtil.drawRoundedRect(ctx, cpX+cpW-20, cpY+5, 14, 10, 3f, ACCENT_COLOR|0xFF000000);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (alphaAnim.get() < 0.9f) return false;

        // Кнопка color picker
        if (mx >= px+W-20 && mx <= px+W-2 && my >= py-14 && my <= py) {
            showColorPicker = !showColorPicker; return true;
        }

        if (showColorPicker) {
            float cpX = px+W-155, cpY = py-14, cpW = 133;
            float sliderX1 = cpX+6, sliderX2 = cpX+cpW-6;
            float[] hsb = Color.RGBtoHSB((ACCENT_COLOR>>16)&0xFF,(ACCENT_COLOR>>8)&0xFF,ACCENT_COLOR&0xFF,null);
            if (mx >= sliderX1 && mx <= sliderX2) {
                float t = (float)((mx-sliderX1)/(sliderX2-sliderX1));
                t = Math.max(0, Math.min(1, t));
                if (my >= cpY+20 && my <= cpY+30) { // Hue
                    ACCENT_COLOR = Color.getHSBColor(t, hsb[1], hsb[2]).getRGB()|0xFF000000; return true;
                }
                if (my >= cpY+45 && my <= cpY+55) { // Saturation
                    ACCENT_COLOR = Color.getHSBColor(hsb[0], t, hsb[2]).getRGB()|0xFF000000; return true;
                }
                if (my >= cpY+67 && my <= cpY+77) { // Brightness
                    ACCENT_COLOR = Color.getHSBColor(hsb[0], hsb[1], t).getRGB()|0xFF000000; return true;
                }
            }
        }

        if (search.mouseClicked((float)mx,(float)my,btn)) return true;
        for (List<ModuleRow> rows : columns.values())
            for (ModuleRow row : rows)
                if (row.mouseClicked((float)mx,(float)my,btn)) return true;
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        // Слайдеры color picker при драге
        if (showColorPicker) {
            float cpX = px+W-155, cpY = py-14, cpW = 133;
            float sliderX1 = cpX+6, sliderX2 = cpX+cpW-6;
            float[] hsb = Color.RGBtoHSB((ACCENT_COLOR>>16)&0xFF,(ACCENT_COLOR>>8)&0xFF,ACCENT_COLOR&0xFF,null);
            if (mx >= sliderX1 && mx <= sliderX2) {
                float t = Math.max(0, Math.min(1, (float)((mx-sliderX1)/(sliderX2-sliderX1))));
                if (my >= cpY+20 && my <= cpY+30) { ACCENT_COLOR = Color.getHSBColor(t,hsb[1],hsb[2]).getRGB()|0xFF000000; return true; }
                if (my >= cpY+45 && my <= cpY+55) { ACCENT_COLOR = Color.getHSBColor(hsb[0],t,hsb[2]).getRGB()|0xFF000000; return true; }
                if (my >= cpY+67 && my <= cpY+77) { ACCENT_COLOR = Color.getHSBColor(hsb[0],hsb[1],t).getRGB()|0xFF000000; return true; }
            }
        }
        for (List<ModuleRow> rows : columns.values())
            for (ModuleRow row : rows) row.mouseDragged(mx, my, btn);
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        for (List<ModuleRow> rows : columns.values())
            for (ModuleRow row : rows) row.mouseReleased((float)mx,(float)my,btn);
        search.mouseReleased((float)mx,(float)my,btn);
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hd, double vd) {
        float ly1 = py-10f+22f-6f, ly2 = ly1+H-70f+32f;
        if (my < ly1 || my > ly2) return false;
        Category[] cats = Category.values();
        float gutter=5f, colW=(W-gutter*(cats.length-1)-220)/cats.length, startX=px+20f;
        for (int i=0;i<cats.length;i++) {
            Category c=cats[i];
            float cx1=startX+i*(colW+gutter)+60f;
            if (mx>=cx1&&mx<=cx1+colW) {
                float tgt = targetScroll.getOrDefault(c,0f)+(float)(vd*SCROLL_SPEED);
                float totalH = columns.getOrDefault(c,Collections.emptyList()).stream().mapToFloat(r->r.getHeight()+2).sum();
                float visH = ly2-(py-10f+22f+20f);
                tgt = Math.max(-(Math.max(0,totalH-visH)),Math.min(0,tgt));
                targetScroll.put(c,tgt); return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key==GLFW.GLFW_KEY_ESCAPE||key==GLFW.GLFW_KEY_RIGHT_SHIFT) { startFadeOut(); return true; }
        if (search.keyPressed(key,scan,mods)) return true;
        for (List<ModuleRow> rows:columns.values())
            for (ModuleRow row:rows) if (row.keyPressed(key,scan,mods)) return true;
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
        if (alphaAnim.getToValue()==0.0&&!alphaAnim.isAlive()) client.setScreen(null);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float pt) {
        ctx.fill(0,0,width,height,new Color(0,0,0,(int)(alphaAnim.get()*150)).getRGB());
    }

    @Override public void onClose() { startFadeOut(); }
    public void startFadeOut() { alphaAnim.run(0.0,0.2,AnimationUtil.Easing.CUBIC_IN); }
    @Override public boolean shouldPause() { return false; }

    // ── Setting types (используются в модулях) ─────────────────────
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
