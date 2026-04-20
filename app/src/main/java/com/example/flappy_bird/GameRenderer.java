package com.example.flappy_bird;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.List;

public class GameRenderer {

    private final Paint textPaint;
    private final Paint uiPaint;
    private final int screenWidth, screenHeight;
    
    // Bitmapy pro bleskové vykreslení bez počítání křivek
    private Bitmap birdBitmap;
    private Bitmap backgroundBitmap;
    private Bitmap pipeCapBitmap;
    private Bitmap pipeBodyBitmap;

    // Pomocné objekty (vytvořené jednou, aby se nezatěžoval Garbage Collector)
    private final Rect rectHelper = new Rect();
    private final RectF rectFHelper = new RectF();

    // Cache pro skóre (aby se nemuselo v každém snímku převádět číslo na String)
    private final String[] scoreCache = new String[1001];

    // Barvy
    private final int colorBoard = Color.parseColor("#DED895");
    private final int colorBoardBorder = Color.parseColor("#543847");
    private final int colorButtonShadow = Color.parseColor("#C46A00");
    private final int colorButton = Color.parseColor("#E89121");

    public GameRenderer(Context context, int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        textPaint = new Paint();
        textPaint.setAntiAlias(true);

        uiPaint = new Paint();
        uiPaint.setAntiAlias(true);

        // Předvyplnění cache pro skóre
        for (int i = 0; i < scoreCache.length; i++) {
            scoreCache[i] = String.valueOf(i);
        }

        initResources(context);
    }

    private void initResources(Context context) {
        try {
            // 1. Ptáček do bitmapy
            Drawable birdDrawable = ContextCompat.getDrawable(context, R.drawable.bird_texture);
            if (birdDrawable != null) {
                int size = 100;
                birdBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(birdBitmap);
                birdDrawable.setBounds(0, 0, size, size);
                birdDrawable.draw(canvas);
            }

            // 2. Celé pozadí do jedné bitmapy (obloha + tráva)
            backgroundBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
            Canvas bgCanvas = new Canvas(backgroundBitmap);
            bgCanvas.drawColor(Color.parseColor("#4EC0CA"));
            uiPaint.setColor(Color.parseColor("#73BF2E"));
            bgCanvas.drawRect(0, screenHeight - 100, screenWidth, screenHeight, uiPaint);
            uiPaint.setColor(Color.parseColor("#DDE87C"));
            bgCanvas.drawRect(0, screenHeight - 100, screenWidth, screenHeight - 90, uiPaint);

            // 3. Tělo trubky jako 1px vysoký vzorek (extrémně rychlé na vykreslování)
            pipeBodyBitmap = Bitmap.createBitmap(150, 1, Bitmap.Config.ARGB_8888);
            Canvas bodyCanvas = new Canvas(pipeBodyBitmap);
            Paint p = new Paint();
            p.setColor(Color.parseColor("#73BF2E")); bodyCanvas.drawRect(0, 0, 150, 1, p);
            p.setColor(Color.parseColor("#558022")); bodyCanvas.drawRect(0, 0, 25, 1, p);
            p.setColor(Color.parseColor("#9DE64E")); bodyCanvas.drawRect(125, 0, 140, 1, p);
            p.setColor(Color.parseColor("#543847")); p.setStrokeWidth(1);
            bodyCanvas.drawLine(0, 0, 0, 1, p);
            bodyCanvas.drawLine(149, 0, 149, 1, p);

            // 4. Klobouček trubky do bitmapy
            int capW = 150 + 30;
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
            Log.e("GameRenderer", "Chyba při inicializaci", e);
        }
    }

    public void drawBackground(Canvas canvas) {
        if (backgroundBitmap != null) {
            canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        }
    }

    public void drawGame(Canvas canvas, Bird bird, List<Obstacle> obstacles, int score) {
        for (int i = 0; i < obstacles.size(); i++) {
            Obstacle obstacle = obstacles.get(i);
            int x = obstacle.getX();
            int w = obstacle.getWidth();
            int h = obstacle.getTopPipeHeight();
            int gap = obstacle.getGap();

            // Vykreslení těl trubek natažením bitmapy (mnohem rychlejší než Recty)
            if (pipeBodyBitmap != null) {
                rectHelper.set(x, 0, x + w, h);
                canvas.drawBitmap(pipeBodyBitmap, null, rectHelper, null);
                rectHelper.set(x, h + gap, x + w, screenHeight - 100);
                canvas.drawBitmap(pipeBodyBitmap, null, rectHelper, null);
            }

            // Kloboučky z bitmapy
            if (pipeCapBitmap != null) {
                canvas.drawBitmap(pipeCapBitmap, x - 15, h - 50, null);
                canvas.drawBitmap(pipeCapBitmap, x - 15, h + gap, null);
            }
        }

        // Ptáček z bitmapy
        if (birdBitmap != null) {
            canvas.drawBitmap(birdBitmap, bird.getX() - bird.getRadius(), bird.getY() - bird.getRadius(), null);
        }

        // Skóre z cache (žádná alokace paměti)
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(140);
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        String s = (score >= 0 && score < scoreCache.length) ? scoreCache[score] : String.valueOf(score);
        canvas.drawText(s, (float) screenWidth / 2, 250, textPaint);
    }

    public void drawMenu(Canvas canvas, int score, int highScore, Rect startButtonRect) {
        float centerX = (float) screenWidth / 2;

        int bWidth = 400;
        int bHeight = 160;
        int bY = (int) (screenHeight * 0.75f);
        startButtonRect.set((int)centerX - bWidth/2, bY - bHeight/2, (int)centerX + bWidth/2, bY + bHeight/2);

        textPaint.setTextSize(160);
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("FLAPPY", centerX, (float) screenHeight * 0.2f, textPaint);
        canvas.drawText("BIRD", centerX, (float) screenHeight * 0.3f, textPaint);

        rectFHelper.set(centerX - 300, (float) screenHeight * 0.45f - 150, centerX + 300, (float) screenHeight * 0.45f + 150);
        uiPaint.setColor(colorBoard);
        canvas.drawRoundRect(rectFHelper, 30, 30, uiPaint);
        uiPaint.setStyle(Paint.Style.STROKE);
        uiPaint.setStrokeWidth(10);
        uiPaint.setColor(colorBoardBorder);
        canvas.drawRoundRect(rectFHelper, 30, 30, uiPaint);
        uiPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(colorBoardBorder);
        textPaint.setTextSize(60);
        canvas.drawText("SCORE: " + score, centerX, (float) screenHeight * 0.45f - 30, textPaint);
        canvas.drawText("BEST: " + highScore, centerX, (float) screenHeight * 0.45f + 70, textPaint);

        rectFHelper.set(startButtonRect.left, startButtonRect.top + 10, startButtonRect.right, startButtonRect.bottom + 10);
        uiPaint.setColor(colorButtonShadow);
        canvas.drawRoundRect(rectFHelper, 20, 20, uiPaint);
        rectFHelper.set(startButtonRect);
        uiPaint.setColor(colorButton);
        canvas.drawRoundRect(rectFHelper, 20, 20, uiPaint);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(80);
        float textY = startButtonRect.centerY() + (textPaint.descent() - textPaint.ascent()) / 2 - textPaint.descent();
        canvas.drawText("START", centerX, textY, textPaint);
    }
}
