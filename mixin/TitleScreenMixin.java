package com.yourcheat.mixin;

import com.yourcheat.gui.MainMenu;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Полностью заменяет ванильный TitleScreen на наш MainMenu при инициализации.
 */
@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void replaceWithMainMenu(CallbackInfo ci) {
        MinecraftClient.getInstance().setScreen(new MainMenu());
        ci.cancel();
    }
}
