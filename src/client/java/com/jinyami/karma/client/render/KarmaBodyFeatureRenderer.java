package com.jinyami.karma.client.render;

import com.jinyami.karma.Karma;
import com.jinyami.karma.state.KarmaState;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class KarmaBodyFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    // Textura para brazos estándar (Steve/Classic - 4px)
    private static final Identifier BODY_OVERLAY_STEVE = new Identifier(Karma.MOD_ID, "textures/entity/karma_phase1_overlay.png");

    // Textura para brazos delgados (Alex/Slim - 3px)
    private static final Identifier BODY_OVERLAY_SLIM = new Identifier(Karma.MOD_ID, "textures/entity/karma_phase1_overlay_slim.png");

    public KarmaBodyFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {

        // Actualizamos los estados de la animación y del color
        boolean isActive = KarmaState.isKarmaActive(player);
        KarmaAnimationManager.update(isActive);
        float progress = KarmaAnimationManager.getProgress();
        float colorProgress = KarmaAnimationManager.getColorProgress();

        // Si la animación está totalmente apagada, no hacemos nada
        if (progress <= 0.0f) {
            return;
        }

        // Selección dinámica de la textura según el modelo del jugador (Slim o Classic)
        boolean isSlim = player.getModel().equals("slim");
        Identifier currentTexture = isSlim ? BODY_OVERLAY_SLIM : BODY_OVERLAY_STEVE;

        matrices.push();

        // Escala ligera para evitar Z-fighting con la skin base
        matrices.scale(1.03F, 1.03F, 1.03F);

        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(currentTexture));
        PlayerEntityModel<AbstractClientPlayerEntity> model = this.getContextModel();
        int overlayCoord = LivingEntityRenderer.getOverlay(player, 0.0f);

        // --- ANIMACIÓN DE ABAJO A ARRIBA (Siempre empieza blanca) ---
        float legAlpha = getPartAlpha(progress, 0.0f, 0.4f);   // 1. Piernas (abajo)
        float bodyAlpha = getPartAlpha(progress, 0.3f, 0.7f);  // 2. Torso y brazos (centro)
        float headAlpha = getPartAlpha(progress, 0.6f, 1.0f);  // 3. Cabeza (arriba)

        // --- CAMBIO GRADUAL DE BLANCO A NEGRO ---
        // colorProgress va de 0.0 (blanco) a 1.0 (negro) solo después de terminar la aparición
        float colorFactor = 1.0f - colorProgress;

        // 1. Renderizamos las Piernas (Base / Abajo)
        if (legAlpha > 0.0f) {
            model.rightLeg.render(matrices, vertexConsumer, light, overlayCoord, colorFactor, colorFactor, colorFactor, legAlpha);
            model.leftLeg.render(matrices, vertexConsumer, light, overlayCoord, colorFactor, colorFactor, colorFactor, legAlpha);
        }

        // 2. Renderizamos el Torso y los Brazos (Centro)
        if (bodyAlpha > 0.0f) {
            model.body.render(matrices, vertexConsumer, light, overlayCoord, colorFactor, colorFactor, colorFactor, bodyAlpha);
            model.rightArm.render(matrices, vertexConsumer, light, overlayCoord, colorFactor, colorFactor, colorFactor, bodyAlpha);
            model.leftArm.render(matrices, vertexConsumer, light, overlayCoord, colorFactor, colorFactor, colorFactor, bodyAlpha);
        }

        // 3. Renderizamos la Cabeza y el Sombrero/Capa exterior (Arriba)
        if (headAlpha > 0.0f) {
            model.head.render(matrices, vertexConsumer, light, overlayCoord, colorFactor, colorFactor, colorFactor, headAlpha);
            model.hat.render(matrices, vertexConsumer, light, overlayCoord, colorFactor, colorFactor, colorFactor, headAlpha);
        }

        matrices.pop();
    }

    // Método auxiliar para calcular el desvanecimiento escalonado de cada parte
    private float getPartAlpha(float progress, float start, float end) {
        if (progress < start) return 0.0f;
        if (progress >= end) return 1.0f;
        return (progress - start) / (end - start);
    }
}