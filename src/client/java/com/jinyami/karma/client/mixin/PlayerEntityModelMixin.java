package com.jinyami.karma.client.mixin;

import com.jinyami.karma.client.KarmaClient;
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
public class PlayerEntityModelMixin {

    @Shadow public ModelPart rightArm;
    @Shadow public ModelPart leftArm;

    @Inject(method = "setAngles", at = @At("TAIL"))
    private void applyKarmaPose(LivingEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        // La pose y el gesto solo ocurren si es un jugador, el Karma está activado
        // Y ADEMÁS está manteniendo presionada la tecla de absorción (V)
        if (entity instanceof PlayerEntity && KarmaClient.isKarmaActive && KarmaClient.isClientAbsorbing) {

            // Brazo derecho: Recto y firme al frente
            this.rightArm.pitch = -1.5707963f;
            this.rightArm.yaw = 0.2f;
            this.rightArm.roll = 0.0f;

            // Brazo izquierdo: Elevado y cruzado en el pecho (tus valores definitivos)
            this.leftArm.pitch = -1.7f;
            this.leftArm.yaw = 1.0f;
            this.leftArm.roll = -0.3f;
        }
    }
}