package com.jinyami.karma.server;

import com.jinyami.karma.network.KarmaNetworking;
import com.jinyami.karma.state.KarmaState;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Eventos que mantienen el estado limpio y el cliente sincronizado.
 * Sin esto, el estado estático sobrevive a desconexiones, muertes y cambios de mundo.
 */
public class KarmaEvents {

    public static void register() {
        // --- Ciclo de vida del servidor (también el integrado en mundo individual) ---
        ServerLifecycleEvents.SERVER_STARTING.register(server -> KarmaState.clearAll());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> KarmaState.clearAll());

        // --- Conexión ---
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                KarmaNetworking.sendTo(handler.getPlayer(), handler.getPlayer()));

        // Quien se desconecta con V pulsada no puede quedar "absorbiendo".
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                KarmaState.reset(handler.getPlayer().getUuid()));

        // --- Muerte y respawn: el Karma se apaga ---
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity player) {
                KarmaState.reset(player.getUuid());
                KarmaNetworking.broadcast(player);
            }
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            KarmaState.reset(newPlayer.getUuid());
            KarmaNetworking.broadcast(newPlayer);
        });

        // --- Cambio de dimensión: el cliente recrea su jugador, se reenvía el estado ---
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) ->
                KarmaNetworking.broadcast(player));

        // --- Un jugador empieza a ver a otro: recibe su estado actual ---
        EntityTrackingEvents.START_TRACKING.register((trackedEntity, viewer) -> {
            if (trackedEntity instanceof ServerPlayerEntity tracked && KarmaState.getFlags(tracked) != 0) {
                KarmaNetworking.sendTo(viewer, tracked);
            }
        });
    }
}
