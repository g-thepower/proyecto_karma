package com.jinyami.karma;

import com.jinyami.karma.network.KarmaNetworking;
import com.jinyami.karma.server.KarmaEvents;
import com.jinyami.karma.server.KarmaServer;
import com.jinyami.karma.sound.KarmaSounds;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Karma implements ModInitializer {
	public static final String MOD_ID = "karma";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// Identificadores de paquetes
	public static final Identifier TOGGLE_KARMA_PACKET = new Identifier(MOD_ID, "toggle_karma"); // C2S: tecla G
	public static final Identifier ABSORB_PACKET = new Identifier(MOD_ID, "absorb_packet");     // C2S: tecla V
	public static final Identifier SYNC_PACKET = new Identifier(MOD_ID, "karma_sync");          // S2C: estado

	@Override
	public void onInitialize() {
		LOGGER.info("Inicializando el mod de Karma (Boruto)...");

		// Los sonidos DEBEN registrarse antes de usarse.
		KarmaSounds.registerSounds();

		// Receptores C2S (G y V) y sincronización S2C
		KarmaNetworking.registerReceivers();

		// Limpieza de estado y sincronización en join / respawn / tracking
		KarmaEvents.register();

		// Lógica de ticks del servidor (buffs, absorción)
		KarmaServer.registerServerTick();
	}
}
