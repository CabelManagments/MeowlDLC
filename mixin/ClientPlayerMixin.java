package com.yourcheat.mixin;

// Пустой миксин-заглушка — реальный перехват атаки перенесён на AttackEntityCallback
// (см. CheatMod.java)
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerMixin {
    // пусто
}

