package com.jinyami.karma.state;

import com.jinyami.karma.sound.KarmaSounds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estado del Karma. SOLO SE USA EN EL SERVIDOR: es la única fuente de verdad.
 * El cliente guarda su propia copia en KarmaClientState, alimentada por el paquete karma_sync.
 */
public class KarmaState {
    /** Bits del paquete karma_sync. */
    public static final int FLAG_ACTIVE = 1;
    public static final int FLAG_ABSORBING = 2;

    /** Ticks mínimos entre dos toggles del mismo jugador (0,5 s). */
    public static final int TOGGLE_COOLDOWN_TICKS = 10;

    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> ABSORBING = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Integer> LAST_TOGGLE_TICK = new ConcurrentHashMap<>();

    // --- CONSULTAS ---
    public static boolean isKarmaActive(PlayerEntity player) {
        return ACTIVE.contains(player.getUuid());
    }

    /** Solo absorbe si el Karma está activo y la tecla está presionada. */
    public static boolean isAbsorbing(PlayerEntity player) {
        UUID uuid = player.getUuid();
        return ACTIVE.contains(uuid) && ABSORBING.contains(uuid);
    }

    public static int getFlags(PlayerEntity player) {
        int flags = 0;
        if (isKarmaActive(player)) flags |= FLAG_ACTIVE;
        if (isAbsorbing(player)) flags |= FLAG_ABSORBING;
        return flags;
    }

    private static boolean canUse(ServerPlayerEntity player) {
        return player.isAlive() && !player.isSpectator();
    }

    // --- KARMA (ON/OFF) ---

    /**
     * Alterna el Karma.
     *
     * @return true si el estado cambió; false si se ignoró (cooldown, muerto o espectador).
     */
    public static boolean toggleKarma(ServerPlayerEntity player) {
        if (!canUse(player)) return false;

        UUID uuid = player.getUuid();
        MinecraftServer server = player.getServer();
        if (server != null) {
            int now = server.getTicks();
            Integer last = LAST_TOGGLE_TICK.get(uuid);
            if (last != null && now - last < TOGGLE_COOLDOWN_TICKS) return false;
            LAST_TOGGLE_TICK.put(uuid, now);
        }

        if (ACTIVE.remove(uuid)) {
            // ACTIVO -> APAGADO
            ABSORBING.remove(uuid);
            player.getWorld().playSound(
                    null, player.getX(), player.getY(), player.getZ(),
                    KarmaSounds.KARMA_DEACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.0f
            );
        } else {
            // APAGADO -> ACTIVO
            ACTIVE.add(uuid);
            player.getWorld().playSound(
                    null, player.getX(), player.getY(), player.getZ(),
                    KarmaSounds.KARMA_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.0f
            );
        }
        return true;
    }

    // --- ABSORCIÓN (MANTENER TECLA) ---
    public static void setAbsorbing(ServerPlayerEntity player, boolean absorbing) {
        UUID uuid = player.getUuid();
        if (absorbing && ACTIVE.contains(uuid) && canUse(player)) {
            ABSORBING.add(uuid);
        } else {
            ABSORBING.remove(uuid);
        }
    }

    // --- LIMPIEZA ---

    /** Apaga todo el estado de un jugador (desconexión, muerte, respawn). */
    public static void reset(UUID uuid) {
        ACTIVE.remove(uuid);
        ABSORBING.remove(uuid);
        LAST_TOGGLE_TICK.remove(uuid);
    }

    /** Vacía todo el estado (inicio y parada del servidor, incluido el integrado en mundo individual). */
    public static void clearAll() {
        ACTIVE.clear();
        ABSORBING.clear();
        LAST_TOGGLE_TICK.clear();
    }
}
