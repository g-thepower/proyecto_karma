package com.jinyami.karma.client.fx;

import com.jinyami.karma.client.state.KarmaClientState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.HashSet;
import java.util.Set;

/**
 * Efecto visual de la absorción del Karma. Todo ocurre en el cliente, a partir del estado sincronizado
 * (KarmaClientState), así que lo ven todos los jugadores y no genera ningún paquete.
 *
 * Aspecto (limpio y simple, en blanco y negro con un toque violeta):
 *  1. EMBUDO   : 3 brazos de energía que giran y son succionados hacia la palma.
 *  2. NÚCLEO   : un orbe blanco pulsante en la palma, con un halo oscuro.
 *  3. CHASQUIDO: un anillo que se cierra de golpe sobre la palma al empezar a absorber.
 *
 * Para cambiar los colores, tocar las constantes de abajo. Formato 0xRRGGBB.
 */
public final class KarmaFx {
    // --- COLORES ---
    private static final int DARK_OUT = 0xFFFFFF;   // energía oscura, en el borde del embudo 0B0412
    private static final int DARK_IN = 0xFFFFF;    // ...y cerca de la palma (violeta profundo 4A1D80)
    private static final int BRIGHT_OUT = 0xA6FFFD; // destellos claros, en el borde (lavanda B9A6FF)
    private static final int BRIGHT_IN = 0xFFFFFF;  // ...y cerca de la palma (blanco)
    private static final int HALO = 0x2A0A4A;       // halo del núcleo
    private static final int CORE = 0xFFFFFF;       // núcleo

    // --- FORMA ---
    private static final int ARMS = 3;               // brazos de la espiral
    private static final float FUNNEL_RADIUS = 1.7f; // radio de la boca del embudo
    private static final float FUNNEL_DEPTH = 0.9f;  // cuánto sobresale la boca por delante de la palma
    private static final float FUNNEL_TURNS = 0.85f; // vueltas hasta llegar a la palma

    /** Orbe blanco con halo en la palma. Desactivado: el centro del embudo queda libre. */
    private static final boolean SHOW_CORE = false;

    // --- DISTANCIAS ---
    private static final double NEAR_SQ = 24.0 * 24.0; // efecto completo
    private static final double FAR_SQ = 48.0 * 48.0;  // más allá, no se dibuja

    private static final double TAU = Math.PI * 2.0;

    /** Texto de diagnóstico en la barra de acciones. Poner a false cuando el efecto funcione. */
    private static final boolean DEBUG = false;

    // Sprite de partícula de vanilla (blob redondeado). Si no te gusta la forma, prueba generic_0, generic_3 o glint.
    private static final Identifier SPRITE_ID = new Identifier("generic_3");

    /** Jugadores que absorbían en el tick anterior, para detectar el instante en que empiezan. */
    private static final Set<Integer> ABSORBING_LAST_TICK = new HashSet<>();

    private KarmaFx() {
    }

    /** Sistema de referencia de la mano de un jugador. */
    record Frame(Vec3d palm, Vec3d axis, Vec3d right, Vec3d up, float scale) {
    }

    /**
     * Calcula dónde está la palma y hacia dónde apunta.
     * - Primera persona (jugador local): relativo a la cámara y con el efecto reducido para no tapar la vista.
     * - Tercera persona y otros jugadores: relativo al cuerpo, con el brazo extendido al frente.
     */
    static Frame frame(PlayerEntity p) {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean firstPerson = p == mc.player && mc.options.getPerspective().isFirstPerson();

        if (firstPerson) {
            double yaw = Math.toRadians(p.getYaw());
            Vec3d look = p.getRotationVector();
            Vec3d right = new Vec3d(-Math.cos(yaw), 0.0, -Math.sin(yaw));
            Vec3d up = right.crossProduct(look).normalize();
            Vec3d palm = p.getEyePos()
                    .add(look.multiply(0.95))
                    .add(right.multiply(0.30))
                    .subtract(up.multiply(0.28));
            return new Frame(palm, look, right, up, 0.55f);
        }

        double yaw = Math.toRadians(p.bodyYaw);
        Vec3d forward = new Vec3d(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3d right = new Vec3d(-forward.z, 0.0, forward.x);
        Vec3d palm = p.getPos()
                .add(right.multiply(0.30))
                .add(forward.multiply(0.70))
                .add(0.0, p.getHeight() * 0.76, 0.0); // altura del hombro (22/16 de bloque en pie)
        return new Frame(palm, forward, right, new Vec3d(0.0, 1.0, 0.0), 1.0f);
    }

    /** Llamar una vez por tick de cliente. */
    public static void tick(MinecraftClient client) {
        ClientWorld world = client.world;
        if (world == null || client.player == null) {
            ABSORBING_LAST_TICK.clear();
            return;
        }

        ParticlesMode mode = client.options.getParticles().getValue();

        // El atlas de partículas no está en el gestor de modelos (client.getSpriteAtlas no lo conoce):
        // se obtiene del TextureManager, donde lo registra el ParticleManager.
        Sprite sprite = null;
        if (client.getTextureManager().getTexture(SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE) instanceof SpriteAtlasTexture atlas) {
            sprite = atlas.getSprite(SPRITE_ID);
        }

        if (DEBUG) {
            int self = client.player.getId();
            client.inGameHud.setOverlayMessage(Text.literal(
                    "Karma activo=" + KarmaClientState.isActive(self)
                            + " absorbiendo=" + KarmaClientState.isAbsorbing(self)
                            + " sprite=" + (sprite != null)
                            + " particulas=" + client.particleManager.getDebugString()), false);
        }
        if (sprite == null) return;

        Set<Integer> absorbingNow = new HashSet<>();

        for (AbstractClientPlayerEntity player : world.getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) continue;

            int id = player.getId();
            if (!KarmaClientState.isAbsorbing(id)) continue;

            double distSq = client.player.squaredDistanceTo(player);
            if (distSq > FAR_SQ) continue;

            absorbingNow.add(id);
            boolean near = distSq < NEAR_SQ;

            // Brazos por tick según la distancia y el ajuste de partículas del jugador.
            int perArm = switch (mode) {
                case ALL -> near ? 3 : 1;
                case DECREASED -> 1;
                default -> 0;
            };

            if (mode != ParticlesMode.MINIMAL && !ABSORBING_LAST_TICK.contains(id)) {
                spawnSnap(client, world, player, sprite);
            }
            spawnFunnel(client, world, player, sprite, perArm);
            if (SHOW_CORE) spawnCore(client, world, player, sprite);
        }

        ABSORBING_LAST_TICK.clear();
        ABSORBING_LAST_TICK.addAll(absorbingNow);
    }

    public static void reset() {
        ABSORBING_LAST_TICK.clear();
    }

    // --- 1. EMBUDO ---
    private static void spawnFunnel(MinecraftClient client, ClientWorld world, PlayerEntity p, Sprite sprite, int perArm) {
        Random rnd = world.random;
        for (int arm = 0; arm < ARMS; arm++) {
            for (int i = 0; i < perArm; i++) {
                int life = 12 + rnd.nextInt(5);
                double theta = arm * TAU / ARMS + (rnd.nextFloat() - 0.5f) * 0.3f;
                float phase = rnd.nextFloat(); // reparte las partículas del mismo tick a lo largo del brazo

                KarmaParticle particle = new KarmaParticle(world, p, sprite)
                        .path(theta, FUNNEL_RADIUS, FUNNEL_DEPTH, FUNNEL_TURNS, life, phase);

                if (rnd.nextFloat() < 0.35f) {
                    particle.look(0.13f, 0.04f, 0.95f, BRIGHT_OUT, BRIGHT_IN); // destello claro
                } else {
                    particle.look(0.24f, 0.07f, 0.85f, DARK_OUT, DARK_IN);     // energía oscura
                }
                client.particleManager.addParticle(particle.start());
            }
        }
    }

    // --- 2. NÚCLEO ---
    private static void spawnCore(MinecraftClient client, ClientWorld world, PlayerEntity p, Sprite sprite) {
        // Halo oscuro, grande y translúcido, detrás...
        client.particleManager.addParticle(new KarmaParticle(world, p, sprite)
                .path(0, 0, 0, 0, 2, 0.0f).asCore()
                .look(0.55f, 0.55f, 0.45f, HALO, HALO).start());
        // ...y el orbe blanco encima.
        client.particleManager.addParticle(new KarmaParticle(world, p, sprite)
                .path(0, 0, 0, 0, 2, 0.0f).asCore()
                .look(0.26f, 0.26f, 1.0f, CORE, CORE).start());
    }

    // --- 3. CHASQUIDO INICIAL ---
    private static void spawnSnap(MinecraftClient client, ClientWorld world, PlayerEntity p, Sprite sprite) {
        int count = 24;
        for (int i = 0; i < count; i++) {
            KarmaParticle particle = new KarmaParticle(world, p, sprite)
                    .path(i * TAU / count, 2.4f, 0.4f, 0.35f, 8, 0.0f);
            if (i % 2 == 0) {
                particle.look(0.14f, 0.05f, 1.0f, BRIGHT_OUT, BRIGHT_IN);
            } else {
                particle.look(0.22f, 0.07f, 0.9f, DARK_OUT, DARK_IN);
            }
            client.particleManager.addParticle(particle.start());
        }
    }
}
