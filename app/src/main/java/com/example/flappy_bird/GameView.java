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
    private final SurfaceHolder holder;
    private final Bird bird;
    private final List<Obstacle> obstacles;
    private int screenWidth, screenHeight;
    
    private final Rect startButtonRect;
    private final Rect arrowLeftRect = new Rect();
    private final Rect arrowRightRect = new Rect();
    
    private GameRenderer renderer;

    private int score = 0;
    private int highScore = 0;
    private int skinIndex = 0; // Index vybraného skinu.
    private final SharedPreferences prefs;

    public GameView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
        this.bird = new Bird(200, 500);
        this.obstacles = new ArrayList<>();

        prefs = context.getSharedPreferences("FlappyBirdPrefs", Context.MODE_PRIVATE);
        highScore = prefs.getInt("highScore", 0);
        skinIndex = prefs.getInt("skinIndex", 0); // Načteme i uložený skin.
        
        startButtonRect = new Rect();
    }

    @Override
    public void run() {
        while(isRunning) {
            if (isPlaying) {
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
            gameOver();
        }

        int currentSpeed = 10 + (score / 10);
        if (currentSpeed > 25) currentSpeed = 25;

        if (screenWidth > 0) {
            int obstacleDistance = 800;
            if (obstacles.isEmpty() || obstacles.get(obstacles.size() - 1).getX() < screenWidth - obstacleDistance) {
                obstacles.add(new Obstacle(screenWidth, screenHeight - 100, currentSpeed));
            }
        }

        Iterator<Obstacle> iterator = obstacles.iterator();
        while (iterator.hasNext()) {
            Obstacle obstacle = iterator.next();
            obstacle.moveToLeft();

            if (!obstacle.isPassed() && bird.getX() > obstacle.getX() + obstacle.getWidth()) {
                score++;
                obstacle.setPassed(true);
            }

            if (obstacle.isColliding(bird)) {
                gameOver();
                return;
            }

            if (obstacle.getX() + obstacle.getWidth() < 0) {
                iterator.remove();
            }
        }
    }

    private void gameOver() {
        isPlaying = false;
        if (score > highScore) {
            highScore = score;
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("highScore", highScore);
            editor.apply();
        }
        bird.reset(screenHeight / 2);
        obstacles.clear();
    }

    private void draw() {
        if (holder.getSurface().isValid()) {
            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                if (renderer != null) {
                    renderer.drawBackground(canvas);
                    if (!isPlaying) {
                        renderer.drawMenu(canvas, score, highScore, skinIndex, startButtonRect, arrowLeftRect, arrowRightRect);
                    } else {
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
                // Přepínání skinů šipkami
                if (arrowLeftRect.contains(x, y)) {
                    skinIndex--;
                    if (skinIndex < 0) skinIndex = renderer.getSkinsCount() - 1;
                    saveSkin();
                } else if (arrowRightRect.contains(x, y)) {
                    skinIndex++;
                    if (skinIndex >= renderer.getSkinsCount()) skinIndex = 0;
                    saveSkin();
                } 
                // Start hry
                else if (startButtonRect.contains(x, y)) {
                    score = 0;
                    isPlaying = true;
                }
            } else {
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
        try {
            if (thread != null) {
                thread.join();
            }
        } catch (InterruptedException e) {
            Log.e("GameView", "Chyba při zastavování vlákna", e);
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        resume();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        this.renderer = new GameRenderer(getContext(), width, height);
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        pause();
    }
}
