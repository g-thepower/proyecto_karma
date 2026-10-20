package com.jinyami.karma.client;

import com.jinyami.karma.Karma;
import com.jinyami.karma.client.fx.KarmaFx;
import com.jinyami.karma.client.render.KarmaBodyFeatureRenderer;
import com.jinyami.karma.client.render.KarmaPalmFeatureRenderer;
import com.jinyami.karma.client.state.KarmaClientState;
import com.jinyami.karma.sound.KarmaSounds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundCategory;
import org.lwjgl.glfw.GLFW;

public class KarmaClient implements ClientModInitializer {
	public static KeyBinding karmaToggleKey;
	public static KeyBinding karmaAbsorbKey;

	/** Último valor de absorción enviado al servidor (solo se envía al cambiar). */
	private static boolean lastSentAbsorb = false;

	/** Instancia del sonido en bucle que se reproduce localmente mientras se mantiene presionada la V. */
	private static PositionedSoundInstance absorbLoopSound = null;

	@Override
	public void onInitializeClient() {
		// -----------------------------------------------------------
		// Feature Renderers (palma y cuerpo)
		// -----------------------------------------------------------
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
			if (entityRenderer instanceof PlayerEntityRenderer playerRenderer) {
				registrationHelper.register(new KarmaPalmFeatureRenderer(playerRenderer));
				registrationHelper.register(new KarmaBodyFeatureRenderer(playerRenderer));
			}
		});

		// -----------------------------------------------------------
		// Keybindings
		// -----------------------------------------------------------
		karmaToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.karma.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.karma.title"
		));

		karmaAbsorbKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.karma.absorb", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.karma.title"
		));

		// -----------------------------------------------------------
		// Servidor -> cliente: estado de Karma de cualquier jugador a la vista
		// -----------------------------------------------------------
		ClientPlayNetworking.registerGlobalReceiver(Karma.SYNC_PACKET,
				(client, handler, buf, responseSender) -> {
					int entityId = buf.readVarInt();
					int flags = buf.readByte() & 0xFF;
					client.execute(() -> KarmaClientState.apply(entityId, flags));
				});

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			KarmaClientState.clear();
			KarmaFx.reset();
			lastSentAbsorb = false;
			absorbLoopSound = null;
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			KarmaClientState.clear();
			KarmaFx.reset();
			lastSentAbsorb = false;
			absorbLoopSound = null;
		});

		// -----------------------------------------------------------
		// Tick de cliente
		// -----------------------------------------------------------
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null || client.world == null) return;
			if (client.isPaused()) return;

			int selfId = client.player.getId();

			// 1. Tecla G (alternar Karma)
			while (karmaToggleKey.wasPressed()) {
				KarmaClientState.toggleLocal(selfId);
				ClientPlayNetworking.send(Karma.TOGGLE_KARMA_PACKET, PacketByteBufs.create());
			}

			// 2. Tecla V (absorber): solo cuenta si el Karma está activo.
			boolean absorbing = karmaAbsorbKey.isPressed() && KarmaClientState.isActive(selfId);
			KarmaClientState.setLocalAbsorbing(selfId, absorbing);

			// --- DETECCIÓN DE CAMBIO EN LA TECLA V (INICIO, BUCLE Y FIN) ---
			if (absorbing != lastSentAbsorb) {
				if (absorbing) {
					// A) Sonido de ENTRADA (Start) instantáneo
					client.player.getWorld().playSound(
							client.player.getX(), client.player.getY(), client.player.getZ(),
							KarmaSounds.KARMA_ABSORB_START,
							SoundCategory.PLAYERS,
							0.8f, 1.0f, false
					);

					// B) Iniciar el sonido en BUCLE (Loop) de forma local
					if (absorbLoopSound == null || !client.getSoundManager().isPlaying(absorbLoopSound)) {
						absorbLoopSound = PositionedSoundInstance.master(
								KarmaSounds.KARMA_ABSORB_LOOP,
								1.0f, // Tono (Pitch)
								0.5f  // Volumen
						);
						client.getSoundManager().play(absorbLoopSound);
					}
				} else {
					// C) Sonido de SALIDA (End) instantáneo
					client.player.getWorld().playSound(
							client.player.getX(), client.player.getY(), client.player.getZ(),
							KarmaSounds.KARMA_ABSORB_END,
							SoundCategory.PLAYERS,
							0.8f, 1.0f, false
					);

					// D) CORTAR EL BUCLE AL INSTANTE
					if (absorbLoopSound != null) {
						client.getSoundManager().stop(absorbLoopSound);
						absorbLoopSound = null;
					}
				}

				lastSentAbsorb = absorbing;
				PacketByteBuf buf = PacketByteBufs.create();
				buf.writeBoolean(absorbing);
				ClientPlayNetworking.send(Karma.ABSORB_PACKET, buf);
			}

			// 3. Avanzar las animaciones de todos los jugadores
			KarmaClientState.tick(client.world);

			// 4. Efecto visual de la absorción
			KarmaFx.tick(client);
		});
	}
}