package com.jinyami.karma.sound;

import com.jinyami.karma.Karma;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class KarmaSounds {
    public static final Identifier KARMA_ACTIVATE_ID = new Identifier(Karma.MOD_ID, "karma_activate");
    public static final SoundEvent KARMA_ACTIVATE = SoundEvent.of(KARMA_ACTIVATE_ID);

    public static final Identifier KARMA_DEACTIVATE_ID = new Identifier(Karma.MOD_ID, "karma_desactivate");
    public static final SoundEvent KARMA_DEACTIVATE = SoundEvent.of(KARMA_DEACTIVATE_ID);

    // --- SONIDOS DE ABSORCIÓN (INICIO, BUCLE Y FIN) ---
    public static final Identifier KARMA_ABSORB_START_ID = new Identifier(Karma.MOD_ID, "karma_absorb_start");
    public static final SoundEvent KARMA_ABSORB_START = SoundEvent.of(KARMA_ABSORB_START_ID);

    public static final Identifier KARMA_ABSORB_LOOP_ID = new Identifier(Karma.MOD_ID, "karma_absorb_loop");
    public static final SoundEvent KARMA_ABSORB_LOOP = SoundEvent.of(KARMA_ABSORB_LOOP_ID);

    public static final Identifier KARMA_ABSORB_END_ID = new Identifier(Karma.MOD_ID, "karma_absorb_end");
    public static final SoundEvent KARMA_ABSORB_END = SoundEvent.of(KARMA_ABSORB_END_ID);

    public static void registerSounds() {
        Registry.register(Registries.SOUND_EVENT, KARMA_ACTIVATE_ID, KARMA_ACTIVATE);
        Registry.register(Registries.SOUND_EVENT, KARMA_DEACTIVATE_ID, KARMA_DEACTIVATE);

        Registry.register(Registries.SOUND_EVENT, KARMA_ABSORB_START_ID, KARMA_ABSORB_START);
        Registry.register(Registries.SOUND_EVENT, KARMA_ABSORB_LOOP_ID, KARMA_ABSORB_LOOP);
        Registry.register(Registries.SOUND_EVENT, KARMA_ABSORB_END_ID, KARMA_ABSORB_END);

        Karma.LOGGER.info("Sonidos registrados para " + Karma.MOD_ID);
    }
}