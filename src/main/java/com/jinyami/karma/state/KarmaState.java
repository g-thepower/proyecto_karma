package com.jinyami.karma.state;

import com.jinyami.karma.sound.KarmaSounds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KarmaState {
    // Mapas para almacenar el estado de cada jugador por su UUID
    private static final Map<UUID, Boolean> karmaActiveMap = new HashMap<>();
    private static final Map<UUID, Boolean> absorbingMap = new HashMap<>();

    // --- GESTIÓN DEL KARMA (ON/OFF) ---
    public static boolean isKarmaActive(PlayerEntity player) {
        return karmaActiveMap.getOrDefault(player.getUuid(), false);
    }

    public static void toggleKarma(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        boolean currentState = isKarmaActive(player);
        boolean newState = !currentState;
        karmaActiveMap.put(uuid, newState);

        // Si acabamos de ACTIVAR el Karma (antes estaba apagado y ahora encendido)
        if (newState) {
            player.getWorld().playSound(
                    null, // null para que lo escuchen todos los jugadores cercanos
                    player.getX(), player.getY(), player.getZ(),
                    KarmaSounds.KARMA_ACTIVATE,
                    SoundCategory.PLAYERS,
                    1.0f, // Volumen
                    1.0f  // Tono (Pitch)
            );
        }

        // Si DESACTIVAMOS el Karma (antes estaba activo y ahora se apaga)
        if (currentState) {
            absorbingMap.put(uuid, false);

            // ¡AQUÍ ESTABA FALTANDO EL SONIDO DE DESACTIVACIÓN!
            player.getWorld().playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    KarmaSounds.KARMA_DESACTIVATE,
                    SoundCategory.PLAYERS,
                    1.0f, // Volumen
                    1.0f  // Tono (Pitch)
            );
        }
    }

    // --- GESTIÓN DE LA ABSORCIÓN (MANTENER TECLA) ---
    public static boolean isAbsorbing(PlayerEntity player) {
        // Solo puede absorber si el Karma está activo y la tecla está presionada
        return isKarmaActive(player) && absorbingMap.getOrDefault(player.getUuid(), false);
    }

    public static void setAbsorbing(ServerPlayerEntity player, boolean absorbing) {
        if (isKarmaActive(player)) {
            absorbingMap.put(player.getUuid(), absorbing);
        } else {
            absorbingMap.put(player.getUuid(), false);
        }
    }
}