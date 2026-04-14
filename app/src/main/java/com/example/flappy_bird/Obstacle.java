package com.example.flappy_bird;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Obstacle {
    private int x; // Pozice trubky na ose X (jak je daleko vpravo).
    private final int screenHeight; // Výška obrazovky.
    private final int width; // Šířka trubky.
    private final int gap; // Velikost mezery, kterou ptáček prolétá.
    private final int speed; // Rychlost, jakou se trubka hýbe doleva.
    private final int topPipeHeight; // Výška horní trubky.
    private final Paint paint; // Barva trubky.
    private boolean isPassed = false; // Příznak, jestli už ptáček tuhle trubku proletěl.

    public Obstacle(int startX, int screenHeight) {
        this.x = startX; // Startovní pozice napravo.
        this.screenHeight = screenHeight;
        this.width = 150; // Šířka trubky.
        this.gap = 450;   // Mezera pro průlet.
        this.speed = 10;  // Jak rychle se to na nás sype.

        paint = new Paint();
        paint.setColor(Color.GREEN); // Trubky budou zelené jako v originálu.

        // Výpočet výšky horní trubky tak, aby mezera nebyla u kraje.
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
        canvas.drawRect(x, (float) topPipeHeight + gap, x + width, screenHeight, paint);
    }

    // Funkce pro zjištění, jestli do nás ptáček vrazil.
    public boolean isColliding(Bird bird) {
        // Nejdřív zjistíme, jestli je ptáček mezi levou a pravou stranou trubky.
        if (bird.getX() + bird.getRadius() > x && bird.getX() - bird.getRadius() < x + width) {
            // Pak zkontrolujeme, jestli narazil do horní nebo spodní části.
            if (bird.getY() - bird.getRadius() < topPipeHeight || 
                bird.getY() + bird.getRadius() > topPipeHeight + gap) {
                return true; // Narazil!
            }
        }
        return false; // Proletěl v pohodě.
    }

    public int getX() { return x; }
    public int getWidth() { return width; }

    public boolean isPassed() { return isPassed; }
    public void setPassed(boolean passed) { isPassed = passed; }
}
