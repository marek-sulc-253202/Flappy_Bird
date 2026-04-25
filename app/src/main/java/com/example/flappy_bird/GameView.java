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

    private Thread thread; 
    private boolean isRunning; 
    private boolean isPlaying = false; 
    private boolean isDying = false; 
    
    private final SurfaceHolder holder;
    private final Bird bird;
    private final List<Obstacle> obstacles;
    private int screenWidth, screenHeight; 
    
    private final Rect startButtonRect;
    private final Rect arrowLeftRect = new Rect();
    private final Rect arrowRightRect = new Rect();
    private final Rect easyBtnRect = new Rect();
    private final Rect normalBtnRect = new Rect();
    private final Rect hardBtnRect = new Rect();
    private final Rect soundBtnRect = new Rect(); // Rect pro ikonku v rohu.
    
    private GameRenderer renderer;
    private final SoundManager soundManager;

    private int score = 0; 
    private final int[] highScores = new int[3]; 
    private int skinIndex = 0; 
    private int difficulty = 1; 
    private final SharedPreferences prefs;

    public GameView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
        this.bird = new Bird(200, 500);
        this.obstacles = new ArrayList<>();
        this.soundManager = new SoundManager(context);

        prefs = context.getSharedPreferences("FlappyBirdPrefs", Context.MODE_PRIVATE);
        
        highScores[0] = prefs.getInt("highScore_0", 0);
        highScores[1] = prefs.getInt("highScore_1", 0);
        highScores[2] = prefs.getInt("highScore_2", 0);
        
        skinIndex = prefs.getInt("skinIndex", 0);
        difficulty = prefs.getInt("difficulty", 1); 
        soundManager.setMuted(prefs.getBoolean("isMuted", false));
        
        startButtonRect = new Rect();
    }

    @Override
    public void run() {
        while(isRunning) {
            if (isPlaying && !isDying) {
                update(); 
            }
            draw(); 
            try {
                //noinspection BusyWait
                Thread.sleep(16); 
            } catch (InterruptedException e) {
                Log.e("GameView", "Chyba v herní smyčce", e);
            }
        }
    }

    private void update() {
        bird.applyPhysics(); 

        if (bird.getY() + bird.getRadius() > screenHeight - 100 || bird.getY() - bird.getRadius() < 0) {
            handleGameOver();
        }

        int baseSpeed = 10;
        int speedStep = 10;
        if (difficulty == 0) { baseSpeed = 7; speedStep = 15; } 
        if (difficulty == 2) { baseSpeed = 14; speedStep = 7; }

        int currentSpeed = baseSpeed + (score / speedStep);
        if (currentSpeed > 30) currentSpeed = 30;

        if (screenWidth > 0) {
            int obstacleDistance = (difficulty == 0) ? 1000 : (difficulty == 2 ? 650 : 800);
            if (obstacles.isEmpty() || obstacles.get(obstacles.size() - 1).getX() < screenWidth - obstacleDistance) {
                int lastHeight = -1;
                if (!obstacles.isEmpty()) {
                    lastHeight = obstacles.get(obstacles.size() - 1).getTopPipeHeight();
                }
                obstacles.add(new Obstacle(screenWidth, screenHeight - 100, currentSpeed, lastHeight));
            }
        }

        Iterator<Obstacle> iterator = obstacles.iterator();
        while (iterator.hasNext()) {
            Obstacle obstacle = iterator.next();
            obstacle.moveToLeft();

            if (!obstacle.isPassed() && bird.getX() > obstacle.getX() + obstacle.getWidth()) {
                score++;
                obstacle.setPassed(true);
                soundManager.playPoint();
            }

            if (obstacle.isColliding(bird)) {
                handleGameOver();
                return;
            }

            if (obstacle.getX() + obstacle.getWidth() < 0) {
                iterator.remove();
            }
        }
    }

    private void handleGameOver() {
        if (isDying) return; 
        isDying = true; 
        soundManager.playHit(); 

        new Thread(() -> {
            try {
                Thread.sleep(600); 
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            post(() -> {
                gameOver();
                isDying = false; 
            });
        }).start();
    }

    private void gameOver() {
        isPlaying = false;
        if (score > highScores[difficulty]) {
            highScores[difficulty] = score;
            saveHighScore();
        }
        bird.reset(screenHeight / 2);
        obstacles.clear();
    }

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
                    renderer.drawBackground(canvas);
                    if (!isPlaying) {
                        renderer.drawMenu(canvas, score, highScores[difficulty], skinIndex, difficulty, 
                                        soundManager.isMuted(), startButtonRect, arrowLeftRect, arrowRightRect,
                                        easyBtnRect, normalBtnRect, hardBtnRect, soundBtnRect);
                    } else {
                        // Přidáváme isMuted a soundBtnRect i pro hru.
                        renderer.drawGame(canvas, bird, obstacles, score, skinIndex, soundManager.isMuted(), soundBtnRect);
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

            // Kliknutí na zvuk funguje v menu i během hry.
            if (soundBtnRect.contains(x, y)) {
                soundManager.toggleMute();
                saveMuteState();
                return true; // Událost zpracována.
            }

            if (!isPlaying) {
                if (easyBtnRect.contains(x, y)) { difficulty = 0; score = 0; saveDifficulty(); }
                else if (normalBtnRect.contains(x, y)) { difficulty = 1; score = 0; saveDifficulty(); }
                else if (hardBtnRect.contains(x, y)) { difficulty = 2; score = 0; saveDifficulty(); }
                else if (arrowLeftRect.contains(x, y)) {
                    skinIndex = (skinIndex - 1 + renderer.getSkinsCount()) % renderer.getSkinsCount();
                    saveSkin();
                } else if (arrowRightRect.contains(x, y)) {
                    skinIndex = (skinIndex + 1) % renderer.getSkinsCount();
                    saveSkin();
                } 
                else if (startButtonRect.contains(x, y)) {
                    score = 0;
                    isPlaying = true;
                }
            } else if (!isDying) {
                bird.jump();
                soundManager.playJump();
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
    
    private void saveMuteState() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isMuted", soundManager.isMuted());
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
