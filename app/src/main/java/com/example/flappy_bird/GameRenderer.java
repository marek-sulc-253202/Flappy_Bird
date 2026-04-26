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

/**
 * Třída zajišťující veškeré vykreslování herních prvků a uživatelského rozhraní.
 * Optimalizována pro vysoký výkon pomocí techniky Bitmap Caching (předkreslování do paměti).
 */
public class GameRenderer {

    // Paint objekty pro různé styly vykreslování (texty, UI tvary).
    private final Paint textPaint;
    private final Paint uiPaint;
    private final int screenWidth, screenHeight;
    
    // Cache bitmapy pro statické nebo často opakované grafické prvky.
    private Bitmap birdBodyBitmap;
    private Bitmap birdDetailsBitmap;
    private Bitmap backgroundBitmap;
    private Bitmap pipeCapBitmap;
    private Bitmap pipeBodyBitmap;
    
    // Ikonky pro indikaci stavu zvuku.
    private final Drawable soundOnDrawable;
    private final Drawable soundOffDrawable;

    // Pomocné objekty pro eliminaci alokací paměti během vykreslovací smyčky.
    private final Rect rectHelper = new Rect(); 
    private final RectF rectFHelper = new RectF(); 
    private final String[] scoreCache = new String[1001]; 

    // Definované barevné varianty pro skiny ptáčka.
    private final int[] skinColors = {
        Color.parseColor("#F0D020"), // Žlutá
        Color.parseColor("#FF5252"), // Červená
        Color.parseColor("#448AFF"), // Modrá
        Color.parseColor("#69F0AE"), // Zelená
        Color.parseColor("#E040FB"), // Fialová
        Color.parseColor("#FF9800"), // Oranžová
        Color.parseColor("#FFFFFF")  // Bílá
    };

    // Barevná paleta pro UI prvky menu.
    private final int colorBoard = Color.parseColor("#DED895"); 
    private final int colorBoardBorder = Color.parseColor("#543847"); 
    private final int colorButton = Color.parseColor("#E89121"); 
    private final int colorDiffSelected = Color.parseColor("#FFC107"); 
    private final int colorDiffUnselected = Color.parseColor("#BDBDBD"); 

    /**
     * Konstruktor inicializující kreslicí nástroje a cache pro skóre.
     */
    public GameRenderer(Context context, int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        textPaint = new Paint();
        textPaint.setAntiAlias(true);

        uiPaint = new Paint();
        uiPaint.setAntiAlias(true);
        
        soundOnDrawable = ContextCompat.getDrawable(context, R.drawable.ic_sound_on);
        soundOffDrawable = ContextCompat.getDrawable(context, R.drawable.ic_sound_off);

        // Naplnění cache řetězců pro zobrazení skóre.
        for (int i = 0; i < scoreCache.length; i++) {
            scoreCache[i] = String.valueOf(i);
        }

        initResources(context); 
    }

    /**
     * Vytváří bitmapy z vektorových zdrojů pro zrychlení vykreslování v reálném čase.
     */
    private void initResources(Context context) {
        try {
            int birdSize = 120;
            // Příprava masky těla pro dynamické barvení.
            Drawable bodyDrawable = ContextCompat.getDrawable(context, R.drawable.bird_body);
            if (bodyDrawable != null) {
                birdBodyBitmap = Bitmap.createBitmap(birdSize, birdSize, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(birdBodyBitmap);
                bodyDrawable.setBounds(0, 0, birdSize, birdSize);
                bodyDrawable.draw(canvas);
            }

            // Příprava detailů ptáčka (oči, zobák).
            Drawable detailsDrawable = ContextCompat.getDrawable(context, R.drawable.bird_details);
            if (detailsDrawable != null) {
                birdDetailsBitmap = Bitmap.createBitmap(birdSize, birdSize, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(birdDetailsBitmap);
                detailsDrawable.setBounds(0, 0, birdSize, birdSize);
                detailsDrawable.draw(canvas);
            }

            // Předkreslení kompletního pozadí (obloha a země).
            backgroundBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
            Canvas bgCanvas = new Canvas(backgroundBitmap);
            bgCanvas.drawColor(Color.parseColor("#4EC0CA"));
            uiPaint.setColor(Color.parseColor("#73BF2E"));
            bgCanvas.drawRect(0, screenHeight - 100, screenWidth, screenHeight, uiPaint);
            uiPaint.setColor(Color.parseColor("#DDE87C"));
            bgCanvas.drawRect(0, screenHeight - 100, screenWidth, screenHeight - 90, uiPaint);

            // Příprava vzorku těla trubky pro bleskové roztažení.
            pipeBodyBitmap = Bitmap.createBitmap(150, 1, Bitmap.Config.ARGB_8888);
            Canvas bodyCanvas = new Canvas(pipeBodyBitmap);
            Paint p = new Paint();
            p.setColor(Color.parseColor("#73BF2E")); bodyCanvas.drawRect(0, 0, 150, 1, p);
            p.setColor(Color.parseColor("#558022")); bodyCanvas.drawRect(0, 0, 25, 1, p);
            p.setColor(Color.parseColor("#9DE64E")); bodyCanvas.drawRect(125, 0, 140, 1, p);
            p.setColor(Color.parseColor("#543847")); p.setStrokeWidth(1);
            bodyCanvas.drawLine(0, 0, 0, 1, p);
            bodyCanvas.drawLine(149, 0, 149, 1, p);

            // Příprava bitmapy kloboučku trubky.
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
            Log.e("GameRenderer", "Chyba při inicializaci", e);
        }
    }

    /**
     * Vykreslí předpřipravenou bitmapu pozadí.
     */
    public void drawBackground(Canvas canvas) {
        if (backgroundBitmap != null) {
            canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        }
    }

    /**
     * Vykreslí ptáčka s aplikovanou barvou skinu a úhlem rotace dle rychlosti.
     */
    private void drawBird(Canvas canvas, Bird bird, int skinIndex) {
        if (birdBodyBitmap != null && birdDetailsBitmap != null) {
            canvas.save();
            float velocity = bird.getVelocity();
            float angle = velocity * 3.0f;
            if (angle > 65) angle = 65;
            if (angle < -20) angle = -20;
            canvas.rotate(angle, (float) bird.getX(), (float) bird.getY());
            
            float left = bird.getX() - bird.getRadius();
            float top = bird.getY() - bird.getRadius();

            // Aplikace barvy skinu pouze na tělo ptáčka pomocí PorterDuff filtru.
            Paint skinPaint = new Paint();
            skinPaint.setColorFilter(new PorterDuffColorFilter(skinColors[skinIndex], PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(birdBodyBitmap, left, top, skinPaint);
            
            // Vykreslení detailů bez filtru.
            canvas.drawBitmap(birdDetailsBitmap, left, top, null);
            
            canvas.restore();
        }
    }

    /**
     * Hlavní metoda pro vykreslování hratelné scény.
     */
    public void drawGame(Canvas canvas, Bird bird, List<Obstacle> obstacles, int score, int skinIndex, boolean isMuted, Rect soundBtn) {
        // Vykreslení překážek.
        for (int i = 0; i < obstacles.size(); i++) {
            Obstacle obstacle = obstacles.get(i);
            int x = obstacle.getX();
            int w = obstacle.getWidth();
            int h = obstacle.getTopPipeHeight();
            int gap = obstacle.getGap();

            if (pipeBodyBitmap != null) {
                rectHelper.set(x, 0, x + w, h);
                canvas.drawBitmap(pipeBodyBitmap, null, rectHelper, null);
                rectHelper.set(x, h + gap, x + w, screenHeight - 100);
                canvas.drawBitmap(pipeBodyBitmap, null, rectHelper, null);
            }

            if (pipeCapBitmap != null) {
                canvas.drawBitmap(pipeCapBitmap, x - 15, h - 50, null);
                canvas.drawBitmap(pipeCapBitmap, x - 15, h + gap, null);
            }
        }

        drawBird(canvas, bird, skinIndex);
        
        // Vykreslení tlačítka zvuku.
        soundBtn.set(30, 30, 160, 160);
        Drawable icon = isMuted ? soundOffDrawable : soundOnDrawable;
        if (icon != null) { icon.setBounds(soundBtn); icon.draw(canvas); }

        // Vykreslení skóre s použitím cache.
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(140);
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        String s = (score >= 0 && score < scoreCache.length) ? scoreCache[score] : String.valueOf(score);
        canvas.drawText(s, (float) screenWidth / 2, 250, textPaint);
    }

    /**
     * Vykreslí kompletní rozhraní hlavního menu.
     */
    public void drawMenu(Canvas canvas, int score, int highScore, int skinIndex, int difficulty, boolean isMuted,
                         String playerName, boolean isOnline,
                         Rect startButtonRect, Rect arrowLeftRect, Rect arrowRightRect,
                         Rect easyBtn, Rect normalBtn, Rect hardBtn, Rect soundBtn,
                         Rect addBtn, Rect deleteBtn, Rect skinLeftRect, Rect skinRightRect,
                         Rect onlineToggleRect, Rect offlineToggleRect, boolean forcedOffline) {
        float centerX = (float) screenWidth / 2;

        // Nadpis hry.
        textPaint.setTextSize(130);
        textPaint.setColor(Color.WHITE);
        textPaint.setFakeBoldText(true);
        textPaint.setShadowLayer(12, 6, 6, Color.BLACK);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("FLAPPY BIRD", centerX, (float) screenHeight * 0.15f, textPaint);
        textPaint.setShadowLayer(0, 0, 0, 0); 

        // Score board plocha.
        rectFHelper.set(centerX - 420, (float) screenHeight * 0.26f, centerX + 420, (float) screenHeight * 0.48f);
        uiPaint.setColor(colorBoard);
        canvas.drawRoundRect(rectFHelper, 30, 30, uiPaint);
        uiPaint.setColor(colorBoardBorder);
        uiPaint.setStyle(Paint.Style.STROKE);
        uiPaint.setStrokeWidth(6); 
        canvas.drawRoundRect(rectFHelper, 30, 30, uiPaint);
        uiPaint.setStyle(Paint.Style.FILL);

        // --- PŘEPÍNAČ ONLINE / OFFLINE ---
        float toggleY = (float) screenHeight * 0.22f;
        int tWidth = 230;
        int tHeight = 80;
        
        offlineToggleRect.set((int)centerX - tWidth - 10, (int)toggleY - tHeight/2, (int)centerX - 10, (int)toggleY + tHeight/2);
        onlineToggleRect.set((int)centerX + 10, (int)toggleY - tHeight/2, (int)centerX + tWidth + 10, (int)toggleY + tHeight/2);
        
        drawModeButton(canvas, offlineToggleRect, "OFFLINE", forcedOffline);
        drawModeButton(canvas, onlineToggleRect, "ONLINE", !forcedOffline);

        // Identifikace hráče a rekordy.
        textPaint.setTextSize(55);
        textPaint.setColor(isOnline && !forcedOffline ? Color.BLUE : Color.RED);
        canvas.drawText(playerName, centerX, (float) screenHeight * 0.33f, textPaint);
        
        if (isOnline && !forcedOffline) {
            textPaint.setTextSize(100);
            textPaint.setColor(colorBoardBorder);
            canvas.drawText("<", centerX - 350, (float) screenHeight * 0.34f, textPaint);
            canvas.drawText(">", centerX + 350, (float) screenHeight * 0.34f, textPaint);
            
            arrowLeftRect.set((int)centerX - 420, (int)(screenHeight * 0.26f), (int)centerX - 220, (int)(screenHeight * 0.40f));
            arrowRightRect.set((int)centerX + 220, (int)(screenHeight * 0.26f), (int)centerX + 420, (int)(screenHeight * 0.40f));
            
            addBtn.set((int)centerX + 180, (int)(screenHeight * 0.41f), (int)centerX + 400, (int)(screenHeight * 0.48f));
            deleteBtn.set((int)centerX - 400, (int)(screenHeight * 0.41f), (int)centerX - 180, (int)(screenHeight * 0.48f));
            textPaint.setTextSize(48);
            canvas.drawText("+ NEW", addBtn.centerX(), addBtn.centerY() + 18, textPaint);
            canvas.drawText("DEL -", deleteBtn.centerX(), deleteBtn.centerY() + 18, textPaint);
        }

        textPaint.setTextSize(60);
        textPaint.setColor(colorBoardBorder);
        canvas.drawText("SCORE: " + score, centerX, (float) screenHeight * 0.39f, textPaint);
        canvas.drawText("BEST: " + highScore, centerX, (float) screenHeight * 0.45f, textPaint);

        // Přepínače obtížnosti.
        float diffY = (float) screenHeight * 0.56f;
        int btnW = 230; int btnH = 100; int spacing = 15;
        easyBtn.set((int)centerX - btnW - btnW/2 - spacing, (int)diffY - btnH/2, (int)centerX - btnW/2 - spacing, (int)diffY + btnH/2);
        normalBtn.set((int)centerX - btnW/2, (int)diffY - btnH/2, (int)centerX + btnW/2, (int)diffY + btnH/2);
        hardBtn.set((int)centerX + btnW/2 + spacing, (int)diffY - btnH/2, (int)centerX + btnW + btnW/2 + spacing, (int)diffY + btnH/2);
        drawDiffButton(canvas, easyBtn, "LEHKÁ", difficulty == 0);
        drawDiffButton(canvas, normalBtn, "NORMAL", difficulty == 1);
        drawDiffButton(canvas, hardBtn, "TĚŽKÁ", difficulty == 2);
        uiPaint.setStrokeWidth(1); 

        // Náhled a přepínání skinů.
        float skinY = (float) screenHeight * 0.72f;
        if (birdBodyBitmap != null && birdDetailsBitmap != null) {
            Paint skinPaint = new Paint();
            skinPaint.setColorFilter(new PorterDuffColorFilter(skinColors[skinIndex], PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(birdBodyBitmap, centerX - 60, skinY - 60, skinPaint);
            canvas.drawBitmap(birdDetailsBitmap, centerX - 60, skinY - 60, null);
        }

        textPaint.setTextSize(110);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("<", centerX - 200, skinY + 35, textPaint);
        canvas.drawText(">", centerX + 200, skinY + 35, textPaint);
        
        skinLeftRect.set((int)centerX - 280, (int)skinY - 90, (int)centerX - 120, (int)skinY + 90);
        skinRightRect.set((int)centerX + 120, (int)skinY - 90, (int)centerX + 280, (int)skinY + 90);

        // Hlavní startovní tlačítko.
        int bY = (int) (screenHeight * 0.88f);
        startButtonRect.set((int)centerX - 200, bY - 80, (int)centerX + 200, bY + 80);
        uiPaint.setColor(colorButton);
        canvas.drawRoundRect(new RectF(startButtonRect), 20, 20, uiPaint);
        textPaint.setColor(Color.WHITE); textPaint.setTextSize(80);
        canvas.drawText("START", centerX, bY + 30, textPaint);

        // Indikátor a tlačítko zvuku.
        soundBtn.set(30, 30, 160, 160);
        Drawable icon = isMuted ? soundOffDrawable : soundOnDrawable;
        if (icon != null) { icon.setBounds(soundBtn); icon.draw(canvas); }
    }

    /**
     * Pomocná metoda pro vykreslení tlačítka režimu (Online/Offline).
     */
    private void drawModeButton(Canvas canvas, Rect rect, String label, boolean isSelected) {
        uiPaint.setColor(isSelected ? colorDiffSelected : colorDiffUnselected);
        canvas.drawRoundRect(new RectF(rect), 15, 15, uiPaint);
        uiPaint.setColor(colorBoardBorder);
        uiPaint.setStyle(Paint.Style.STROKE);
        uiPaint.setStrokeWidth(isSelected ? 6 : 2);
        canvas.drawRoundRect(new RectF(rect), 15, 15, uiPaint);
        uiPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(32);
        textPaint.setColor(isSelected ? colorBoardBorder : Color.DKGRAY);
        canvas.drawText(label, rect.centerX(), rect.centerY() + 12, textPaint);
    }

    /**
     * Pomocná metoda pro vykreslení jednotlivého tlačítka obtížnosti.
     */
    private void drawDiffButton(Canvas canvas, Rect rect, String label, boolean isSelected) {
        uiPaint.setColor(isSelected ? colorDiffSelected : colorDiffUnselected);
        canvas.drawRoundRect(new RectF(rect), 15, 15, uiPaint);
        uiPaint.setColor(colorBoardBorder);
        uiPaint.setStyle(Paint.Style.STROKE);
        uiPaint.setStrokeWidth(isSelected ? 6 : 2);
        canvas.drawRoundRect(new RectF(rect), 15, 15, uiPaint);
        uiPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(38);
        textPaint.setColor(isSelected ? colorBoardBorder : Color.DKGRAY);
        canvas.drawText(label, rect.centerX(), rect.centerY() + 14, textPaint);
    }
    
    public int getSkinsCount() { return skinColors.length; }
}
