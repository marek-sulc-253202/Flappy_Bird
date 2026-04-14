package com.example.flappy_bird;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Obstacle {
    private int x; // Pozice trubky na ose X (jak je daleko vpravo).
    private int screenHeight; // Výška obrazovky.
    private int width; // Šířka trubky.
    private int gap; // Velikost mezery, kterou ptáček prolétá.
    private int speed; // Rychlost, jakou se trubka hýbe doleva.
    private int topPipeHeight; // Výška horní trubky.
    private Paint paint; // Barva trubky.

    public Obstacle(int startX, int screenHeight) {
        this.x = startX; // Startovní pozice napravo.
        this.screenHeight = screenHeight;
        this.width = 150; // Šířka trubky.
        this.gap = 450;   // Mezera pro průlet.
        this.speed = 10;  // Rychlost pohybu trubky.

        paint = new Paint();
        paint.setColor(Color.GREEN); // Nastavení barvy trubek na zelenou.

        // Pojištění, aby mezera byla vždy v hratelné výšce (ne u úplného okraje)
        int minHeight = 100;
        int maxHeight = screenHeight - gap - 100;
        this.topPipeHeight = (int) (Math.random() * (maxHeight - minHeight) + minHeight);
    }

    // Metoda pro posun trubky doleva.
    public void moveToLeft() {
        x -= speed;
    }

    public void draw(Canvas canvas) {
        // Vykreslení horní trubky (odshora dolů k mezeře).
        canvas.drawRect(x, 0, x + width, topPipeHeight, paint);
        // Vykreslení spodní trubky (od konce mezery až dolů).
        canvas.drawRect(x, topPipeHeight + gap, x + width, screenHeight, paint);
    }

    // Metoda pro detekci kolize
    public boolean isColliding(Bird bird) {
        // Kontrola, zda je ptáček horizontálně v úrovni trubky
        if (bird.getX() + bird.getRadius() > x && bird.getX() - bird.getRadius() < x + width) {
            // Kontrola kolize s horní nebo spodní trubkou
            if (bird.getY() - bird.getRadius() < topPipeHeight || 
                bird.getY() + bird.getRadius() > topPipeHeight + gap) {
                return true;
            }
        }
        return false;
    }

    public int getX() { return x; }
    public int getWidth() { return width; }
}
