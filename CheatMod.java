package com.yourcheat;

import com.yourcheat.gui.FontRenderer;
import com.yourcheat.gui.HUD;
import com.yourcheat.modules.*;
import com.yourcheat.util.SoundManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class CheatMod implements ClientModInitializer {

    public static KeyBinding guiKey;

    // Визуальные модули
    public static final JumpCircleModule   jumpCircle   = new JumpCircleModule();
    public static final TargetESPModule    targetESP    = new TargetESPModule();
    public static final HitParticlesModule hitParticles = new HitParticlesModule();
    public static final ChinaHatModule     chinaHat     = new ChinaHatModule();
    public static final CapeModule         cape         = new CapeModule();
    public static final TargetHUDModule    targetHUD    = new TargetHUDModule();
    public static final WatermarkModule    watermark    = new WatermarkModule();

    @Override
    public void onInitializeClient() {
        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.yourcheat.gui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "category.yourcheat"
        ));

        // Загружаем шрифт после ресурсов
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
            .registerReloadListener(new SimpleSynchronousResourceReloadListener() {
                @Override public Identifier getFabricId() {
                    return Identifier.of("yourcheat", "font_loader");
                }
                @Override public void reload(ResourceManager manager) {
                    FontRenderer.INSTANCE.init();
                }
            });

        // Тик
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (guiKey.wasPressed() && client.currentScreen == null) {
                client.setScreen(new com.yourcheat.gui.ClickGUI());
            }
            jumpCircle.tick();
        });

        // 3D рендер
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            jumpCircle.onRender(ctx);
            targetESP.onRender(ctx);
            hitParticles.onRender(ctx);
            chinaHat.onRender(ctx);
            cape.onRender(ctx);
        });

        // HUD
        HUD.getInstance().register();
        targetHUD.register();
        watermark.register();

        // Звуки при атаке
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient && entity instanceof LivingEntity living) {
                hitParticles.spawnAt(
                    entity.getX(),
                    entity.getY() + entity.getHeight() * 0.7,
                    entity.getZ()
                );
                SoundManager.playHit();
                if (living.getHealth() - 1f <= 0) SoundManager.playKill();
            }
            return ActionResult.PASS;
        });
    }
}
