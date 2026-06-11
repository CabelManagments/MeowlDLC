package com.yourcheat.gui;

import com.yourcheat.CheatMod;
import com.yourcheat.modules.IModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ClickGUI extends Screen {

    // ── Цвета ─────────────────────────────────────────────────────
    static final int BG_DARK      = 0xE5101015;
    static final int BG_PANEL     = 0xF0181820;
    static final int BG_HEADER    = 0xFF201828;
    static final int BG_MODULE    = 0xFF141418;
    static final int BG_MODULE_HV = 0xFF1e1e28;
    static final int BG_SETTINGS  = 0xFF0f0f14;
    static final int ACCENT_ON    = 0xFFc96abf;
    static final int ACCENT_OFF   = 0xFF3a3a4a;
    static final int TEXT_PRIMARY = 0xFFf0eaf8;
    static final int TEXT_DIM     = 0xFF888898;
    static final int TEXT_ACCENT  = 0xFFd488cc;

    static final int PANEL_W   = 160;
    static final int HEADER_H  = 22;
    static final int MODULE_H  = 20;
    static final int TOGGLE_W  = 28;
    static final int TOGGLE_H  = 13;
    static final int PAD       = 6;
    static final int SLIDER_H  = 14;

    private final List<Panel> panels = new ArrayList<>();

    public ClickGUI() {
        super(Text.literal("ClickGUI"));
    }

    @Override
    protected void init() {
        panels.clear();
        List<IModule> allModules = List.of(
            CheatMod.jumpCircle,
            CheatMod.targetESP,
            CheatMod.hitParticles
        );
        panels.add(new Panel("Render", allModules, 20, 40));
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // затемнение
        ctx.fill(0, 0, width, height, 0xB0050508);
        for (Panel p : panels) p.render(ctx, mx, my);
        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        for (Panel p : panels) if (p.mouseClicked(mx, my, btn)) return true;
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        for (Panel p : panels) if (p.mouseDragged(mx, my, dx, dy)) return true;
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        for (Panel p : panels) p.mouseReleased();
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close(); return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override public boolean shouldPause() { return false; }

    // ══════════════════════════════════════════════════════════════
    class Panel {
        String title;
        List<IModule> modules;
        int x, y;
        boolean dragging;
        double dox, doy;
        List<ModuleRow> rows = new ArrayList<>();

        Panel(String title, List<IModule> modules, int x, int y) {
            this.title = title; this.modules = modules;
            this.x = x; this.y = y;
            for (IModule m : modules) rows.add(new ModuleRow(m));
        }

        int totalH() { return HEADER_H + rows.stream().mapToInt(ModuleRow::height).sum(); }

        void render(DrawContext ctx, int mx, int my) {
            int h = totalH();
            // тень
            ctx.fill(x+3, y+3, x+PANEL_W+3, y+h+3, 0x55000000);
            // фон
            ctx.fill(x, y, x+PANEL_W, y+h, BG_PANEL);
            // акцентная полоска
            ctx.fill(x, y, x+PANEL_W, y+3, ACCENT_ON);
            // заголовок
            ctx.fill(x, y, x+PANEL_W, y+HEADER_H, BG_HEADER);
            ctx.drawText(textRenderer, title, x+10, y+7, TEXT_PRIMARY, false);

            int ry = y + HEADER_H;
            for (ModuleRow row : rows) {
                row.render(ctx, mx, my, x, ry);
                ry += row.height();
            }
        }

        boolean mouseClicked(double mx, double my, int btn) {
            if (mx>=x && mx<=x+PANEL_W && my>=y && my<y+HEADER_H) {
                dragging=true; dox=mx-x; doy=my-y; return true;
            }
            int ry = y+HEADER_H;
            for (ModuleRow row : rows) {
                if (row.mouseClicked(mx, my, btn, x, ry)) return true;
                ry += row.height();
            }
            return false;
        }

        boolean mouseDragged(double mx, double my, double dx, double dy) {
            if (dragging) { x=(int)(mx-dox); y=(int)(my-doy); return true; }
            int ry = y+HEADER_H;
            for (ModuleRow row : rows) {
                if (row.mouseDragged(mx, my, x, ry)) return true;
                ry += row.height();
            }
            return false;
        }

        void mouseReleased() {
            dragging = false;
            for (ModuleRow row : rows) row.mouseReleased();
        }
    }

    // ══════════════════════════════════════════════════════════════
    class ModuleRow {
        IModule module;
        boolean settingsOpen = false;

        ModuleRow(IModule m) { this.module = m; }

        int height() {
            if (!settingsOpen) return MODULE_H;
            int h = MODULE_H;
            for (Setting s : module.getSettings()) h += s.getHeight();
            return h;
        }

        void render(DrawContext ctx, int mx, int my, int px, int py) {
            boolean hov = mx>=px && mx<=px+PANEL_W && my>=py && my<py+MODULE_H;
            ctx.fill(px, py, px+PANEL_W, py+MODULE_H, hov ? BG_MODULE_HV : BG_MODULE);
            ctx.fill(px+4, py+MODULE_H-1, px+PANEL_W-4, py+MODULE_H, 0xFF1e1e28);

            int nameColor = module.isEnabled() ? TEXT_ACCENT : TEXT_PRIMARY;
            ctx.drawText(textRenderer, module.getName(), px+8, py+6, nameColor, false);

            if (!module.getSettings().isEmpty()) {
                ctx.drawText(textRenderer, settingsOpen ? "▴" : "▾", px+PANEL_W-20, py+6, TEXT_DIM, false);
            }
            renderToggle(ctx, px+PANEL_W-TOGGLE_W-4, py+(MODULE_H-TOGGLE_H)/2);

            if (settingsOpen) {
                int sy = py+MODULE_H;
                for (Setting s : module.getSettings()) {
                    s.render(ctx, mx, my, px, sy);
                    sy += s.getHeight();
                }
            }
        }

        void renderToggle(DrawContext ctx, int tx, int ty) {
            boolean en = module.isEnabled();
            ctx.fill(tx, ty, tx+TOGGLE_W, ty+TOGGLE_H, en ? ACCENT_ON : ACCENT_OFF);
            int kx = en ? tx+TOGGLE_W-TOGGLE_H+2 : tx+2;
            ctx.fill(kx, ty+2, kx+TOGGLE_H-4, ty+TOGGLE_H-2, 0xFFffffff);
        }

        boolean mouseClicked(double mx, double my, int btn, int px, int py) {
            if (mx<px || mx>px+PANEL_W) return false;
            if (my>=py && my<py+MODULE_H) {
                int tx = px+PANEL_W-TOGGLE_W-4;
                if (mx>=tx && mx<=tx+TOGGLE_W) { module.toggle(); return true; }
                if (!module.getSettings().isEmpty() && mx>=px+PANEL_W-24 && mx<=px+PANEL_W-4) {
                    settingsOpen = !settingsOpen; return true;
                }
                module.toggle(); return true;
            }
            if (settingsOpen) {
                int sy = py+MODULE_H;
                for (Setting s : module.getSettings()) {
                    if (s.mouseClicked(mx, my, btn, px, sy)) return true;
                    sy += s.getHeight();
                }
            }
            return false;
        }

        boolean mouseDragged(double mx, double my, int px, int py) {
            if (!settingsOpen) return false;
            int sy = py+MODULE_H;
            for (Setting s : module.getSettings()) {
                if (s.mouseDragged(mx, my, px, sy)) return true;
                sy += s.getHeight();
            }
            return false;
        }

        void mouseReleased() { for (Setting s : module.getSettings()) s.mouseReleased(); }
    }

    // ══════════════════════════════════════════════════════════════
    // Settings
    // ══════════════════════════════════════════════════════════════
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
        float value, min, max;
        boolean dragging;
        final Consumer<Float> onChange;

        public SliderSetting(String label, float value, float min, float max, Consumer<Float> onChange) {
            super(label); this.value=value; this.min=min; this.max=max; this.onChange=onChange;
        }

        @Override public int getHeight() { return PAD + SLIDER_H + 4; }

        @Override
        public void render(DrawContext ctx, int mx, int my, int px, int py) {
            int sx=px+PAD, sw=PANEL_W-PAD*2, sy=py+PAD;
            String txt = label+": "+String.format("%.1f", value);
            ctx.drawText(net.minecraft.client.MinecraftClient.getInstance().textRenderer, txt, sx, sy, TEXT_DIM, false);
            int trackY = sy+9;
            ctx.fill(sx, trackY, sx+sw, trackY+4, 0xFF2a2a35);
            int fw = (int)((value-min)/(max-min)*sw);
            if (fw>0) ctx.fill(sx, trackY, sx+fw, trackY+4, ACCENT_ON);
            ctx.fill(sx+fw-3, trackY-2, sx+fw+5, trackY+6, 0xFFffffff);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int btn, int px, int py) {
            int sx=px+PAD, sw=PANEL_W-PAD*2, trackY=py+PAD+9;
            if (mx>=sx && mx<=sx+sw && my>=trackY-4 && my<=trackY+8) {
                dragging=true; update(mx,sx,sw); return true;
            }
            return false;
        }

        @Override
        public boolean mouseDragged(double mx, double my, int px, int py) {
            if (!dragging) return false;
            update(mx, px+PAD, PANEL_W-PAD*2); return true;
        }

        @Override public void mouseReleased() { dragging=false; }

        private void update(double mx, int sx, int sw) {
            float t = Math.max(0, Math.min(1, (float)((mx-sx)/sw)));
            value = min + t*(max-min);
            value = Math.round(value/0.05f)*0.05f;
            onChange.accept(value);
        }
    }

    public static class BoolSetting extends Setting {
        boolean value;
        final Consumer<Boolean> onChange;

        public BoolSetting(String label, boolean value, Consumer<Boolean> onChange) {
            super(label); this.value=value; this.onChange=onChange;
        }

        @Override public int getHeight() { return MODULE_H; }

        @Override
        public void render(DrawContext ctx, int mx, int my, int px, int py) {
            ctx.fill(px, py, px+PANEL_W, py+MODULE_H, BG_SETTINGS);
            ctx.drawText(net.minecraft.client.MinecraftClient.getInstance().textRenderer, "  "+label, px+8, py+6, TEXT_DIM, false);
            int tx=px+PANEL_W-24, ty=py+4;
            ctx.fill(tx, ty, tx+18, ty+10, value ? ACCENT_ON : ACCENT_OFF);
            ctx.fill(value?tx+9:tx+1, ty+1, value?tx+17:tx+9, ty+9, 0xFFffffff);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int btn, int px, int py) {
            if (mx>=px && mx<=px+PANEL_W && my>=py && my<py+MODULE_H) {
                value=!value; onChange.accept(value); return true;
            }
            return false;
        }
    }

    public static class ColorSetting extends Setting {
        float hue;
        Color current;
        boolean expanded=false;
        final Consumer<Color> onChange;

        public ColorSetting(String label, Color initial, Consumer<Color> onChange) {
            super(label);
            float[] hsb = Color.RGBtoHSB(initial.getRed(), initial.getGreen(), initial.getBlue(), null);
            hue=hsb[0]; current=initial; this.onChange=onChange;
        }

        @Override public int getHeight() { return expanded ? PAD+20+SLIDER_H+4 : PAD+20; }

        @Override
        public void render(DrawContext ctx, int mx, int my, int px, int py) {
            var tr = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
            ctx.drawText(tr, label, px+PAD, py+PAD+4, TEXT_DIM, false);
            int pc = 0xFF000000|(current.getRed()<<16)|(current.getGreen()<<8)|current.getBlue();
            ctx.fill(px+PANEL_W-28, py+PAD+2, px+PANEL_W-10, py+PAD+16, pc);
            ctx.fill(px+PANEL_W-10, py+PAD+2, px+PANEL_W-2, py+PAD+16, expanded?ACCENT_ON:ACCENT_OFF);
            if (expanded) {
                int sx=px+PAD, sw=PANEL_W-PAD*2, ty=py+PAD+20;
                // упрощённая радуга (6 цветов)
                int[] hc = {0xFFFF0000,0xFFFFFF00,0xFF00FF00,0xFF00FFFF,0xFF0000FF,0xFFFF00FF};
                int segW = sw/6;
                for (int i=0;i<6;i++) ctx.fill(sx+i*segW, ty, sx+(i+1)*segW, ty+6, hc[i]);
                int tx2 = sx+(int)(hue*sw)-2;
                ctx.fill(tx2, ty-2, tx2+4, ty+8, 0xFFffffff);
            }
        }

        @Override
        public boolean mouseClicked(double mx, double my, int btn, int px, int py) {
            if (mx>=px+PANEL_W-10 && mx<=px+PANEL_W-2 && my>=py+PAD+2 && my<=py+PAD+16) {
                expanded=!expanded; return true;
            }
            if (expanded) {
                int sx=px+PAD, sw=PANEL_W-PAD*2, ty=py+PAD+20;
                if (mx>=sx && mx<=sx+sw && my>=ty-2 && my<=ty+8) {
                    hue=Math.max(0,Math.min(0.99f,(float)((mx-sx)/sw)));
                    current=Color.getHSBColor(hue,0.8f,0.9f);
                    onChange.accept(current); return true;
                }
            }
            return false;
        }
    }
}

