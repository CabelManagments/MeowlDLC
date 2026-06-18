package com.yourcheat.modules;

import com.yourcheat.gui.ClickGUI;
import com.yourcheat.gui.RenderUtil;
import com.yourcheat.gui.RollingNumber;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Watermark в стиле Kronex: "MeowlDLC • username • pingms • fps"
 * с градиентным именем клиента и плавно интерполируемыми числами.
 */
public class WatermarkModule implements IModule {

    private boolean enabled = true;

    private final RollingNumber fps  = new RollingNumber(0, 0.12f);
    private final RollingNumber ping = new RollingNumber(0, 0.08f);

    @Override public String getName()           { return "Watermark"; }
    @Override public boolean isEnabled()        { return enabled; }
    @Override public void setEnabled(boolean v) { enabled = v; }
    @Override public List<ClickGUI.Setting> getSettings() { return new ArrayList<>(); }

    public void register() {
        HudRenderCallback.EVENT.register(this::render);
    }

    private void render(DrawContext ctx, RenderTickCounter counter) {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        var font = mc.textRenderer;
        String separator = " \u2022 "; // •

        fps.setTarget(mc.getCurrentFps());
        var nh = mc.getNetworkHandler();
        if (nh != null) {
            var entry = nh.getPlayerListEntry(mc.player.getUuid());
            ping.setTarget(entry != null ? Math.max(entry.getLatency(), 0) : 0);
        }

        String clientName = "MeowlDLC";
        String username    = separator + mc.getSession().getUsername();
        String pingPart     = separator + ping.getInt() + "ms";
        String fpsPart       = separator + fps.getInt() + "fps";

        int nameW = font.getWidth(clientName);
        int userW = font.getWidth(username);
        int pingW = font.getWidth(pingPart);
        int fpsW  = font.getWidth(fpsPart);
        int totalW = nameW + userW + pingW + fpsW + 10;

        int x = 5, y = 5, h = 16;

        // Фон с акцентной полоской (наш стиль RenderUtil)
        RenderUtil.drawRoundedRect(ctx, x, y, totalW, h, 4f,
                new Color(16, 14, 18, 215).getRGB());

        // Анимированный градиентный цвет имени клиента
        long t = System.currentTimeMillis();
        float hue = 0.74f + (float)(Math.sin(t / 1800.0) * 0.05f);
        int nameColor = Color.getHSBColor(hue, 0.65f, 0.95f).getRGB();

        int tx = x + 6, ty = y + (h - 8) / 2;
        ctx.drawText(font, clientName, tx, ty, nameColor, false);
        tx += nameW;
        ctx.drawText(font, username, tx, ty, new Color(210,210,210,255).getRGB(), false);
        tx += userW;

        // Цвет пинга по значению
        int p = ping.getInt();
        int pingColor = p < 80 ? 0xFF7CFF7C : p < 150 ? 0xFFFFC95C : 0xFFFF6B6B;
        ctx.drawText(font, pingPart, tx, ty, pingColor, false);
        tx += pingW;

        ctx.drawText(font, fpsPart, tx, ty, new Color(190,190,200,255).getRGB(), false);
    }
}
