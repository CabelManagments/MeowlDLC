package com.yourcheat;

import com.yourcheat.modules.HitParticlesModule;
import com.yourcheat.modules.JumpCircleModule;
import com.yourcheat.modules.TargetESPModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class CheatMod implements ClientModInitializer {

    public static KeyBinding guiKey;

    // Инстансы модулей
    public static final JumpCircleModule jumpCircle   = new JumpCircleModule();
    public static final TargetESPModule  targetESP    = new TargetESPModule();
    public static final HitParticlesModule hitParticles = new HitParticlesModule();

    @Override
    public void onInitializeClient() {
        // Регистрируем кейбинд Right Shift
        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.yourcheat.gui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "category.yourcheat"
        ));

        // Открываем GUI по кейбинду
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (guiKey.wasPressed() && client.currentScreen == null) {
                client.setScreen(new com.yourcheat.gui.ClickGUI());
            }
        });

        // Регистрируем рендер событие для модулей
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            jumpCircle.onRender(ctx);
            targetESP.onRender(ctx);
            hitParticles.onRender(ctx);
        });
    }
}
