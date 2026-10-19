package com.jinyami.karma.server;

import com.jinyami.karma.state.KarmaState;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class KarmaServer {

    // --- SONIDO DE BUCLE PARA LA ABSORCIÓN ---
    private static final SoundEvent LOOP_SOUND = SoundEvents.BLOCK_BEACON_AMBIENT;

    // --- PARTÍCULA DE POLVO AMARILLO BRILLANTE / DORADO ---
    private static final DustParticleEffect BRIGHT_YELLOW_DUST = new DustParticleEffect(
            new Vector3f(1.0f, 0.85f, 0.1f), 0.7f
    );

    private static void playAbsorbSound(World world, ServerPlayerEntity player) {
        world.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_FIRE_EXTINGUISH,
                SoundCategory.PLAYERS,
                0.6f,
                1.2f
        );
    }

    public static void registerServerTick() {
        // --- 1. BLOQUEO ABSOLUTO DE DAÑO ESPECIAL ---
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                if (KarmaState.isAbsorbing(player)) {
                    if (source.isOf(DamageTypes.SONIC_BOOM) ||
                            source.isOf(DamageTypes.LIGHTNING_BOLT) ||
                            source.isOf(DamageTypes.INDIRECT_MAGIC) ||
                            source.isOf(DamageTypes.MAGIC) ||
                            source.isOf(DamageTypes.EXPLOSION) ||
                            source.isOf(DamageTypes.PLAYER_EXPLOSION) ||
                            source.isOf(DamageTypes.FIREWORKS) ||
                            source.isOf(DamageTypes.IN_FIRE) ||
                            source.isOf(DamageTypes.ON_FIRE)) {

                        if (player.getWorld() instanceof ServerWorld serverWorld) {
                            serverWorld.spawnParticles(
                                    net.minecraft.particle.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                                    player.getX(), player.getY() + 1.0, player.getZ(),
                                    8, 0.3, 0.3, 0.3, 0.02
                            );
                        }
                        playAbsorbSound(player.getWorld(), player);
                        return false;
                    }
                }
            }
            return true;
        });

        // --- 2. BUCLE DE TICK DEL SERVIDOR ---
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

                // Buffs pasivos cuando el Karma está activo (G)
                if (KarmaState.isKarmaActive(player)) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 40, 2, true, false, true));
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 2, true, false, true));
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 40, 1, true, false, true));
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 2, true, false, true));
                }

                // Absorción en área (V)
                if (KarmaState.isAbsorbing(player)) {
                    World world = player.getWorld();

                    if (world.getTime() % 20 == 0) {
                        world.playSound(
                                null,
                                player.getX(), player.getY(), player.getZ(),
                                LOOP_SOUND,
                                SoundCategory.PLAYERS,
                                0.5f,
                                2f
                        );
                    }

                    // --- EFECTO VISUAL: EMBUDO / VÓRTICE EN ESPIRAL CONCÉNTRICA ---
                    if (world instanceof ServerWorld serverWorld) {
                        float bodyYawRad = (float) Math.toRadians(player.getBodyYaw());
                        double forwardX = -Math.sin(bodyYawRad);
                        double forwardZ = Math.cos(bodyYawRad);

                        Vec3d forward = new Vec3d(forwardX, 0, forwardZ);
                        Vec3d right = new Vec3d(-forward.z, 0, forward.x).normalize();

                        // Centro exacto de la palma
                        double handX = player.getX() + (right.x * 0.35) + (forward.x * 0.75);
                        double handY = player.getY() + 1.15;
                        double handZ = player.getZ() + (right.z * 0.35) + (forward.z * 0.75);

                        double rotationSpeed = world.getTime() * 0.4;
                        int spiralArms = 3; // 3 hilos de energía girando en espiral
                        int pointsPerArm = 6; // Puntos a lo largo de cada hilo

                        for (int arm = 0; arm < spiralArms; arm++) {
                            double armOffset = arm * (Math.PI * 2 / spiralArms);

                            for (int step = 0; step < pointsPerArm; step++) {
                                double progress = (double) step / pointsPerArm; // 0.0 (afuera) a 1.0 (palma)
                                double currentRadius = 0.45 * (1.0 - progress); // Se estrecha hacia la palma
                                double angle = rotationSpeed + armOffset + (progress * Math.PI * 2);

                                // Desplazamiento frontal para crear forma de coto/embudo hacia la mano
                                double depthOffset = (1.0 - progress) * 0.2;

                                double px = handX + (Math.cos(angle) * currentRadius * right.x) + (forward.x * depthOffset);
                                double py = handY + (Math.sin(angle) * currentRadius);
                                double pz = handZ + (Math.cos(angle) * currentRadius * right.z) + (forward.z * depthOffset);

                                serverWorld.spawnParticles(
                                        BRIGHT_YELLOW_DUST,
                                        px, py, pz,
                                        1, 0.0, 0.0, 0.0, 0.0
                                );
                            }
                        }
                    }

                    double radius = 1.5;
                    Box boundingBox = player.getBoundingBox().expand(radius);

                    // A) Proyectiles
                    List<ProjectileEntity> projectiles = world.getEntitiesByClass(
                            ProjectileEntity.class, boundingBox, p -> !p.isRemoved()
                    );
                    boolean absorbedProjectile = false;

                    for (ProjectileEntity projectile : projectiles) {
                        if (projectile.getOwner() == player) continue;

                        double px = projectile.getX();
                        double py = projectile.getY();
                        double pz = projectile.getZ();

                        projectile.discard();
                        absorbedProjectile = true;

                        if (world instanceof ServerWorld serverWorld) {
                            serverWorld.spawnParticles(
                                    net.minecraft.particle.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                                    px, py, pz,
                                    8, 0.1, 0.1, 0.1, 0.02
                            );
                        }
                    }

                    if (absorbedProjectile) {
                        playAbsorbSound(world, player);
                    }

                    // B) Nubes mágicas
                    List<AreaEffectCloudEntity> clouds = world.getEntitiesByClass(
                            AreaEffectCloudEntity.class, boundingBox, c -> !c.isRemoved()
                    );
                    boolean absorbedCloud = false;
                    for (AreaEffectCloudEntity cloud : clouds) {
                        double cx = cloud.getX();
                        double cy = cloud.getY();
                        double cz = cloud.getZ();

                        cloud.discard();
                        absorbedCloud = true;

                        if (world instanceof ServerWorld serverWorld) {
                            serverWorld.spawnParticles(
                                    net.minecraft.particle.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                                    cx, cy, cz,
                                    12, 0.4, 0.2, 0.4, 0.03
                            );
                        }
                    }
                    if (absorbedCloud) {
                        playAbsorbSound(world, player);
                    }

                    // C) Desintegrar Creepers a punto de explotar
                    List<CreeperEntity> creepers = world.getEntitiesByClass(
                            CreeperEntity.class, boundingBox, c -> !c.isRemoved()
                    );
                    boolean absorbedCreeper = false;
                    for (CreeperEntity creeper : creepers) {
                        if (creeper.getFuseSpeed() > 0) {
                            double cx = creeper.getX();
                            double cy = creeper.getY() + 0.5;
                            double cz = creeper.getZ();

                            creeper.discard();
                            absorbedCreeper = true;

                            if (world instanceof ServerWorld serverWorld) {
                                serverWorld.spawnParticles(
                                        net.minecraft.particle.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                                        cx, cy, cz,
                                        15, 0.3, 0.5, 0.3, 0.03
                                );
                            }
                        }
                    }
                    if (absorbedCreeper) {
                        playAbsorbSound(world, player);
                    }

                    // D) Extinguir fuego y apagar fogatas cercanas en área y altura ampliada
                    BlockPos playerPos = player.getBlockPos();
                    boolean extinguishedFire = false;
                    int fireRadius = 3;

                    for (int x = -fireRadius; x <= fireRadius; x++) {
                        for (int y = -3; y <= 4; y++) {
                            for (int z = -fireRadius; z <= fireRadius; z++) {
                                BlockPos pos = playerPos.add(x, y, z);
                                net.minecraft.block.BlockState blockState = world.getBlockState(pos);

                                // 1. Fuego normal o de alma (se elimina el bloque)
                                if (blockState.isOf(net.minecraft.block.Blocks.FIRE) ||
                                        blockState.isOf(net.minecraft.block.Blocks.SOUL_FIRE)) {

                                    world.removeBlock(pos, false);
                                    extinguishedFire = true;

                                    if (world instanceof ServerWorld serverWorld) {
                                        serverWorld.spawnParticles(
                                                net.minecraft.particle.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                                5, 0.2, 0.2, 0.2, 0.01
                                        );
                                    }
                                }
                                // 2. Fogatas (se apagan manteniendo la estructura)
                                else if ((blockState.isOf(net.minecraft.block.Blocks.CAMPFIRE) ||
                                        blockState.isOf(net.minecraft.block.Blocks.SOUL_CAMPFIRE)) &&
                                        blockState.get(net.minecraft.state.property.Properties.LIT)) {

                                    // Cambiar el estado lit a false para apagarlas
                                    world.setBlockState(pos, blockState.with(net.minecraft.state.property.Properties.LIT, false), 3);
                                    extinguishedFire = true;

                                    if (world instanceof ServerWorld serverWorld) {
                                        serverWorld.spawnParticles(
                                                net.minecraft.particle.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                                8, 0.2, 0.2, 0.2, 0.02
                                        );
                                    }
                                }
                            }
                        }
                    }

                    if (player.isOnFire()) {
                        player.extinguish();
                        extinguishedFire = true;

                        if (world instanceof ServerWorld serverWorld) {
                            serverWorld.spawnParticles(
                                    net.minecraft.particle.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                                    player.getX(), player.getY() + 1.0, player.getZ(),
                                    10, 0.3, 0.5, 0.3, 0.02
                            );
                        }
                    }
                    if (extinguishedFire) {
                        playAbsorbSound(world, player);
                    }

                    // E) Purificar efectos dañinos (Debuffs)
                    List<StatusEffectInstance> activeEffects = new ArrayList<>(player.getStatusEffects());
                    boolean purifiedEffect = false;
                    for (StatusEffectInstance effect : activeEffects) {
                        if (effect.getEffectType().getCategory() == StatusEffectCategory.HARMFUL) {
                            player.removeStatusEffect(effect.getEffectType());
                            purifiedEffect = true;
                        }
                    }
                    if (purifiedEffect) {
                        if (world instanceof ServerWorld serverWorld) {
                            serverWorld.spawnParticles(
                                    net.minecraft.particle.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                                    player.getX(), player.getY() + 1.0, player.getZ(),
                                    12, 0.3, 0.5, 0.3, 0.03
                            );
                        }
                        playAbsorbSound(world, player);
                    }
                }
            }
        });
    }
}