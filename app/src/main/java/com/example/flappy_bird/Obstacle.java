package com.example.flappy_bird;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Obstacle {
    private int x; // Pozice trubky na ose X.
    private final int screenHeight; // Výška obrazovky.
    private final int width; // Šířka trubky.
    private final int gap; // Velikost mezery.
    private final int speed; // Rychlost pohybu.
    private final int topPipeHeight; // Výška horní trubky.
    private final Paint paint; // Barva trubky.
    private final Paint capPaint; // Barva pro zakončení trubky.
    private boolean isPassed = false; // Už jsme proletěli?

    public Obstacle(int startX, int screenHeight, int speed, int lastPipeHeight) {
        this.x = startX;
        this.screenHeight = screenHeight;
        this.width = 150;
        this.gap = 450;
        this.speed = speed;

        paint = new Paint();
        paint.setColor(Color.GREEN);
        
        capPaint = new Paint();
        capPaint.setColor(Color.parseColor("#558022")); 
        capPaint.setStyle(Paint.Style.FILL);

        int minHeight = 150; 
        int maxHeight = screenHeight - gap - 150; 

        if (lastPipeHeight == -1) {
            // První trubka - úplně náhodná výška.
            this.topPipeHeight = (int) (Math.random() * (maxHeight - minHeight) + minHeight);
        } else {
            // Další trubky - omezení rozdílu oproti té minulé.
            int maxShift = 500; // Maximálně o kolik pixelů může být další mezera výš nebo níž.
            int preferredHeight = lastPipeHeight + (int) (Math.random() * (maxShift * 2) - maxShift);
            
            // Pojistka, aby mezera nevyjela z hratelné plochy.
            if (preferredHeight < minHeight) preferredHeight = minHeight;
            if (preferredHeight > maxHeight) preferredHeight = maxHeight;
            
            this.topPipeHeight = preferredHeight;
        }
    }

    public void moveToLeft() {
        x -= speed;
    }

    public void draw(Canvas canvas) {
        canvas.drawRect(x, 0, x + width, topPipeHeight, paint);
        canvas.drawRect(x, (float) topPipeHeight + gap, x + width, screenHeight, paint);
    }

    public boolean isColliding(Bird bird) {
        if (bird.getX() + bird.getRadius() > x && bird.getX() - bird.getRadius() < x + width) {
            if (bird.getY() - bird.getRadius() < topPipeHeight || 
                bird.getY() + bird.getRadius() > topPipeHeight + gap) {
                return true;
            }
        }
        return false;
    }

    public int getX() { return x; }
    public int getWidth() { return width; }
    public int getTopPipeHeight() { return topPipeHeight; }
    public int getGap() { return gap; }
    public boolean isPassed() { return isPassed; }
    public void setPassed(boolean passed) { isPassed = passed; }
}
