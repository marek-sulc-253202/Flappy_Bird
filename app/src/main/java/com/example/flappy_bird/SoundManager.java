package com.example.flappy_bird;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

public class SoundManager {
    private SoundPool soundPool;
    private final int soundJump, soundPoint, soundHit;
    private int jumpStreamId, pointStreamId; // Ukládáme si ID běžících zvuků pro možnost zastavení.
    private boolean isMuted = false;

    public SoundManager(Context context) {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();

        // Načtení zvuků.
        soundJump = soundPool.load(context, R.raw.jump, 1);
        soundPoint = soundPool.load(context, R.raw.point, 1);
        soundHit = soundPool.load(context, R.raw.hit, 1);
    }

    public void playJump() {
        if (!isMuted) jumpStreamId = soundPool.play(soundJump, 1, 1, 0, 0, 1);
    }

    public void playPoint() {
        if (!isMuted) pointStreamId = soundPool.play(soundPoint, 1, 1, 0, 0, 1);
    }

    public void playHit() {
        stopOtherSounds(); // Při nárazu nejdřív všechno ostatní utichne.
        if (!isMuted) soundPool.play(soundHit, 1, 1, 1, 0, 1); // Vyšší priorita pro náraz.
    }

    // Zastaví zvuky skoku a bodování (třeba při nárazu nebo mute).
    public void stopOtherSounds() {
        if (soundPool != null) {
            soundPool.stop(jumpStreamId);
            soundPool.stop(pointStreamId);
        }
    }

    public void toggleMute() {
        isMuted = !isMuted;
        if (isMuted) stopOtherSounds();
    }

    public boolean isMuted() { return isMuted; }
    public void setMuted(boolean muted) { this.isMuted = muted; }

    public void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}
