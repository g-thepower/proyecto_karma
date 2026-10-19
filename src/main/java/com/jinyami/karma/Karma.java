package com.jinyami.karma;

import com.jinyami.karma.server.KarmaServer;
import com.jinyami.karma.state.KarmaState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Karma implements ModInitializer {
	public static final String MOD_ID = "karma";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// Identificadores de paquetes para la red
	public static final Identifier TOGGLE_KARMA_PACKET = new Identifier(MOD_ID, "toggle_karma");
	public static final Identifier ABSORB_PACKET = new Identifier(MOD_ID, "absorb_packet");

	@Override
	public void onInitialize() {
		LOGGER.info("Inicializando el mod de Karma (Boruto)...");

		// Registrar la lógica de ticks del servidor (Buffs y estado)
		KarmaServer.registerServerTick();

		// 1. Escuchar cuando el cliente pide alternar el Karma (Tecla G)
		ServerPlayNetworking.registerGlobalReceiver(TOGGLE_KARMA_PACKET, (server, player, handler, buf, responseSender) -> {
			server.execute(() -> {
				KarmaState.toggleKarma(player);
				LOGGER.info("Estado de Karma cambiado para el jugador: " + player.getName().getString());
			});
		});

		// 2. Escuchar el estado de absorción (Tecla V manteniéndose presionada)
		ServerPlayNetworking.registerGlobalReceiver(ABSORB_PACKET, (server, player, handler, buf, responseSender) -> {
			boolean isAbsorbing = buf.readBoolean();
			server.execute(() -> {
				KarmaState.setAbsorbing(player, isAbsorbing);

				// Sincronizar el estado de absorción con los demás jugadores cercanos
				// (Esto es útil si quieres que otros vean el vórtice a tu alrededor)
			});
		});
	}
}