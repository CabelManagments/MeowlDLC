package com.yourcheat;

import com.yourcheat.gui.ClickGUI;
import com.yourcheat.modules.HitParticlesModule;
import com.yourcheat.modules.JumpCircleModule;
import com.yourcheat.modules.TargetESPModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

@Mod("yourcheat")
public class CheatMod {

    public static KeyBinding guiKey;

    public CheatMod() {
        // Регистрируем onClientSetup на MOD bus
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
        // Регистрируем события (KeyInput, и модули) на FORGE bus
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(JumpCircleModule.INSTANCE);
        MinecraftForge.EVENT_BUS.register(TargetESPModule.INSTANCE);
        MinecraftForge.EVENT_BUS.register(HitParticlesModule.INSTANCE);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        guiKey = new KeyBinding("Open ClickGUI", GLFW.GLFW_KEY_RIGHT_SHIFT, "YourCheat");
        ClientRegistry.registerKeyBinding(guiKey);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (guiKey != null && guiKey.isPressed() && mc.currentScreen == null) {
            mc.displayGuiScreen(new ClickGUI());
        }
    }
}

