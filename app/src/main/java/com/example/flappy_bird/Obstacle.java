package com.example.flappy_bird;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * Třída reprezentující překážku v podobě horní a spodní trubky s mezerou.
 * Zajišťuje logiku pohybu, generování pozic a detekci kolizí s hráčem.
 */
public class Obstacle {
    // Horizontální pozice překážky na obrazovce.
    private int x;
    
    // Rozměry obrazovky a parametry překážky.
    private final int screenHeight;
    private final int width; // Šířka trubky.
    private final int gap; // Vertikální velikost mezery pro průlet.
    private final int speed; // Rychlost pohybu překážky směrem doleva.
    
    // Výška horní trubky (zároveň určuje počátek mezery).
    private final int topPipeHeight;
    
    // Pomocné objekty pro vykreslování základních barev (pro fallback kreslení).
    private final Paint paint;
    private final Paint capPaint;
    
    // Příznak označující, zda hráč touto překážkou již úspěšně proletěl.
    private boolean isPassed = false;

    /**
     * Konstruktor pro vytvoření nové překážky.
     * 
     * @param startX Počáteční horizontální pozice (obvykle pravý okraj obrazovky).
     * @param screenHeight Celková výška hrací plochy.
     * @param speed Rychlost posunu v pixelech na snímek.
     * @param lastPipeHeight Výška horní části předchozí překážky (pro plynulé navazování).
     */
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

        // Minimální a maximální výška horní trubky pro zachování hratelnosti.
        int minHeight = 150; 
        int maxHeight = screenHeight - gap - 150; 

        if (lastPipeHeight == -1) {
            // Generování náhodné výšky pro úplně první překážku ve hře.
            this.topPipeHeight = (int) (Math.random() * (maxHeight - minHeight) + minHeight);
        } else {
            // Generování výšky na základě předchozí překážky k omezení vertikálních skoků.
            int maxShift = 500; // Maximální povolená odchylka od předchozí výšky.
            int preferredHeight = lastPipeHeight + (int) (Math.random() * (maxShift * 2) - maxShift);
            
            // Omezení výsledné výšky na hratelné rozmezí obrazovky.
            if (preferredHeight < minHeight) preferredHeight = minHeight;
            if (preferredHeight > maxHeight) preferredHeight = maxHeight;
            
            this.topPipeHeight = preferredHeight;
        }
    }

    /**
     * Posune horizontální pozici překážky směrem doleva o nastavenou rychlost.
     */
    public void moveToLeft() {
        x -= speed;
    }

    /**
     * Základní vykreslovací metoda pro zobrazení trubek na plátno.
     */
    public void draw(Canvas canvas) {
        canvas.drawRect(x, 0, x + width, topPipeHeight, paint);
        canvas.drawRect(x, (float) topPipeHeight + gap, x + width, screenHeight, paint);
    }

    /**
     * Provádí kontrolu kolize mezi ptáčkem (kruhem) a obdélníky trubek.
     * 
     * @param bird Reference na objekt hráče pro získání jeho pozice a rozměrů.
     * @return True, pokud došlo k nárazu do některé z částí trubky.
     */
    public boolean isColliding(Bird bird) {
        // Kontrola, zda se ptáček nachází v horizontálním rozsahu překážky.
        if (bird.getX() + bird.getRadius() > x && bird.getX() - bird.getRadius() < x + width) {
            // Kontrola kolize s horní částí nebo spodní částí trubky.
            if (bird.getY() - bird.getRadius() < topPipeHeight || 
                bird.getY() + bird.getRadius() > topPipeHeight + gap) {
                return true;
            }
        }
        return false;
    }

    // Veřejné metody pro přístup k vlastnostem překážky (využíváno Rendererem a hrou).
    public int getX() { return x; }
    public int getWidth() { return width; }
    public int getTopPipeHeight() { return topPipeHeight; }
    public int getGap() { return gap; }
    public boolean isPassed() { return isPassed; }
    public void setPassed(boolean passed) { isPassed = passed; }
}
