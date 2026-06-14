package com.yourcheat.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class SoundManager {

    private static final Identifier HIT_ID     = Identifier.of("yourcheat", "hit");
    private static final Identifier KILL_ID    = Identifier.of("yourcheat", "kill");
    private static final Identifier ENABLE_ID  = Identifier.of("yourcheat", "enable");
    private static final Identifier DISABLE_ID = Identifier.of("yourcheat", "disable");

    public static void playHit()     { play(HIT_ID,     0.6f); }
    public static void playKill()    { play(KILL_ID,    0.8f); }
    public static void playEnable()  { play(ENABLE_ID,  0.5f); }
    public static void playDisable() { play(DISABLE_ID, 0.5f); }

    private static void play(Identifier id, float volume) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getSoundManager() == null) return;
        mc.getSoundManager().play(
            PositionedSoundInstance.master(
                SoundEvent.of(id),
                1.0f,
                volume
            )
        );
    }
}

