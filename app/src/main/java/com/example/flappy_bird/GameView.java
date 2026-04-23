package com.example.flappy_bird;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameView extends SurfaceView implements Runnable, SurfaceHolder.Callback {

    private Thread thread; // Vlákno pro herní smyčku.
    private boolean isRunning; // Jestli vlákno běží.
    private boolean isPlaying = false; // Jestli se zrovna hraje.
    private final SurfaceHolder holder;
    private final Bird bird;
    private final List<Obstacle> obstacles;
    private int screenWidth, screenHeight; // Rozměry displeje.
    
    private final Rect startButtonRect;
    private final Rect arrowLeftRect = new Rect();
    private final Rect arrowRightRect = new Rect();
    
    // Tlačítka pro obtížnost.
    private final Rect easyBtnRect = new Rect();
    private final Rect normalBtnRect = new Rect();
    private final Rect hardBtnRect = new Rect();
    
    private GameRenderer renderer;

    private int score = 0; // Aktuální body.
    private final int[] highScores = new int[3]; // Rekordy pro 3 obtížnosti.
    private int skinIndex = 0; // Vybraný skin.
    private int difficulty = 1; // 0: LEHKÁ, 1: NORMAL, 2: TĚŽKÁ.
    private final SharedPreferences prefs;

    public GameView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
        this.bird = new Bird(200, 500);
        this.obstacles = new ArrayList<>();

        prefs = context.getSharedPreferences("FlappyBirdPrefs", Context.MODE_PRIVATE);
        
        // Načtení rekordů pro všechny obtížnosti.
        highScores[0] = prefs.getInt("highScore_0", 0);
        highScores[1] = prefs.getInt("highScore_1", 0);
        highScores[2] = prefs.getInt("highScore_2", 0);
        
        skinIndex = prefs.getInt("skinIndex", 0);
        difficulty = prefs.getInt("difficulty", 1); 
        
        startButtonRect = new Rect();
    }

    @Override
    public void run() {
        // Smyčka běží pořád dokola, dokud je isRunning true.
        while(isRunning) {
            if (isPlaying) {
                update(); // Výpočty jen při hře.
            }
            draw(); // Kreslíme vždycky (menu nebo hru).
            try {
                //noinspection BusyWait
                Thread.sleep(16); // Stabilních 60 FPS.
            } catch (InterruptedException e) {
                Log.e("GameView", "Chyba v herní smyčce", e);
            }
        }
    }

    private void update() {
        bird.applyPhysics(); // Gravitace táhne ptáčka k zemi.

        // Pokud ptáček vyletí z obrazovky nebo spadne, konec hry.
        if (bird.getY() + bird.getRadius() > screenHeight - 100 || bird.getY() - bird.getRadius() < 0) {
            gameOver();
        }

        // Nastavení parametrů podle zvolené obtížnosti.
        int baseSpeed = 10;
        int speedStep = 10;
        if (difficulty == 0) { baseSpeed = 7; speedStep = 15; } // LEHKÁ: pomalejší start i zrychlování.
        if (difficulty == 2) { baseSpeed = 14; speedStep = 7; } // TĚŽKÁ: rychlý start a drsné zrychlování.

        int currentSpeed = baseSpeed + (score / speedStep);
        if (currentSpeed > 30) currentSpeed = 30; // Maximální rozumná rychlost.

        // Generování nových překážek.
        if (screenWidth > 0) {
            // Vzdálenost trubek se mění podle obtížnosti.
            int obstacleDistance = (difficulty == 0) ? 1000 : (difficulty == 2 ? 650 : 800);
            if (obstacles.isEmpty() || obstacles.get(obstacles.size() - 1).getX() < screenWidth - obstacleDistance) {
                
                // Zjistíme výšku poslední trubky, abychom na ni mohli navázat.
                int lastHeight = -1;
                if (!obstacles.isEmpty()) {
                    lastHeight = obstacles.get(obstacles.size() - 1).getTopPipeHeight();
                }
                
                // Přidání nové trubky s informací o té minulé.
                obstacles.add(new Obstacle(screenWidth, screenHeight - 100, currentSpeed, lastHeight));
            }
        }

        // Pohyb a kolize všech trubek.
        Iterator<Obstacle> iterator = obstacles.iterator();
        while (iterator.hasNext()) {
            Obstacle obstacle = iterator.next();
            obstacle.moveToLeft();

            // Přičtení bodu při úspěšném průletu.
            if (!obstacle.isPassed() && bird.getX() > obstacle.getX() + obstacle.getWidth()) {
                score++;
                obstacle.setPassed(true);
            }

            // Pokud narazíme, konec.
            if (obstacle.isColliding(bird)) {
                gameOver();
                return;
            }

            // Smazání trubky co už uletěla mimo displej.
            if (obstacle.getX() + obstacle.getWidth() < 0) {
                iterator.remove();
            }
        }
    }

    // Volá se při nárazu nebo pádu.
    private void gameOver() {
        isPlaying = false; // Návrat do menu.
        // Kontrola a uložení nového rekordu pro danou obtížnost.
        if (score > highScores[difficulty]) {
            highScores[difficulty] = score;
            saveHighScore();
        }
        bird.reset(screenHeight / 2); // Reset ptáčka na střed.
        obstacles.clear(); // Vyčistit staré trubky.
    }

    // Uložení rekordu do paměti mobilu.
    private void saveHighScore() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("highScore_" + difficulty, highScores[difficulty]);
        editor.apply();
    }

    private void draw() {
        if (holder.getSurface().isValid()) {
            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                if (renderer != null) {
                    renderer.drawBackground(canvas); // Pozadí.
                    if (!isPlaying) {
                        // Vykreslení menu se správným skóre a rekordem.
                        renderer.drawMenu(canvas, score, highScores[difficulty], skinIndex, difficulty, 
                                        startButtonRect, arrowLeftRect, arrowRightRect,
                                        easyBtnRect, normalBtnRect, hardBtnRect);
                    } else {
                        // Vykreslení samotné hry.
                        renderer.drawGame(canvas, bird, obstacles, score, skinIndex);
                    }
                }
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            performClick(); 
            int x = (int) event.getX();
            int y = (int) event.getY();

            if (!isPlaying) {
                // Přepínání obtížnosti v menu - VYNULUJEME SCORE, aby se nepřenášelo.
                if (easyBtnRect.contains(x, y)) { difficulty = 0; score = 0; saveDifficulty(); }
                else if (normalBtnRect.contains(x, y)) { difficulty = 1; score = 0; saveDifficulty(); }
                else if (hardBtnRect.contains(x, y)) { difficulty = 2; score = 0; saveDifficulty(); }
                
                // Přepínání skinů.
                else if (arrowLeftRect.contains(x, y)) {
                    skinIndex = (skinIndex - 1 + renderer.getSkinsCount()) % renderer.getSkinsCount();
                    saveSkin();
                } else if (arrowRightRect.contains(x, y)) {
                    skinIndex = (skinIndex + 1) % renderer.getSkinsCount();
                    saveSkin();
                } 
                
                // Start hry.
                else if (startButtonRect.contains(x, y)) {
                    score = 0;
                    isPlaying = true;
                }
            } else {
                // Během hry klepnutí znamená skok.
                bird.jump();
            }
        }
        return true;
    }

    private void saveSkin() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("skinIndex", skinIndex);
        editor.apply();
    }

    private void saveDifficulty() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("difficulty", difficulty);
        editor.apply();
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    public void resume() {
        isRunning = true;
        if (holder.getSurface().isValid()) {
            thread = new Thread(this);
            thread.start();
        }
    }

    public void pause() {
        isRunning = false;
        try { if (thread != null) thread.join(); } catch (InterruptedException e) { Log.e("GameView", "Join error", e); }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) { resume(); }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        this.renderer = new GameRenderer(getContext(), width, height);
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) { pause(); }
}
