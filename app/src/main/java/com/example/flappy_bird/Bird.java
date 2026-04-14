package com.example.flappy_bird;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Bird {

    private int x, y; // Pozice ptáčka na obrazovce.
    private int velocity; // Rychlost ptáčka (stoupání nebo klesání).
    private int gravity; // Síla, která ptáčka táhne k zemi.
    private int radius; // Velikost ptáčka (kolečka).
    private Paint paint; // Barva ptáčka.
    private int jumpPower; // Síla, jakou ptáček vyletí nahoru při kliknutí.

    public Bird(int startX, int startY) {
        // Souřadnice zažínají vlevo nahoře.
        this.x = startX;    // X souřadnice.
        this.y = startY;    // Y souřadnice.
        this.velocity = 0;  // Ze začátku je rychlost 0 (stojí na místě).
        this.radius = 50;   // Velikost kolečka (Bird).
        this.gravity = 2;   // Gravitační zrychlení.
        this.jumpPower = -30;   // Zrychlení při skoku.

        // Nastavení barvy kolečka (Bird).
        paint = new Paint();
        paint.setColor(Color.RED);
    }

    public void applyPhysics() {    // Metoda pro fyziku pohybu.
        velocity += gravity;
        y += velocity;
    }

    public void draw(Canvas canvas) {   // Vykreslovací metoda.
        canvas.drawCircle(x, y, radius, paint); // Vykreslení kruhu (Bird).
    }

    public void jump() {    // Metoda pro skok.
        velocity = jumpPower;
    }

    // Gettery pro detekci kolizí
    public int getX() { return x; }
    public int getY() { return y; }
    public int getRadius() { return radius; }
    
    // Resetování ptáčka po nárazu.
    public void reset(int startY) {
        this.y = startY;
        this.velocity = 0; // Vynulování padání po restartu.
    }
}
