package com.example.flappy_bird;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * Třída reprezentující herní entitu ptáčka.
 * Obsahuje logiku pohybu, fyzikální vlastnosti a stavové proměnné.
 */
public class Bird {

    // Pozice ptáčka na obrazovce v souřadnicovém systému X a Y.
    private int x, y;
    
    // Aktuální vertikální rychlost (kladná hodnota = pád, záporná = stoupání).
    private int velocity;
    
    // Konstantní síla přitahující ptáčka k zemi v každém snímku.
    private final int gravity;
    
    // Poloměr kruhu reprezentujícího velikost ptáčka (použito pro kolize a vykreslování).
    private final int radius;
    
    // Síla skoku aplikovaná při interakci uživatele (nastavuje zápornou rychlost).
    private final int jumpPower;

    // Pomocný objekt pro vykreslování základního tvaru (pokud není k dispozici bitmapa).
    private final Paint paint;

    /**
     * Konstruktor inicializující startovní pozici a fyzikální parametry.
     * 
     * @param startX Počáteční horizontální pozice.
     * @param startY Počáteční vertikální pozice.
     */
    public Bird(int startX, int startY) {
        this.x = startX;
        this.y = startY;
        this.velocity = 0;
        this.radius = 60; // Definovaná velikost entity.
        this.gravity = 2; // Nastavení plynulosti pádu.
        this.jumpPower = -30; // Intenzita výskoku nahoru.

        paint = new Paint();
        paint.setColor(Color.RED);
    }

    /**
     * Aplikuje gravitační zrychlení na aktuální rychlost a aktualizuje vertikální pozici.
     */
    public void applyPhysics() {
        velocity += gravity;
        y += velocity;
    }

    /**
     * Základní vykreslovací metoda pro zobrazení entity na plátno.
     * V moderních verzích rendereru je tato metoda nahrazena vykreslováním bitmap.
     */
    public void draw(Canvas canvas) {
        canvas.drawCircle((float) x, (float) y, (float) radius, paint);
    }

    /**
     * Okamžitě nastaví vertikální rychlost na hodnotu jumpPower, čímž vyvolá pohyb vzhůru.
     */
    public void jump() {
        velocity = jumpPower;
    }

    // Gettery pro přístup k vlastnostem entity z externích tříd (Renderer, Collision detection).
    public int getX() { return x; }
    public int getY() { return y; }
    public int getRadius() { return radius; }
    public int getVelocity() { return velocity; }
    
    /**
     * Resetuje stav ptáčka do výchozích hodnot pro novou hru.
     * 
     * @param startY Nová počáteční vertikální pozice (zpravidla střed obrazovky).
     */
    public void reset(int startY) {
        this.y = startY;
        this.velocity = 0;
    }
}
