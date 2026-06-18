package com.yourcheat.mixin;

import com.yourcheat.CheatMod;
import com.yourcheat.model.RabbitEntityModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Подменяет рендер модели игрока на RabbitEntityModel когда CustomModels включён
 * и текущая выбранная модель — "Crazy Rabbit". Рисуется поверх ванильной модели
 * (полный cancel требует Accessor на приватные поля рендерера — пока не нужен,
 * т.к. кролик крупнее и визуально перекрывает скин игрока).
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerModelMixin {

    private static RabbitEntityModel rabbitModel;

    @Inject(
        method = "render(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("HEAD")
    )
    private void onRenderHead(PlayerEntityRenderState state, MatrixStack matrices,
                               VertexConsumerProvider vcp, int light, CallbackInfo ci) {
        if (!CheatMod.customModels.isEnabled()) return;
        if (!"Crazy Rabbit".equals(CheatMod.customModels.getCurrentModelName())) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (rabbitModel == null) {
            var loader = mc.getEntityModelLoader();
            rabbitModel = new RabbitEntityModel(loader.getModelPart(RabbitEntityModel.LAYER));
        }

        matrices.push();
        matrices.scale(1.25f, 1.25f, 1.25f);
        matrices.translate(0.0, -0.3, 0.0);

        rabbitModel.setAngles(state);

        var vc = vcp.getBuffer(RenderLayer.getEntityCutout(
                Identifier.of("yourcheat", "textures/models/rabbit.png")));

        rabbitModel.render(matrices, vc, light, OverlayTexture.DEFAULT_UV, 0xFFFFFFFF);

        matrices.pop();
    }
}
