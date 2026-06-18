package com.yourcheat.model;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Crazy Rabbit — портирован из Forge 1.16.5 PlayerModel.java (rabbitBone иерархия)
 * на Fabric 1.21.4 ModelPart API. Геометрия 1:1 с оригиналом.
 */
public class RabbitEntityModel extends EntityModel<PlayerEntityRenderState> {

    public static final EntityModelLayer LAYER =
            new EntityModelLayer(Identifier.of("yourcheat", "rabbit"), "main");

    private final ModelPart bone;     // корень (rabbitBone)
    private final ModelPart head;     // rabbitHead
    private final ModelPart leftArm;  // rabbitLarm
    private final ModelPart rightArm; // rabbitRarm
    private final ModelPart leftLeg;  // rabbitLleg
    private final ModelPart rightLeg; // rabbitRleg

    public RabbitEntityModel(ModelPart root) {
        super(root);
        this.bone     = root.getChild("bone");
        this.head     = bone.getChild("head");
        this.leftArm  = bone.getChild("left_arm");
        this.rightArm = bone.getChild("right_arm");
        this.leftLeg  = bone.getChild("left_leg");
        this.rightLeg = bone.getChild("right_leg");
    }

    /** Описывает геометрию — вызывается один раз при регистрации слоя модели. */
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData bone = root.addChild("bone", ModelPartBuilder.create()
                .uv(28, 45).cuboid(-5.0F, -13.0F, -5.0F, 10, 11, 8),
                ModelTransform.pivot(0.0F, 24.0F, 0.0F));

        bone.addChild("right_leg", ModelPartBuilder.create()
                .cuboid(-2.0F, 0.0F, -2.0F, 4, 2, 4),
                ModelTransform.pivot(-3.0F, -2.0F, -1.0F));

        bone.addChild("left_arm", ModelPartBuilder.create()
                .cuboid(0.0F, 0.0F, -2.0F, 2, 8, 4),
                ModelTransform.of(5.0F, -13.0F, -1.0F, 0.0F, 0.0F, -0.0873F));

        bone.addChild("right_arm", ModelPartBuilder.create()
                .cuboid(-2.0F, 0.0F, -2.0F, 2, 8, 4),
                ModelTransform.of(-5.0F, -13.0F, -1.0F, 0.0F, 0.0F, 0.0873F));

        bone.addChild("left_leg", ModelPartBuilder.create()
                .cuboid(-2.0F, 0.0F, -2.0F, 4, 2, 4),
                ModelTransform.pivot(3.0F, -2.0F, -1.0F));

        ModelPartData head = bone.addChild("head", ModelPartBuilder.create()
                .uv(0, 0).cuboid(-3.0F, 0.0F, -4.0F, 6, 1, 6)
                .uv(56, 0).mirrored().cuboid(-5.0F, -9.0F, -5.0F, 2, 3, 2)
                .uv(56, 0).cuboid(3.0F, -9.0F, -5.0F, 2, 3, 2)
                .uv(0, 45).cuboid(-4.0F, -11.0F, -4.0F, 8, 11, 8)
                .uv(46, 0).cuboid(1.0F, -20.0F, 0.0F, 3, 9, 1)
                .uv(46, 0).cuboid(-4.0F, -20.0F, 0.0F, 3, 9, 1),
                ModelTransform.pivot(0.0F, -14.0F, -1.0F));

        return TexturedModelData.of(modelData, 64, 64);
    }

    /** Синхронизирует ротации с ванильным состоянием игрока (анимация ходьбы/взгляда). */
    @Override
    public void setAngles(PlayerEntityRenderState state) {
        this.head.pitch = state.pitch * (float)(Math.PI / 180);
        this.head.yaw   = state.relativeHeadYaw * (float)(Math.PI / 180);

        float limbSwing = state.limbSwingAnimationProgress;
        float speed      = state.limbSwingAmount;

        this.rightArm.pitch = (float) Math.cos(limbSwing * 0.6662F + Math.PI) * 2.0F * speed * 0.5F;
        this.leftArm.pitch  = (float) Math.cos(limbSwing * 0.6662F) * 2.0F * speed * 0.5F;
        this.rightLeg.pitch = (float) Math.cos(limbSwing * 0.6662F) * 1.4F * speed * 0.5F;
        this.leftLeg.pitch  = (float) Math.cos(limbSwing * 0.6662F + Math.PI) * 1.4F * speed * 0.5F;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer,
                        int light, int overlay, int color) {
        bone.render(matrices, vertexConsumer, light, overlay, color);
    }
}

