package com.jinyami.karma.client.render;

import com.jinyami.karma.Karma;
import com.jinyami.karma.client.state.KarmaClientState;
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

        // Estado y animación de ESTE jugador (sincronizados por karma_sync, avanzados en el tick de cliente).
        int id = player.getId();
        float progress = KarmaClientState.getProgress(id, tickDelta);
        float colorProgress = KarmaClientState.getColorProgress(id, tickDelta);

        // Si la animación está totalmente apagada, no hacemos nada
        if (progress <= 0.0f) {
            return;
        }

        // Selección dinámica de la textura según el modelo del jugador (Slim o Classic)
        boolean isSlim = "slim".equals(player.getModel());
        Identifier currentTexture = isSlim ? BODY_OVERLAY_SLIM : BODY_OVERLAY_STEVE;

        matrices.push();

        // Escala ligera para evitar Z-fighting con la skin base
        matrices.scale(1.01F, 1.01F, 1.03F);

        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(currentTexture));
        PlayerEntityModel<AbstractClientPlayerEntity> model = this.getContextModel();
        int overlayCoord = LivingEntityRenderer.getOverlay(player, 0.0f);

        // --- ANIMACIÓN DE ABAJO A ARRIBA (siempre empieza blanca) ---
        float legAlpha = getPartAlpha(progress, 0.0f, 0.4f);   // 1. Piernas (abajo)
        float bodyAlpha = getPartAlpha(progress, 0.3f, 0.7f);  // 2. Torso y brazos (centro)
        float headAlpha = getPartAlpha(progress, 0.6f, 1.0f);  // 3. Cabeza (arriba)

        // --- CAMBIO GRADUAL DE BLANCO A NEGRO ---
        float colorFactor = 1.0f - colorProgress;

        // 1. Piernas
        if (legAlpha > 0.0f) {
            model.rightLeg.render(matrices, vertexConsumer, light, overlayCoord, colorFactor, colorFactor, colorFactor, legAlpha);
            model.leftLeg.render(matrices, vertexConsumer, light, overlayCoord, colorFactor, colorFactor, colorFactor, legAlpha);
        }

        // 2. Torso y brazos
        if (bodyAlpha > 0.0f) {
            model.body.render(matrices, vertexConsumer, light, overlayCoord, colorFactor, colorFactor, colorFactor, bodyAlpha);
            model.rightArm.render(matrices, vertexConsumer, light, overlayCoord, colorFactor, colorFactor, colorFactor, bodyAlpha);
            model.leftArm.render(matrices, vertexConsumer, light, overlayCoord, colorFactor, colorFactor, colorFactor, bodyAlpha);
        }

        // 3. Cabeza y sombrero/capa exterior
        if (headAlpha > 0.0f) {
            model.head.render(matrices, vertexConsumer, light, overlayCoord, colorFactor, colorFactor, colorFactor, headAlpha);
            model.hat.render(matrices, vertexConsumer, light, overlayCoord, colorFactor, colorFactor, colorFactor, headAlpha);
        }

        matrices.pop();
    }

    // Desvanecimiento escalonado de cada parte
    private float getPartAlpha(float progress, float start, float end) {
        if (progress < start) return 0.0f;
        if (progress >= end) return 1.0f;
        return (progress - start) / (end - start);
    }
}
