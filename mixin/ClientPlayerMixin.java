package com.yourcheat.mixin;

import com.yourcheat.CheatMod;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerMixin {

    // В 1.21.4 yarn метод называется attackEntity, не attack
    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void onAttackEntity(Entity target, CallbackInfo ci) {
        CheatMod.hitParticles.spawnAt(
            target.getX(),
            target.getY() + target.getHeight() * 0.7,
            target.getZ()
        );
    }
}
