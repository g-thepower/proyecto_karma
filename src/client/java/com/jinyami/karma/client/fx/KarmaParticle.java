package com.jinyami.karma.client.fx;

import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Partícula propia del Karma. No usa velocidad: en cada tick recalcula su posición como una función
 * de su edad, siempre respecto a la mano ACTUAL de su dueño. Eso da una espiral perfecta que sigue a la
 * mano aunque el jugador se mueva, y como el juego interpola entre el tick anterior y el actual, se ve
 * fluida a cualquier FPS.
 *
 * Hay dos tipos:
 *  - Vórtice: nace en un anillo delante de la palma y gira hacia ella cada vez más rápido.
 *  - Núcleo : se queda fijo en la palma y pulsa (se usa para el orbe y su halo).
 */
public class KarmaParticle extends SpriteBillboardParticle {
    private static final double TAU = Math.PI * 2.0;

    /** Fracción del radio del embudo que se deja vacía en el centro (0 = llegan a la palma, 0.3 = hueco grande). */
    private static final float CENTER_HOLE = 0.15f;

    private final PlayerEntity owner;

    // Trayectoria (vórtice)
    private double theta0;
    private float radius0, depth0, turns, phase;

    // Aspecto
    private boolean core;
    private float scaleStart, scaleEnd, maxAlpha;
    private float r0, g0, b0, r1, g1, b1;

    public KarmaParticle(ClientWorld world, PlayerEntity owner, Sprite sprite) {
        super(world, owner.getX(), owner.getY(), owner.getZ());
        this.owner = owner;
        this.setSprite(sprite);
        this.collidesWithWorld = false;
        this.gravityStrength = 0.0f;
    }

    /**
     * @param theta0  ángulo inicial en el anillo (radianes)
     * @param radius0 radio inicial del anillo, en bloques
     * @param depth0  distancia inicial por delante de la palma
     * @param turns   vueltas completas que da hasta llegar a la palma
     * @param life    duración en ticks
     * @param phase   0..1: adelanta la partícula una fracción de tick para que las de un mismo tick no se amontonen
     */
    public KarmaParticle path(double theta0, float radius0, float depth0, float turns, int life, float phase) {
        this.theta0 = theta0;
        this.radius0 = radius0;
        this.depth0 = depth0;
        this.turns = turns;
        this.maxAge = Math.max(1, life);
        this.phase = phase;
        return this;
    }

    /** Partícula fija en la palma, con pulso. */
    public KarmaParticle asCore() {
        this.core = true;
        return this;
    }

    public KarmaParticle look(float scaleStart, float scaleEnd, float alpha, int colorFrom, int colorTo) {
        this.scaleStart = scaleStart;
        this.scaleEnd = scaleEnd;
        this.maxAlpha = alpha;
        this.r0 = ((colorFrom >> 16) & 0xFF) / 255.0f;
        this.g0 = ((colorFrom >> 8) & 0xFF) / 255.0f;
        this.b0 = (colorFrom & 0xFF) / 255.0f;
        this.r1 = ((colorTo >> 16) & 0xFF) / 255.0f;
        this.g1 = ((colorTo >> 8) & 0xFF) / 255.0f;
        this.b1 = (colorTo & 0xFF) / 255.0f;
        return this;
    }

    /** Coloca la partícula en su posición inicial. Llamar una vez, justo antes de añadirla al ParticleManager. */
    public KarmaParticle start() {
        transform();
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;
        return this;
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;

        if (this.owner.isRemoved() || this.age++ >= this.maxAge) {
            this.markDead();
            return;
        }
        transform();
    }

    private void transform() {
        float t = Math.min(1.0f, (this.age + this.phase) / (float) this.maxAge);
        KarmaFx.Frame frame = KarmaFx.frame(this.owner);
        Vec3d pos;

        if (this.core) {
            pos = frame.palm();
            float pulse = 1.0f + 0.18f * MathHelper.sin((this.owner.getWorld().getTime() + this.age) * 0.9f);
            this.scale = this.scaleStart * pulse * frame.scale();
            this.alpha = this.maxAlpha;
            this.red = this.r0;
            this.green = this.g0;
            this.blue = this.b0;
        } else {
            // c crece despacio al principio y rápido al final: la energía "es succionada" hacia la palma.
            float c = (float) Math.pow(t, 1.7);
            // s es lo mismo pero sin llegar a 1: las partículas se desvanecen antes de alcanzar la palma
            // y dejan un pequeño hueco en el centro, así cualquier desajuste con la mano no se nota.
            float s = c * (1.0f - CENTER_HOLE);
            double radius = this.radius0 * (1.0f - s) * frame.scale();
            double depth = this.depth0 * (1.0f - s) * frame.scale();
            double angle = this.theta0 + this.turns * TAU * t;

            pos = frame.palm()
                    .add(frame.axis().multiply(depth))
                    .add(frame.right().multiply(Math.cos(angle) * radius))
                    .add(frame.up().multiply(Math.sin(angle) * radius));

            float fadeIn = Math.min(1.0f, t / 0.15f);
            float fadeOut = Math.min(1.0f, (1.0f - t) / 0.25f);
            this.alpha = this.maxAlpha * Math.max(0.0f, Math.min(fadeIn, fadeOut));
            this.scale = MathHelper.lerp(t, this.scaleStart, this.scaleEnd) * frame.scale();

            this.red = MathHelper.lerp(c, this.r0, this.r1);
            this.green = MathHelper.lerp(c, this.g0, this.g1);
            this.blue = MathHelper.lerp(c, this.b0, this.b1);
        }

        this.setPos(pos.x, pos.y, pos.z);
    }

    /** Brillo máximo: el efecto se ve igual de noche y en cuevas. */
    @Override
    protected int getBrightness(float tint) {
        return 0xF000F0;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }
}
