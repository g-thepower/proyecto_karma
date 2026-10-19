package com.jinyami.karma.sound;

import com.jinyami.karma.Karma;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class KarmaSounds {
    public static final Identifier KARMA_ACTIVATE_ID = new Identifier(Karma.MOD_ID, "karma_activate");
    public static SoundEvent KARMA_ACTIVATE = SoundEvent.of(KARMA_ACTIVATE_ID);

    public static final Identifier KARMA_DESACTIVATE_ID = new Identifier(Karma.MOD_ID, "karma_desactivate");
    public static SoundEvent KARMA_DESACTIVATE = SoundEvent.of(KARMA_DESACTIVATE_ID);

    public static void registerSounds() {
        Registry.register(Registries.SOUND_EVENT, KARMA_ACTIVATE_ID, KARMA_ACTIVATE);
        Registry.register(Registries.SOUND_EVENT, KARMA_DESACTIVATE_ID, KARMA_DESACTIVATE);

        Karma.LOGGER.info("Registrando sonidos para " + Karma.MOD_ID);
    }
}