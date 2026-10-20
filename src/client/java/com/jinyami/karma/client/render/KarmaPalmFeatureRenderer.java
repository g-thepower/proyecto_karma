package com.jinyami.karma.client.render;

import com.jinyami.karma.Karma;
import com.jinyami.karma.client.state.KarmaClientState;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class KarmaPalmFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    private static final Identifier KARMA_TEXTURE = new Identifier(Karma.MOD_ID, "textures/entity/karma_palm.png");

    public KarmaPalmFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {

        // 1. Progreso de la animación de ESTE jugador (0.0 = inactivo, 1.0 = cuerpo 100% activo)
        float progress = KarmaClientState.getProgress(player.getId(), tickDelta);

        // La opacidad de la palma es la inversa de la del cuerpo
        float palmAlphaFactor = 1.0f - progress;

        // Si la activación del cuerpo es total (1.0), ocultamos por completo la palma
        if (palmAlphaFactor <= 0.0f) {
            return;
        }

        matrices.push();

        // 2. Nos alineamos con el brazo derecho del jugador
        this.getContextModel().rightArm.rotate(matrices);

        // Comprobamos si el modelo del jugador es "slim" (Alex) o normal (Steve)
        boolean isSlim = "slim".equals(player.getModel());

        // 3. Offsets independientes (intactos)
        float xOffset = isSlim ? -0.02F : -0.06F;
        float yOffset = 0.63F;
        float zOffset = 0F;

        matrices.translate(xOffset, yOffset, zOffset);

        // Rotación para que quede plano contra la palma
        matrices.multiply(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90), 1.0f, 0.0f, 0.0f)));

        // 4. Escala pequeña para la mano
        matrices.scale(0.10f, 0.10f, 0.10f);

        // 5. Animación de parpadeo original + transición de transparencia
        double time = player.getWorld().getTime() + tickDelta;
        float pulse = (float) ((Math.sin(time * 0.15D) + 1.0D) / 2.0D);

        int colorValue = (int) (pulse * 255);
        int alpha = (int) (palmAlphaFactor * 255);

        // 6. Plano texturizado con el color dinámico y el alpha de desvanecimiento
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(KARMA_TEXTURE));
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float size = 0.5f;

        vertexConsumer.vertex(matrix, -size, -size, 0.0f).color(colorValue, colorValue, colorValue, alpha).texture(0.0f, 1.0f).overlay(0, 10).light(light).normal(0.0f, 1.0f, 0.0f).next();
        vertexConsumer.vertex(matrix,  size, -size, 0.0f).color(colorValue, colorValue, colorValue, alpha).texture(1.0f, 1.0f).overlay(0, 10).light(light).normal(0.0f, 1.0f, 0.0f).next();
        vertexConsumer.vertex(matrix,  size,  size, 0.0f).color(colorValue, colorValue, colorValue, alpha).texture(1.0f, 0.0f).overlay(0, 10).light(light).normal(0.0f, 1.0f, 0.0f).next();
        vertexConsumer.vertex(matrix, -size,  size, 0.0f).color(colorValue, colorValue, colorValue, alpha).texture(0.0f, 0.0f).overlay(0, 10).light(light).normal(0.0f, 1.0f, 0.0f).next();

        matrices.pop();
    }
}
