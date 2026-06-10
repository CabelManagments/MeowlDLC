package com.yourcheat.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.yourcheat.modules.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.StringTextComponent;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ClickGUI — открывается на Right Shift.
 * Стиль: тёмный фон (#1a1a1f), розово-фиолетовые акценты (#c96abf / #7c4dff),
 * панели по категориям, модули с toggle + настройки (слайдеры, color picker).
 *
 * Регистрация кейбинда в главном классе клиента:
 *   KeyBinding guiKey = new KeyBinding("ClickGUI", GLFW.GLFW_KEY_RIGHT_SHIFT, "YourClient");
 *   if (guiKey.isPressed()) Minecraft.getInstance().displayGuiScreen(new ClickGUI());
 */
public class ClickGUI extends Screen {

    // ── Цвета (ARGB) ──────────────────────────────────────────────
    public static final int BG_DARK      = 0xE5101015;  // основной фон
    public static final int BG_PANEL     = 0xF0181820;  // панель
    public static final int BG_HEADER    = 0xFF201828;  // заголовок категории
    public static final int BG_MODULE    = 0xFF141418;  // строка модуля
    public static final int BG_MODULE_HV = 0xFF1e1e28;  // hover строки
    public static final int BG_SETTINGS  = 0xFF0f0f14;  // фон настроек
    public static final int ACCENT_ON    = 0xFFc96abf;  // toggle включён (розовый)
    public static final int ACCENT_OFF   = 0xFF3a3a4a;  // toggle выключен
    public static final int TEXT_PRIMARY = 0xFFf0eaf8;
    public static final int TEXT_DIM     = 0xFF888898;
    public static final int TEXT_ACCENT  = 0xFFd488cc;

    // ── Размеры ───────────────────────────────────────────────────
    private static final int PANEL_W      = 180;
    private static final int HEADER_H     = 22;
    private static final int MODULE_H     = 20;
    private static final int TOGGLE_W     = 28;
    private static final int TOGGLE_H     = 13;
    private static final int SETTINGS_PAD = 6;
    private static final int SLIDER_H     = 14;
    private static final int COLOR_H      = 20;

    private final List<CategoryPanel> panels = new ArrayList<>();
    private float animTick = 0f; // для плавного открытия

    // ── Категории ─────────────────────────────────────────────────
    public enum Category { COMBAT, MOVEMENT, PLAYER, RENDER }

    public ClickGUI() {
        super(new StringTextComponent("ClickGUI"));
        buildPanels();
    }

    private void buildPanels() {
        panels.add(new CategoryPanel("Combat",   Category.COMBAT,   20,  40));
        panels.add(new CategoryPanel("Movement", Category.MOVEMENT, 220, 40));
        panels.add(new CategoryPanel("Player",   Category.PLAYER,   420, 40));
        panels.add(new CategoryPanel("Render",   Category.RENDER,   620, 40));
    }

    @Override
    public void render(MatrixStack ms, int mx, int my, float pt) {
        // затемнение фона
        fillGradient(ms, 0, 0, width, height, 0xB5050508, 0xBF080810);

        animTick = Math.min(1f, animTick + 0.07f);

        for (CategoryPanel p : panels) {
            p.render(ms, mx, my, animTick);
        }

        super.render(ms, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        for (CategoryPanel p : panels) {
            if (p.mouseClicked(mx, my, btn)) return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        for (CategoryPanel p : panels) {
            if (p.mouseDragged(mx, my, btn, dx, dy)) return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        for (CategoryPanel p : panels) p.mouseReleased();
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        for (CategoryPanel p : panels) {
            if (p.mouseScrolled(mx, my, delta)) return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT || key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ═══════════════════════════════════════════════════════════════
    // ── CategoryPanel ──────────────────────────────────────────────
    // ═══════════════════════════════════════════════════════════════
    public class CategoryPanel {
        final String title;
        final Category category;
        int x, y;
        boolean dragging;
        double dragOX, dragOY;
        final List<ModuleEntry> modules = new ArrayList<>();

        CategoryPanel(String title, Category cat, int x, int y) {
            this.title = title;
            this.category = cat;
            this.x = x;
            this.y = y;
            populateModules(cat);
        }

        private void populateModules(Category cat) {
            switch (cat) {
                case RENDER:
                    modules.add(new ModuleEntry("JumpCircle",  JumpCircleModule.INSTANCE));
                    modules.add(new ModuleEntry("TargetESP",   TargetESPModule.INSTANCE));
                    modules.add(new ModuleEntry("HitParticles",HitParticlesModule.INSTANCE));
                    break;
                case COMBAT:
                    modules.add(new ModuleEntry("KillAura",    null));
                    modules.add(new ModuleEntry("AutoClicker", null));
                    modules.add(new ModuleEntry("Reach",       null));
                    break;
                case MOVEMENT:
                    modules.add(new ModuleEntry("Sprint",      null));
                    modules.add(new ModuleEntry("Bhop",        null));
                    modules.add(new ModuleEntry("Speed",       null));
                    break;
                case PLAYER:
                    modules.add(new ModuleEntry("NoFall",      null));
                    modules.add(new ModuleEntry("AntiKB",      null));
                    break;
            }
        }

        void render(MatrixStack ms, int mx, int my, float anim) {
            int totalH = HEADER_H + modules.stream().mapToInt(m -> m.getHeight()).sum();
            int rx = x, ry = y;

            // shadow
            DrawUtils.drawShadow(ms, rx - 4, ry - 4, PANEL_W + 8, totalH + 8, 8, 0x60000000);

            // панель фон
            DrawUtils.fillRoundRect(ms, rx, ry, PANEL_W, totalH, 6, BG_PANEL);

            // акцентная полоска сверху
            DrawUtils.fillRoundRect(ms, rx, ry, PANEL_W, 3, 3, ACCENT_ON);

            // заголовок категории
            DrawUtils.fillRoundRect(ms, rx, ry, PANEL_W, HEADER_H, 6, BG_HEADER);
            Minecraft.getInstance().fontRenderer.drawString(
                ms, title, rx + 10, ry + 7, TEXT_PRIMARY);

            // модули
            int moduleY = ry + HEADER_H;
            for (ModuleEntry m : modules) {
                m.render(ms, mx, my, rx, moduleY);
                moduleY += m.getHeight();
            }
        }

        boolean mouseClicked(double mx, double my, int btn) {
            int totalH = HEADER_H + modules.stream().mapToInt(m -> m.getHeight()).sum();
            // drag на хедере
            if (mx >= x && mx <= x + PANEL_W && my >= y && my <= y + HEADER_H) {
                dragging = true;
                dragOX = mx - x;
                dragOY = my - y;
                return true;
            }
            // клики по модулям
            int moduleY = y + HEADER_H;
            for (ModuleEntry m : modules) {
                if (m.mouseClicked(mx, my, btn, x, moduleY)) return true;
                moduleY += m.getHeight();
            }
            return false;
        }

        boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
            if (dragging) {
                x = (int)(mx - dragOX);
                y = (int)(my - dragOY);
                return true;
            }
            // слайдеры
            int moduleY = y + HEADER_H;
            for (ModuleEntry m : modules) {
                if (m.mouseDragged(mx, my, x, moduleY)) return true;
                moduleY += m.getHeight();
            }
            return false;
        }

        void mouseReleased() {
            dragging = false;
            for (ModuleEntry m : modules) m.mouseReleased();
        }

        boolean mouseScrolled(double mx, double my, double delta) { return false; }
    }

    // ═══════════════════════════════════════════════════════════════
    // ── ModuleEntry ────────────────────────────────────────────────
    // ═══════════════════════════════════════════════════════════════
    public static class ModuleEntry {
        final String name;
        final IClientModule module; // null = заглушка
        boolean settingsOpen = false;
        boolean hovered = false;

        ModuleEntry(String name, IClientModule module) {
            this.name = name;
            this.module = module;
        }

        boolean isEnabled() { return module != null && module.isEnabled(); }

        List<Setting> getSettings() {
            return module != null ? module.getSettings() : new ArrayList<>();
        }

        int getHeight() {
            if (!settingsOpen) return MODULE_H;
            int h = MODULE_H;
            for (Setting s : getSettings()) h += s.getHeight();
            return h;
        }

        void render(MatrixStack ms, int mx, int my, int px, int py) {
            hovered = mx >= px && mx <= px + PANEL_W && my >= py && my < py + MODULE_H;
            int bgColor = hovered ? BG_MODULE_HV : BG_MODULE;
            DrawUtils.fillRect(ms, px, py, PANEL_W, MODULE_H, bgColor);

            // separator line
            DrawUtils.fillRect(ms, px + 4, py + MODULE_H - 1, PANEL_W - 8, 1, 0xFF1e1e28);

            // имя модуля
            int nameColor = isEnabled() ? TEXT_ACCENT : TEXT_PRIMARY;
            Minecraft.getInstance().fontRenderer.drawString(ms, name, px + 8, py + 6, nameColor);

            // стрелка настроек
            if (!getSettings().isEmpty()) {
                String arrow = settingsOpen ? "▴" : "▾";
                Minecraft.getInstance().fontRenderer.drawString(ms, arrow,
                    px + PANEL_W - 22, py + 6, TEXT_DIM);
            }

            // toggle
            renderToggle(ms, px + PANEL_W - TOGGLE_W - 6, py + (MODULE_H - TOGGLE_H) / 2);

            // настройки
            if (settingsOpen) {
                int sy = py + MODULE_H;
                for (Setting s : getSettings()) {
                    s.render(ms, mx, my, px, sy);
                    sy += s.getHeight();
                }
            }
        }

        void renderToggle(MatrixStack ms, int tx, int ty) {
            boolean en = isEnabled();
            int trackColor = en ? ACCENT_ON : ACCENT_OFF;
            DrawUtils.fillRoundRect(ms, tx, ty, TOGGLE_W, TOGGLE_H, TOGGLE_H / 2, trackColor);
            int knobX = en ? tx + TOGGLE_W - TOGGLE_H + 2 : tx + 2;
            DrawUtils.fillRoundRect(ms, knobX, ty + 2, TOGGLE_H - 4, TOGGLE_H - 4,
                (TOGGLE_H - 4) / 2, 0xFFffffff);
        }

        boolean mouseClicked(double mx, double my, int btn, int px, int py) {
            if (mx < px || mx > px + PANEL_W) return false;
            if (my >= py && my < py + MODULE_H) {
                // клик по toggle
                int tx = px + PANEL_W - TOGGLE_W - 6;
                int ty = py + (MODULE_H - TOGGLE_H) / 2;
                if (mx >= tx && mx <= tx + TOGGLE_W && my >= ty && my <= ty + TOGGLE_H) {
                    if (module != null) module.toggle();
                    return true;
                }
                // клик по стрелке настроек
                if (!getSettings().isEmpty() && mx >= px + PANEL_W - 22 && mx <= px + PANEL_W - 8) {
                    settingsOpen = !settingsOpen;
                    return true;
                }
                // клик по имени = toggle
                if (mx >= px + 4 && mx < px + PANEL_W - TOGGLE_W - 10) {
                    if (module != null) module.toggle();
                    return true;
                }
                return true;
            }
            // клики по настройкам
            if (settingsOpen) {
                int sy = py + MODULE_H;
                for (Setting s : getSettings()) {
                    if (s.mouseClicked(mx, my, btn, px, sy)) return true;
                    sy += s.getHeight();
                }
            }
            return false;
        }

        boolean mouseDragged(double mx, double my, int px, int py) {
            if (!settingsOpen) return false;
            int sy = py + MODULE_H;
            for (Setting s : getSettings()) {
                if (s.mouseDragged(mx, my, px, sy)) return true;
                sy += s.getHeight();
            }
            return false;
        }

        void mouseReleased() {
            for (Setting s : getSettings()) s.mouseReleased();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ── Settings ───────────────────────────────────────────────────
    // ═══════════════════════════════════════════════════════════════
    public static abstract class Setting {
        final String label;
        Setting(String label) { this.label = label; }
        abstract int getHeight();
        abstract void render(MatrixStack ms, int mx, int my, int px, int py);
        boolean mouseClicked(double mx, double my, int btn, int px, int py) { return false; }
        boolean mouseDragged(double mx, double my, int px, int py) { return false; }
        void mouseReleased() {}
    }

    /** Слайдер (float диапазон) */
    public static class SliderSetting extends Setting {
        float value, min, max;
        boolean dragging;
        final java.util.function.Consumer<Float> onChange;

        public SliderSetting(String label, float value, float min, float max,
                             java.util.function.Consumer<Float> onChange) {
            super(label);
            this.value = value; this.min = min; this.max = max;
            this.onChange = onChange;
        }

        @Override public int getHeight() { return SETTINGS_PAD + SLIDER_H + 4; }

        @Override
        public void render(MatrixStack ms, int mx, int my, int px, int py) {
            int sx = px + SETTINGS_PAD;
            int sw = PANEL_W - SETTINGS_PAD * 2;
            int sy = py + SETTINGS_PAD;

            // label + value
            String txt = label + ": " + String.format("%.1f", value);
            Minecraft.getInstance().fontRenderer.drawString(ms, txt, sx, sy, TEXT_DIM);

            // track
            int trackY = sy + 9;
            DrawUtils.fillRoundRect(ms, sx, trackY, sw, 4, 2, 0xFF2a2a35);

            // fill
            int fillW = (int)((value - min) / (max - min) * sw);
            if (fillW > 0) DrawUtils.fillRoundRect(ms, sx, trackY, fillW, 4, 2, ACCENT_ON);

            // thumb
            int thumbX = sx + fillW - 4;
            DrawUtils.fillRoundRect(ms, thumbX, trackY - 2, 8, 8, 4, 0xFFffffff);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int btn, int px, int py) {
            int sx = px + SETTINGS_PAD, sw = PANEL_W - SETTINGS_PAD * 2;
            int trackY = py + SETTINGS_PAD + 9;
            if (mx >= sx && mx <= sx + sw && my >= trackY - 4 && my <= trackY + 8) {
                dragging = true;
                updateValue(mx, sx, sw);
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseDragged(double mx, double my, int px, int py) {
            if (!dragging) return false;
            updateValue(mx, px + SETTINGS_PAD, PANEL_W - SETTINGS_PAD * 2);
            return true;
        }

        @Override public void mouseReleased() { dragging = false; }

        private void updateValue(double mx, int sx, int sw) {
            float t = (float)((mx - sx) / sw);
            t = Math.max(0, Math.min(1, t));
            value = min + t * (max - min);
            // округление до 0.05
            value = Math.round(value / 0.05f) * 0.05f;
            onChange.accept(value);
        }
    }

    /** Color picker (HSB колесо упрощённое) */
    public static class ColorSetting extends Setting {
        int r, g, b;
        boolean expanded = false;
        final java.util.function.Consumer<Color> onChange;
        // Простой hue-слайдер
        float hue;

        public ColorSetting(String label, Color initial, java.util.function.Consumer<Color> onChange) {
            super(label);
            float[] hsb = Color.RGBtoHSB(initial.getRed(), initial.getGreen(), initial.getBlue(), null);
            hue = hsb[0];
            Color c = Color.getHSBColor(hue, 0.8f, 0.9f);
            r = c.getRed(); g = c.getGreen(); b = c.getBlue();
            this.onChange = onChange;
        }

        @Override public int getHeight() { return expanded ? SETTINGS_PAD + COLOR_H + SLIDER_H + 6 : SETTINGS_PAD + COLOR_H; }

        @Override
        public void render(MatrixStack ms, int mx, int my, int px, int py) {
            int sx = px + SETTINGS_PAD;
            int sy = py + SETTINGS_PAD / 2;
            Minecraft.getInstance().fontRenderer.drawString(ms, label, sx, sy + 6, TEXT_DIM);

            // превью цвета
            int previewColor = 0xFF000000 | (r << 16) | (g << 8) | b;
            DrawUtils.fillRoundRect(ms, px + PANEL_W - 28, sy + 2, 18, 14, 3, previewColor);
            DrawUtils.fillRoundRect(ms, px + PANEL_W - 10, sy + 2, 8, 14, 3,
                expanded ? ACCENT_ON : ACCENT_OFF); // expand btn

            if (expanded) {
                // hue slider
                int trackY = sy + COLOR_H + 2;
                int sw = PANEL_W - SETTINGS_PAD * 2;
                // рисуем радугу
                renderHueBar(ms, sx, trackY, sw, 6);
                // указатель
                int thumbX = sx + (int)(hue * sw);
                DrawUtils.fillRoundRect(ms, thumbX - 3, trackY - 2, 6, 10, 3, 0xFFffffff);
            }
        }

        private void renderHueBar(MatrixStack ms, int x, int y, int w, int h) {
            // Рисуем 6 градиентных секций (цветовой спектр)
            int steps = 6;
            int segW = w / steps;
            int[] hueColors = {0xFFFF0000,0xFFFFFF00,0xFF00FF00,0xFF00FFFF,0xFF0000FF,0xFFFF00FF,0xFFFF0000};
            for (int i = 0; i < steps; i++) {
                // fillGradient горизонтально не поддерживается напрямую через fillRect
                // поэтому делаем кусочки
                DrawUtils.fillRect(ms, x + i * segW, y, segW, h, hueColors[i]);
            }
        }

        @Override
        public boolean mouseClicked(double mx, double my, int btn, int px, int py) {
            int sy = py + SETTINGS_PAD / 2;
            // expand кнопка
            if (mx >= px + PANEL_W - 10 && mx <= px + PANEL_W - 2 &&
                my >= sy + 2 && my <= sy + 16) {
                expanded = !expanded;
                return true;
            }
            if (expanded) {
                int sx = px + SETTINGS_PAD, sw = PANEL_W - SETTINGS_PAD * 2;
                int trackY = (int)(sy + COLOR_H + 2);
                if (mx >= sx && mx <= sx + sw && my >= trackY - 2 && my <= trackY + 8) {
                    hue = (float)((mx - sx) / sw);
                    hue = Math.max(0, Math.min(0.99f, hue));
                    Color c = Color.getHSBColor(hue, 0.8f, 0.9f);
                    r = c.getRed(); g = c.getGreen(); b = c.getBlue();
                    onChange.accept(c);
                    return true;
                }
            }
            return false;
        }
    }

    /** Boolean toggle настройка (например "Use hearts") */
    public static class BoolSetting extends Setting {
        boolean value;
        final java.util.function.Consumer<Boolean> onChange;

        public BoolSetting(String label, boolean value, java.util.function.Consumer<Boolean> onChange) {
            super(label);
            this.value = value;
            this.onChange = onChange;
        }

        @Override public int getHeight() { return MODULE_H; }

        @Override
        public void render(MatrixStack ms, int mx, int my, int px, int py) {
            DrawUtils.fillRect(ms, px, py, PANEL_W, MODULE_H, BG_SETTINGS);
            Minecraft.getInstance().fontRenderer.drawString(ms, "  " + label, px + 8, py + 6, TEXT_DIM);
            // мини-toggle
            int tx = px + PANEL_W - 24, ty = py + 4;
            DrawUtils.fillRoundRect(ms, tx, ty, 18, 10, 5, value ? ACCENT_ON : ACCENT_OFF);
            DrawUtils.fillRoundRect(ms, value ? tx + 9 : tx + 1, ty + 1, 8, 8, 4, 0xFFffffff);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int btn, int px, int py) {
            if (mx >= px && mx <= px + PANEL_W && my >= py && my < py + MODULE_H) {
                value = !value;
                onChange.accept(value);
                return true;
            }
            return false;
        }
    }
}

