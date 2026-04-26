package com.example.flappy_bird;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

/**
 * Třída pro správu a přehrávání zvukových efektů.
 * Využívá SoundPool pro nízko-latentní přehrávání krátkých zvukových souborů.
 */
public class SoundManager {
    private SoundPool soundPool;
    
    // Identifikátory načtených zvukových zdrojů.
    private final int soundJump, soundPoint, soundHit;
    
    // Identifikátory aktuálně hrajících streamů (umožňují zastavení konkrétního zvuku).
    private int jumpStreamId, pointStreamId; 
    
    // Globální stav ztlumení zvuků v aplikaci.
    private boolean isMuted = false;

    /**
     * Konstruktor inicializující SoundPool s parametry optimalizovanými pro mobilní hry.
     */
    public SoundManager(Context context) {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5) // Maximální počet souběžně hrajících zvuků.
                .setAudioAttributes(audioAttributes)
                .build();

        // Asynchronní načtení zvukových souborů ze zdrojů (res/raw).
        soundJump = soundPool.load(context, R.raw.jump, 1);
        soundPoint = soundPool.load(context, R.raw.point, 1);
        soundHit = soundPool.load(context, R.raw.hit, 1);
    }

    /**
     * Přehraje zvuk skoku, pokud není aplikace ztlumena.
     */
    public void playJump() {
        if (!isMuted) jumpStreamId = soundPool.play(soundJump, 1, 1, 0, 0, 1);
    }

    /**
     * Přehraje zvuk získání bodu, pokud není aplikace ztlumena.
     */
    public void playPoint() {
        if (!isMuted) pointStreamId = soundPool.play(soundPoint, 1, 1, 0, 0, 1);
    }

    /**
     * Přehraje zvuk nárazu s prioritou. Před spuštěním zastaví ostatní běžící efekty.
     */
    public void playHit() {
        stopOtherSounds(); // Eliminace zvukového smogu při kolizi.
        if (!isMuted) soundPool.play(soundHit, 1, 1, 1, 0, 1); 
    }

    /**
     * Okamžitě zastaví specifické hrající streamy (skok a bod).
     */
    public void stopOtherSounds() {
        if (soundPool != null) {
            soundPool.stop(jumpStreamId);
            soundPool.stop(pointStreamId);
        }
    }

    /**
     * Přepne stav ztlumení. Při aktivaci mute okamžitě zastaví hrající zvuky.
     */
    public void toggleMute() {
        isMuted = !isMuted;
        if (isMuted) stopOtherSounds();
    }

    public boolean isMuted() { return isMuted; }
    
    public void setMuted(boolean muted) { this.isMuted = muted; }

    /**
     * Uvolní zdroje SoundPoolu při ukončení aplikace nebo zobrazení.
     */
    public void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}
