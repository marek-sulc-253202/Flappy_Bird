package com.example.flappy_bird;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Bird {

    private int x, y; // Pozice ptáčka na obrazovce.
    private int velocity; // Rychlost ptáčka (stoupání nebo klesání).
    private int gravity; // Síla, která ptáčka táhne k zemi.
    private final int radius; // Velikost ptáčka (kolečka).
    private final Paint paint; // Barva ptáčka.
    private int jumpPower; // Síla, jakou ptáček vyletí nahoru při kliknutí.

    public Bird(int startX, int startY) {
        this.x = startX;
        this.y = startY;
        this.velocity = 0;
        this.radius = 50;
        this.gravity = 2; // Vráceno na původní hodnotu.
        this.jumpPower = -30; // Vráceno na původní hodnotu.

        paint = new Paint();
        paint.setColor(Color.RED);
    }

    public void applyPhysics() {
        velocity += gravity;
        y += velocity;
    }

    public void draw(Canvas canvas) {
        canvas.drawCircle((float) x, (float) y, (float) radius, paint);
    }

    public void jump() {
        velocity = jumpPower;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getRadius() { return radius; }
    
    public void reset(int startY) {
        this.y = startY;
        this.velocity = 0;
    }
}
