package com.jinyami.karma.network;

import com.jinyami.karma.Karma;
import com.jinyami.karma.state.KarmaState;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Red del lado servidor.
 *
 * C2S  toggle_karma : vacío                 -> el jugador pide alternar el Karma.
 * C2S  absorb_packet: boolean               -> el jugador pulsa/suelta la tecla de absorción.
 * S2C  karma_sync   : varint entityId, byte -> estado (bit0 activo, bit1 absorbiendo) de un jugador.
 *
 * El servidor decide siempre. Tras cada petición responde con karma_sync aunque la haya rechazado
 * (cooldown, muerto...), de modo que la predicción local del cliente se corrige sola.
 */
public class KarmaNetworking {

    public static void registerReceivers() {
        // Tecla G
        ServerPlayNetworking.registerGlobalReceiver(Karma.TOGGLE_KARMA_PACKET,
                (server, player, handler, buf, responseSender) -> server.execute(() -> {
                    KarmaState.toggleKarma(player);
                    broadcast(player);
                }));

        // Tecla V (el buffer se lee fuera de server.execute porque se libera al volver el handler)
        ServerPlayNetworking.registerGlobalReceiver(Karma.ABSORB_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    if (!buf.isReadable()) return;
                    boolean absorbing = buf.readBoolean();
                    server.execute(() -> {
                        KarmaState.setAbsorbing(player, absorbing);
                        broadcast(player);
                    });
                });
    }

    /** Envía el estado actual de {@code subject} a él mismo y a todos los que lo están viendo. */
    public static void broadcast(ServerPlayerEntity subject) {
        Set<ServerPlayerEntity> targets = new LinkedHashSet<>(PlayerLookup.tracking(subject));
        targets.add(subject);
        for (ServerPlayerEntity target : targets) {
            sendTo(target, subject);
        }
    }

    /** Envía el estado de {@code subject} a un único jugador. */
    public static void sendTo(ServerPlayerEntity target, ServerPlayerEntity subject) {
        if (!ServerPlayNetworking.canSend(target, Karma.SYNC_PACKET)) return; // cliente sin el mod

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(subject.getId());
        buf.writeByte(KarmaState.getFlags(subject));
        ServerPlayNetworking.send(target, Karma.SYNC_PACKET, buf);
    }
}
