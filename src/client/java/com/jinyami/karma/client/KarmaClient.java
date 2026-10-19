package com.jinyami.karma.client;

import com.jinyami.karma.Karma;
import com.jinyami.karma.client.render.KarmaBodyFeatureRenderer;
import com.jinyami.karma.client.render.KarmaPalmFeatureRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KarmaClient implements ClientModInitializer {
	public static KeyBinding karmaToggleKey;
	public static KeyBinding karmaAbsorbKey;
	private static boolean lastAbsorbState = false;
	public static boolean isClientAbsorbing = false;
	public static boolean isKarmaActive = false;

	@Override
	public void onInitializeClient() {
		// -----------------------------------------------------------
		// Registro del Feature Renderer para la Palma del Jugador
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
		// Tick de Cliente
		// -----------------------------------------------------------
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) return;

			// 1. Tecla G (Alternar Karma)
			while (karmaToggleKey.wasPressed()) {
				isKarmaActive = !isKarmaActive; // Cambia el estado local al pulsar G
				ClientPlayNetworking.send(Karma.TOGGLE_KARMA_PACKET, PacketByteBufs.create());
			}

			// 2. Tecla V (Absorber sin efectos visuales de partículas)
			boolean currentAbsorbState = karmaAbsorbKey.isPressed() && isKarmaActive;
			isClientAbsorbing = currentAbsorbState;

			// Enviar paquete al servidor si cambia el estado de la absorción
			if (currentAbsorbState != lastAbsorbState) {
				lastAbsorbState = currentAbsorbState;
				var buf = PacketByteBufs.create();
				buf.writeBoolean(currentAbsorbState);
				ClientPlayNetworking.send(Karma.ABSORB_PACKET, buf);
			}
		});
	}
}