package com.jinyami.karma.client.state;

import com.jinyami.karma.state.KarmaState;
import net.minecraft.client.world.ClientWorld;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Copia en el cliente del estado del Karma de CADA jugador, indexada por entityId.
 * Se alimenta del paquete karma_sync (servidor -> cliente). Solo se toca desde el hilo del cliente.
 *
 * Aquí viven también los progresos de animación (aparición y color) de cada jugador. Se avanzan UNA vez
 * por tick de cliente (no una vez por render), así la animación dura lo mismo con cualquier cantidad de
 * FPS o de jugadores a la vista, y se interpola con tickDelta para que se vea fluida.
 */
public class KarmaClientState {
    /** Avance por tick de la aparición de abajo a arriba (0 -> 1 en 10 ticks = 0,5 s). */
    private static final float APPEAR_SPEED = 0.1f;
    /** Avance por tick del cambio de blanco a negro, una vez completada la aparición. */
    private static final float COLOR_FORWARD_SPEED = 0.05f;
    /** Retroceso por tick del color al desactivar. */
    private static final float COLOR_BACK_SPEED = 0.1f;
    /** Ticks que se tolera que la entidad no exista en el mundo antes de olvidarla. */
    private static final int MISSING_GRACE_TICKS = 40;

    private static final Map<Integer, Entry> ENTRIES = new HashMap<>();

    private static final class Entry {
        boolean active;
        boolean absorbing;
        float progress, prevProgress;
        float colorProgress, prevColorProgress;
        int missingTicks;
    }

    // --- ENTRADA DE DATOS ---

    /** Aplica el estado recibido del servidor (bit0 = activo, bit1 = absorbiendo). */
    public static void apply(int entityId, int flags) {
        boolean active = (flags & KarmaState.FLAG_ACTIVE) != 0;
        boolean absorbing = (flags & KarmaState.FLAG_ABSORBING) != 0;

        Entry entry = ENTRIES.get(entityId);
        if (entry == null) {
            if (!active && !absorbing) return; // nada que guardar
            entry = new Entry();
            ENTRIES.put(entityId, entry);
        }
        entry.active = active;
        entry.absorbing = active && absorbing;
    }

    /** Predicción local al pulsar G: respuesta instantánea; el siguiente karma_sync la corrige si hace falta. */
    public static void toggleLocal(int selfId) {
        Entry entry = ENTRIES.computeIfAbsent(selfId, id -> new Entry());
        entry.active = !entry.active;
        if (!entry.active) entry.absorbing = false;
    }

    /** Absorción del jugador local según la tecla V (también es predicción local). */
    public static void setLocalAbsorbing(int selfId, boolean absorbing) {
        Entry entry = ENTRIES.get(selfId);
        if (entry != null) entry.absorbing = absorbing && entry.active;
    }

    public static void clear() {
        ENTRIES.clear();
    }

    // --- CONSULTAS ---
    public static boolean isActive(int entityId) {
        Entry entry = ENTRIES.get(entityId);
        return entry != null && entry.active;
    }

    public static boolean isAbsorbing(int entityId) {
        Entry entry = ENTRIES.get(entityId);
        return entry != null && entry.active && entry.absorbing;
    }

    /** Karma activo y absorbiendo: es cuando se aplica la pose de brazos. */
    public static boolean isPosing(int entityId) {
        return isAbsorbing(entityId);
    }

    /** Progreso de aparición (0 = apagado, 1 = cuerpo completo), interpolado. */
    public static float getProgress(int entityId, float tickDelta) {
        Entry entry = ENTRIES.get(entityId);
        if (entry == null) return 0.0f;
        return lerp(entry.prevProgress, entry.progress, tickDelta);
    }

    /** Progreso del color (0 = blanco, 1 = negro), interpolado. */
    public static float getColorProgress(int entityId, float tickDelta) {
        Entry entry = ENTRIES.get(entityId);
        if (entry == null) return 0.0f;
        return lerp(entry.prevColorProgress, entry.colorProgress, tickDelta);
    }

    // --- TICK (una vez por tick de cliente) ---
    public static void tick(ClientWorld world) {
        Iterator<Map.Entry<Integer, Entry>> it = ENTRIES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Entry> mapEntry = it.next();
            Entry e = mapEntry.getValue();

            e.prevProgress = e.progress;
            e.prevColorProgress = e.colorProgress;
            step(e);

            // Olvidar entidades que ya no existen (salieron de rango, desconexión, cambio de mundo).
            // Si vuelven, el servidor reenvía su estado en START_TRACKING.
            if (world.getEntityById(mapEntry.getKey()) == null) {
                if (++e.missingTicks > MISSING_GRACE_TICKS) {
                    it.remove();
                    continue;
                }
            } else {
                e.missingTicks = 0;
            }

            // Apagado del todo y sin animación pendiente: no hace falta conservarlo.
            if (!e.active && e.progress == 0.0f && e.colorProgress == 0.0f
                    && e.prevProgress == 0.0f && e.prevColorProgress == 0.0f) {
                it.remove();
            }
        }
    }

    private static void step(Entry e) {
        if (e.active) {
            // Primero la aparición de abajo a arriba; completada, el cambio gradual de blanco a negro.
            if (e.progress < 1.0f) {
                e.progress = Math.min(1.0f, e.progress + APPEAR_SPEED);
            } else if (e.colorProgress < 1.0f) {
                e.colorProgress = Math.min(1.0f, e.colorProgress + COLOR_FORWARD_SPEED);
            }
        } else {
            // Al desactivar se revierte primero el color y luego la aparición.
            if (e.colorProgress > 0.0f) {
                e.colorProgress = Math.max(0.0f, e.colorProgress - COLOR_BACK_SPEED);
            } else if (e.progress > 0.0f) {
                e.progress = Math.max(0.0f, e.progress - APPEAR_SPEED);
            }
        }
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }
}
