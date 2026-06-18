package com.yourcheat.modules;

import com.yourcheat.gui.ClickGUI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ZoomModule implements IModule {

    private boolean enabled = false;
    public int keyBind     = GLFW.GLFW_KEY_C;
    public float zoomFov   = 30f;   // FOV во время зума
    public float smoothness = 0.25f;

    private boolean zooming     = false;
    private double  originalFov;
    private double  currentFov;
    private boolean fovCaptured = false;

    @Override public String getName()           { return "Zoom"; }
    @Override public boolean isEnabled()        { return enabled; }
    @Override public void setEnabled(boolean v) {
        enabled = v;
        if (!v) restoreFov();
    }

    @Override
    public List<ClickGUI.Setting> getSettings() {
        List<ClickGUI.Setting> list = new ArrayList<>();
        list.add(new ClickGUI.SliderSetting("Zoom FOV",   zoomFov,    5f,  90f, v -> zoomFov   = v));
        list.add(new ClickGUI.SliderSetting("Smoothness", smoothness, 0.05f,0.6f,v -> smoothness = v));
        return list;
    }

    /** Вызывается каждый клиентский тик из CheatMod */
    public void tick() {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null) return;

        boolean keyDown = mc.currentScreen == null &&
                GLFW.glfwGetKey(mc.getWindow().getHandle(), keyBind) == GLFW.GLFW_PRESS;

        if (keyDown && !zooming) {
            zooming = true;
            originalFov = mc.options.getFov().getValue();
            currentFov  = originalFov;
            fovCaptured = true;
        } else if (!keyDown && zooming) {
            zooming = false;
        }

        if (!fovCaptured) return;

        double target = zooming ? zoomFov : originalFov;
        currentFov += (target - currentFov) * smoothness;

        if (!zooming && Math.abs(currentFov - originalFov) < 0.5) {
            mc.options.getFov().setValue((int) originalFov);
            fovCaptured = false;
            return;
        }

        mc.options.getFov().setValue((int) MathHelper.clamp(currentFov, 1, 130));
    }

    private void restoreFov() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (fovCaptured && mc.options != null) {
            mc.options.getFov().setValue((int) originalFov);
        }
        zooming = false;
        fovCaptured = false;
    }
}

