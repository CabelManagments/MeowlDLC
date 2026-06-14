package com.yourcheat;

import com.yourcheat.gui.HUD;
import com.yourcheat.modules.HitParticlesModule;
import com.yourcheat.modules.JumpCircleModule;
import com.yourcheat.modules.TargetESPModule;
import com.yourcheat.util.SoundManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

public class CheatMod implements ClientModInitializer {

    public static KeyBinding guiKey;

    public static final JumpCircleModule   jumpCircle   = new JumpCircleModule();
    public static final TargetESPModule    targetESP    = new TargetESPModule();
    public static final HitParticlesModule hitParticles = new HitParticlesModule();

    @Override
    public void onInitializeClient() {
        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.yourcheat.gui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "category.yourcheat"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (guiKey.wasPressed() && client.currentScreen == null) {
                client.setScreen(new com.yourcheat.gui.ClickGUI());
            }
            jumpCircle.tick();
        });

        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            jumpCircle.onRender(ctx);
            targetESP.onRender(ctx);
            hitParticles.onRender(ctx);
        });

        HUD.getInstance().register();

        // HitSound + HitParticles + KillSound
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient && entity instanceof LivingEntity living) {
                hitParticles.spawnAt(
                    entity.getX(),
                    entity.getY() + entity.getHeight() * 0.7,
                    entity.getZ()
                );
                SoundManager.playHit();
                // KillSound при смерти цели
                if (living.getHealth() - 1f <= 0) {
                    SoundManager.playKill();
                }
            }
            return ActionResult.PASS;
        });
    }
}
