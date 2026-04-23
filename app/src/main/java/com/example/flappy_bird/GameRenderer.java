package com.example.flappy_bird;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.List;

public class GameRenderer {

    private final Paint textPaint; // Štětec pro veškeré nápisy a čísla.
    private final Paint uiPaint; // Štětec pro pozadí a UI prvky.
    private final int screenWidth, screenHeight; // Rozměry tvého displeje.
    
    // Bitmapy (obrázky v paměti) pro bleskové kreslení bez sekání.
    private Bitmap birdBodyBitmap;
    private Bitmap birdDetailsBitmap;
    private Bitmap backgroundBitmap;
    private Bitmap pipeCapBitmap;
    private Bitmap pipeBodyBitmap;

    private final Rect rectHelper = new Rect(); // Pomocník pro obdélníky.
    private final RectF rectFHelper = new RectF(); // Pomocník pro zaoblené tvary.
    private final String[] scoreCache = new String[1001]; // Předpřipravené texty skóre pro vyšší výkon.

    // Seznam barev pro skiny tvého ptáčka.
    private final int[] skinColors = {
        Color.parseColor("#F0D020"), // Žlutá (originál)
        Color.parseColor("#FF5252"), // Červená
        Color.parseColor("#448AFF"), // Modrá
        Color.parseColor("#69F0AE"), // Zelená
        Color.parseColor("#E040FB"), // Fialová
        Color.parseColor("#FF9800"), // Oranžová
        Color.parseColor("#FFFFFF")  // Bílá
    };

    private final int colorBoard = Color.parseColor("#DED895"); // Barva tabulky se skóre.
    private final int colorBoardBorder = Color.parseColor("#543847"); // Barva okraje tabulky.
    private final int colorButton = Color.parseColor("#E89121"); // Barva tlačítka START.

    public GameRenderer(Context context, int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        textPaint = new Paint();
        textPaint.setAntiAlias(true);

        uiPaint = new Paint();
        uiPaint.setAntiAlias(true);

        // Cache pro skóre, ať se nealokuje paměť v každém snímku (šetří Garbage Collector).
        for (int i = 0; i < scoreCache.length; i++) {
            scoreCache[i] = String.valueOf(i);
        }

        initResources(context); // Načtení všeho do paměti při startu.
    }

    // Tady se všechno načte a připraví do bitmap, aby se hra nesekala.
    private void initResources(Context context) {
        try {
            int birdSize = 120;
            // 1a. Načtení masky těla (to co budeme barvit).
            Drawable bodyDrawable = ContextCompat.getDrawable(context, R.drawable.bird_body);
            if (bodyDrawable != null) {
                birdBodyBitmap = Bitmap.createBitmap(birdSize, birdSize, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(birdBodyBitmap);
                bodyDrawable.setBounds(0, 0, birdSize, birdSize);
                bodyDrawable.draw(canvas);
            }

            // 1b. Načtení detailů (oči, zobák), co zůstávají barevně stejné.
            Drawable detailsDrawable = ContextCompat.getDrawable(context, R.drawable.bird_details);
            if (detailsDrawable != null) {
                birdDetailsBitmap = Bitmap.createBitmap(birdSize, birdSize, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(birdDetailsBitmap);
                detailsDrawable.setBounds(0, 0, birdSize, birdSize);
                detailsDrawable.draw(canvas);
            }

            // 2. Předkreslení celého pozadí (obloha a tráva) do jedné bitmapy.
            backgroundBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
            Canvas bgCanvas = new Canvas(backgroundBitmap);
            bgCanvas.drawColor(Color.parseColor("#4EC0CA"));
            uiPaint.setColor(Color.parseColor("#73BF2E"));
            bgCanvas.drawRect(0, screenHeight - 100, screenWidth, screenHeight, uiPaint);
            uiPaint.setColor(Color.parseColor("#DDE87C"));
            bgCanvas.drawRect(0, screenHeight - 100, screenWidth, screenHeight - 90, uiPaint);

            // 3. Tělo trubky jako tenký vzorek, co se bleskově natáhne na celou výšku.
            pipeBodyBitmap = Bitmap.createBitmap(150, 1, Bitmap.Config.ARGB_8888);
            Canvas bodyCanvas = new Canvas(pipeBodyBitmap);
            Paint p = new Paint();
            p.setColor(Color.parseColor("#73BF2E")); bodyCanvas.drawRect(0, 0, 150, 1, p);
            p.setColor(Color.parseColor("#558022")); bodyCanvas.drawRect(0, 0, 25, 1, p);
            p.setColor(Color.parseColor("#9DE64E")); bodyCanvas.drawRect(125, 0, 140, 1, p);
            p.setColor(Color.parseColor("#543847")); p.setStrokeWidth(1);
            bodyCanvas.drawLine(0, 0, 0, 1, p);
            bodyCanvas.drawLine(149, 0, 149, 1, p);

            // 4. Zakončení trubky (klobouček) do samostatné bitmapy.
            int capW = 180;
            int capH = 50;
            pipeCapBitmap = Bitmap.createBitmap(capW, capH, Bitmap.Config.ARGB_8888);
            Canvas capCanvas = new Canvas(pipeCapBitmap);
            uiPaint.setColor(Color.parseColor("#73BF2E"));
            capCanvas.drawRect(0, 0, capW, capH, uiPaint);
            uiPaint.setColor(Color.parseColor("#543847"));
            uiPaint.setStyle(Paint.Style.STROKE);
            uiPaint.setStrokeWidth(5);
            capCanvas.drawRect(0, 0, capW, capH, uiPaint);
            uiPaint.setStyle(Paint.Style.FILL);

        } catch (Exception e) {
            Log.e("GameRenderer", "Chyba při inicializaci grafiky", e);
        }
    }

    // Vykreslení oblohy a země.
    public void drawBackground(Canvas canvas) {
        if (backgroundBitmap != null) {
            canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        }
    }

    // Metoda pro vykreslení ptáčka se správným skinem a rotací.
    private void drawBird(Canvas canvas, Bird bird, int skinIndex) {
        if (birdBodyBitmap != null && birdDetailsBitmap != null) {
            canvas.save();
            // Výpočet úhlu rotace podle aktuální rychlosti (velocity).
            float velocity = bird.getVelocity();
            float angle = velocity * 3.0f;
            if (angle > 65) angle = 65;
            if (angle < -20) angle = -20;
            canvas.rotate(angle, (float) bird.getX(), (float) bird.getY());
            
            float left = bird.getX() - bird.getRadius();
            float top = bird.getY() - bird.getRadius();

            // 1. Vykreslíme jen tělo a obarvíme ho filtrem podle skinIndexu.
            Paint skinPaint = new Paint();
            skinPaint.setColorFilter(new PorterDuffColorFilter(skinColors[skinIndex], PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(birdBodyBitmap, left, top, skinPaint);
            
            // 2. Navrch plácneme detaily (oko, zobák), co zůstávají barevně stejné.
            canvas.drawBitmap(birdDetailsBitmap, left, top, null);
            
            canvas.restore();
        }
    }

    // Hlavní vykreslování herní scény během hraní.
    public void drawGame(Canvas canvas, Bird bird, List<Obstacle> obstacles, int score, int skinIndex) {
        // Vykreslení všech trubek na obrazovce.
        for (int i = 0; i < obstacles.size(); i++) {
            Obstacle obstacle = obstacles.get(i);
            int x = obstacle.getX();
            int w = obstacle.getWidth();
            int h = obstacle.getTopPipeHeight();
            int gap = obstacle.getGap();

            if (pipeBodyBitmap != null) {
                // Horní část trubky.
                rectHelper.set(x, 0, x + w, h);
                canvas.drawBitmap(pipeBodyBitmap, null, rectHelper, null);
                // Spodní část trubky.
                rectHelper.set(x, h + gap, x + w, screenHeight - 100);
                canvas.drawBitmap(pipeBodyBitmap, null, rectHelper, null);
            }

            if (pipeCapBitmap != null) {
                // Kloboučky na koncích trubek.
                canvas.drawBitmap(pipeCapBitmap, x - 15, h - 50, null);
                canvas.drawBitmap(pipeCapBitmap, x - 15, h + gap, null);
            }
        }

        drawBird(canvas, bird, skinIndex); // Vykreslení našeho fógla.

        // Aktuální skóre nahoře uprostřed.
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(140);
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        String s = (score >= 0 && score < scoreCache.length) ? scoreCache[score] : String.valueOf(score);
        canvas.drawText(s, (float) screenWidth / 2, 250, textPaint);
    }

    // Vykreslení menu s výběrem skinů a statistikami.
    public void drawMenu(Canvas canvas, int score, int highScore, int skinIndex, Rect startButtonRect, Rect arrowLeftRect, Rect arrowRightRect) {
        float centerX = (float) screenWidth / 2;

        // Nadpis hry.
        textPaint.setTextSize(160);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("FLAPPY BIRD", centerX, (float) screenHeight * 0.15f, textPaint);

        // Score board (tabulka se skóre).
        rectFHelper.set(centerX - 300, (float) screenHeight * 0.25f, centerX + 300, (float) screenHeight * 0.45f);
        uiPaint.setColor(colorBoard);
        canvas.drawRoundRect(rectFHelper, 30, 30, uiPaint);
        uiPaint.setColor(colorBoardBorder);
        uiPaint.setStyle(Paint.Style.STROKE);
        canvas.drawRoundRect(rectFHelper, 30, 30, uiPaint);
        uiPaint.setStyle(Paint.Style.FILL);

        textPaint.setTextSize(60);
        textPaint.setColor(colorBoardBorder);
        canvas.drawText("SCORE: " + score, centerX, (float) screenHeight * 0.33f, textPaint);
        canvas.drawText("BEST: " + highScore, centerX, (float) screenHeight * 0.4f, textPaint);

        // Sekce výběru skinu (ptáček a šipky).
        float skinY = (float) screenHeight * 0.58f;
        if (birdBodyBitmap != null && birdDetailsBitmap != null) {
            Paint skinPaint = new Paint();
            skinPaint.setColorFilter(new PorterDuffColorFilter(skinColors[skinIndex], PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(birdBodyBitmap, centerX - 60, skinY - 60, skinPaint);
            canvas.drawBitmap(birdDetailsBitmap, centerX - 60, skinY - 60, null);
        }

        textPaint.setTextSize(100);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("<", centerX - 180, skinY + 30, textPaint);
        canvas.drawText(">", centerX + 180, skinY + 30, textPaint);
        
        // Oblasti pro klikání na šipky.
        arrowLeftRect.set((int)centerX - 250, (int)skinY - 80, (int)centerX - 100, (int)skinY + 80);
        arrowRightRect.set((int)centerX + 100, (int)skinY - 80, (int)centerX + 250, (int)skinY + 80);

        // Tlačítko START.
        int bY = (int) (screenHeight * 0.75f);
        startButtonRect.set((int)centerX - 200, bY - 80, (int)centerX + 200, bY + 80);
        uiPaint.setColor(colorButton);
        canvas.drawRoundRect(new RectF(startButtonRect), 20, 20, uiPaint);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(80);
        canvas.drawText("START", centerX, bY + 30, textPaint);
    }
    
    // Vrátí počet dostupných skinů.
    public int getSkinsCount() {
        return skinColors.length;
    }
}
