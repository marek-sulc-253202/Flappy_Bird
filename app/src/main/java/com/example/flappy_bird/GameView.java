package com.example.flappy_bird;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

    private Thread thread; // Vlákno, ve kterém běží celá hra.
    private boolean isRunning; // Proměnná, co hlídá, jestli běží vlákno.
    private boolean isPlaying = false; // Hrajeme zrovna, nebo jsme v menu?
    private final SurfaceHolder holder; // Pomocník pro přístup k ploše, na které kreslíme.
    private final Bird bird; // Náš hlavní ptáček.
    private final List<Obstacle> obstacles; // Seznam všech trubek na obrazovce.
    private int screenWidth, screenHeight; // Rozměry displeje.
    
    private final Paint textPaint; // Barva a styl textu v menu.
    private final Rect startButtonRect; // Oblast pro kliknutí ke startu.

    public GameView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
        this.bird = new Bird(200, 500);
        this.obstacles = new ArrayList<>();

        // Nastavení vzhledu textu pro menu.
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(100);
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        startButtonRect = new Rect();
    }

    @Override
    public void run() {
        while(isRunning) {
            if (isPlaying) {
                update(); // Pohyby a kolize řešíme jen při hře.
            }
            draw(); // Kreslíme pořád.
            
            // Krátká pauza, aby hra neběžela moc rychle.
            try {
                //noinspection BusyWait
                Thread.sleep(16); // 60 FPS.
            } catch (InterruptedException e) {
                Log.e("GameView", "Chyba v herní smyčce", e);
            }
        }
    }

    private void update() {
        bird.applyPhysics(); // Gravitace.

        // Pokud ptáček vypadne z obrazovky, konec hry.
        if (bird.getY() + bird.getRadius() > screenHeight || bird.getY() - bird.getRadius() < 0) {
            resetGame();
        }

        // Generování nových trubek.
        if (screenWidth > 0) {
            int obstacleDistance = 700; // Vzdálenost mezi trubkami.
            if (obstacles.isEmpty() || obstacles.get(obstacles.size() - 1).getX() < screenWidth - obstacleDistance) {
                obstacles.add(new Obstacle(screenWidth, screenHeight));
            }
        }

        // Pohyb trubek a kolize.
        Iterator<Obstacle> iterator = obstacles.iterator();
        while (iterator.hasNext()) {
            Obstacle obstacle = iterator.next();
            obstacle.moveToLeft();

            if (obstacle.isColliding(bird)) {
                resetGame(); // Narazil jsi, jdeš do menu.
                return;
            }

            if (obstacle.getX() + obstacle.getWidth() < 0) {
                iterator.remove(); // Smazání staré trubky.
            }
        }
    }

    // Metoda pro reset hry a návrat zpět do menu.
    private void resetGame() {
        isPlaying = false; // Hodíme hráče do menu.
        bird.reset(screenHeight / 2); // Ptáček zpátky na střed.
        obstacles.clear(); // Vyčistit plochu od trubek.
    }

    private void draw() {
        if (holder.getSurface().isValid()) {
            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                canvas.drawColor(Color.CYAN); // Obloha.

                if (!isPlaying) {
                    drawMenu(canvas); // Menu, když se nehraje.
                } else {
                    bird.draw(canvas);
                    for (Obstacle obstacle : obstacles) {
                        obstacle.draw(canvas);
                    }
                }

                holder.unlockCanvasAndPost(canvas);
            }
        }
    }

    // Vykreslení úvodního menu.
    private void drawMenu(Canvas canvas) {
        textPaint.setTextSize(150);
        canvas.drawText("FLAPPY BIRD", (float) screenWidth / 2, (float) screenHeight / 3, textPaint);

        textPaint.setTextSize(80);
        String startText = "TAP TO START";
        canvas.drawText(startText, (float) screenWidth / 2, (float) screenHeight / 2, textPaint);
        
        // Kliknutí kdekoli uprostřed spustí hru.
        startButtonRect.set(screenWidth/2 - 400, screenHeight/2 - 150, screenWidth/2 + 400, screenHeight/2 + 100);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            performClick(); // Kvůli přístupnosti.
            if (!isPlaying) {
                if (startButtonRect.contains((int)event.getX(), (int)event.getY())) {
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
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        pause();
    }
}
