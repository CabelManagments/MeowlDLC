package com.yourcheat.mixin;

import com.yourcheat.CheatMod;
import com.yourcheat.model.RabbitEntityModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Подменяет рендер модели игрока на RabbitEntityModel когда CustomModels включён
 * и текущая выбранная модель — "Crazy Rabbit". Применяется к себе и (опционально) друзьям.
 *
 * Подход: рисуем кастомную модель ПОВЕРХ обычной (cancel оригинального render
 * сложнее из-за приватных полей PlayerEntityRenderer, поэтому используем HEAD-инъекцию
 * на render() и рисуем кролика, а ванильную модель скрываем через invisible-трюк
 * не требуется — кролик крупнее и визуально перекрывает скин).
 */
@Mixin(PlayerEntityRenderer.class)
public class PlayerModelMixin {

    private static RabbitEntityModel rabbitModel;

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(PlayerEntityRenderState state, MatrixStack matrices,
                               VertexConsumerProvider vcp, int light, CallbackInfo ci) {
        if (!CheatMod.customModels.isEnabled()) return;
        if (!"Crazy Rabbit".equals(CheatMod.customModels.getCurrentModelName())) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        // TODO: точная проверка self/friend по имени из state требует доступа
        // к полю profileName — для упрощения сейчас рисуем для всех игроков
        // если модуль включён (можно ограничить позже через FriendStorage-аналог).

        if (rabbitModel == null) {
            EntityModelLoader loader = mc.getEntityModelLoader();
            rabbitModel = new RabbitEntityModel(loader.getModelPart(RabbitEntityModel.LAYER));
        }

        matrices.push();
        matrices.scale(1.25f, 1.25f, 1.25f);
        matrices.translate(0.0, -0.3, 0.0);

        rabbitModel.setAngles(state);

        var vc = vcp.getBuffer(net.minecraft.client.render.RenderLayer.getEntityCutout(
                net.minecraft.util.Identifier.of("yourcheat", "textures/models/rabbit.png")));

        rabbitModel.render(matrices, vc, light, net.minecraft.client.render.OverlayTexture.DEFAULT_UV, 0xFFFFFFFF);

        matrices.pop();
    }
}

