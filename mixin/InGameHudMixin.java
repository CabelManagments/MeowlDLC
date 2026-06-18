package com.yourcheat.mixin;

import com.yourcheat.modules.NoFluidModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
    private void cancelFluidOverlay(DrawContext ctx, net.minecraft.util.Identifier texture,
                                     float opacity, CallbackInfo ci) {
        if (NoFluidModule.ACTIVE) ci.cancel();
    }
}

