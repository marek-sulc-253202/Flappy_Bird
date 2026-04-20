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
    private GameRenderer renderer;

    private int score = 0;
    private int highScore = 0;
    private final SharedPreferences prefs;

    public GameView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
        this.bird = new Bird(200, 500);
        this.obstacles = new ArrayList<>();

        prefs = context.getSharedPreferences("FlappyBirdPrefs", Context.MODE_PRIVATE);
        highScore = prefs.getInt("highScore", 0);
        
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
                        renderer.drawMenu(canvas, score, highScore, startButtonRect);
                    } else {
                        renderer.drawGame(canvas, bird, obstacles, score);
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
            if (!isPlaying) {
                if (startButtonRect.contains((int)event.getX(), (int)event.getY())) {
                    score = 0;
                    isPlaying = true;
                }
            } else {
                bird.jump();
            }
        }
        return true;
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
        // Předáváme Context pro načtení Drawable zdrojů.
        this.renderer = new GameRenderer(getContext(), width, height);
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        pause();
    }
}
