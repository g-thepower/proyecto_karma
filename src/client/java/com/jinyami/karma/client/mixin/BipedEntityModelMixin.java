package com.jinyami.karma.client.mixin;

import com.jinyami.karma.client.state.KarmaClientState;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public class BipedEntityModelMixin {

    @Shadow public ModelPart rightArm;
    @Shadow public ModelPart leftArm;

    @Inject(method = "setAngles", at = @At("TAIL"))
    private void applyKarmaPose(LivingEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        // La pose depende del estado DE ESA entidad (por su id), no del jugador local:
        // así cada jugador con Karma activo y absorbiendo levanta los brazos, y solo él.
        if (entity instanceof PlayerEntity && KarmaClientState.isPosing(entity.getId())) {

            // Brazo derecho: recto y firme al frente (yaw 0 = exactamente en la dirección del cuerpo,
            // que es hacia donde apunta el eje del embudo en KarmaFx)
            this.rightArm.pitch = -1.5707963f;
            this.rightArm.yaw = 0.0f;
            this.rightArm.roll = 0.0f;

            // Brazo izquierdo: elevado y cruzado en el pecho
            this.leftArm.pitch = -1.7f;
            this.leftArm.yaw = 1.0f;
            this.leftArm.roll = -0.3f;
        }
    }
}
