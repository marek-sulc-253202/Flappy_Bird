package com.example.flappy_bird;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameView extends SurfaceView implements Runnable, SurfaceHolder.Callback {

    private Thread thread; // Vlákno, ve kterém běží celá hra.
    private boolean isRunning; // Proměnná, co hlídá, jestli běží vlákno.
    private boolean isPlaying = false; // Teď zrovna hrajeme, nebo jsme v menu?
    private SurfaceHolder holder; // Pomocník pro přístup k ploše, na které kreslíme.
    private Bird bird; // Náš hlavní ptáček.
    private List<Obstacle> obstacles; // Seznam všech trubek na obrazovce.
    private int screenWidth, screenHeight; // Rozměry displeje.
    private int obstacleDistance = 700; // Mezera mezi trubkami.
    
    private Paint textPaint; // Barva a styl textu v menu.
    private Rect startButtonRect; // Oblast, kde je tlačítko START v menu.
    private Rect menuButtonRect; // Oblast pro tlačítko MENU během hry.

    public GameView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
        this.bird = new Bird(200, 500);
        this.obstacles = new ArrayList<Obstacle>();

        // Nastavení vzhledu textu pro menu a tlačítka.
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(100);
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        startButtonRect = new Rect();
        menuButtonRect = new Rect();
    }

    @Override
    public void run() {
        while(isRunning) {
            if (isPlaying) {
                update(); // Pokud hrajeme, hýbej věcmi.
            }
            draw(); // Kreslíme vždycky (buď menu, nebo hru).
            try {
                Thread.sleep(16); // Stabilních 60 FPS.
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void update() {
        bird.applyPhysics(); // Gravitace táhne ptáčka dolů.

        // Pokud ptáček vypadne z obrazovky, resetujeme ho.
        if (bird.getY() + bird.getRadius() > screenHeight || bird.getY() - bird.getRadius() < 0) {
            resetGame();
        }

        // Generování nových trubek.
        if (screenWidth > 0) {
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
                resetGame(); // Náraz do trubky.
                return;
            }

            if (obstacle.getX() + obstacle.getWidth() < 0) {
                iterator.remove(); // Smazání trubky, co už uletěla.
            }
        }
    }

    // Metoda pro reset hry a návrat do menu.
    private void resetGame() {
        isPlaying = false; // Přepneme do menu.
        bird.reset(screenHeight / 2); // Ptáček zpátky na střed.
        obstacles.clear(); // Vyčistíme trubky.
    }

    private void draw() {
        if (holder.getSurface().isValid()) {
            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                canvas.drawColor(Color.CYAN); // Barva oblohy.

                if (!isPlaying) {
                    drawMenu(canvas); // Pokud nehrajeme, kresli menu.
                } else {
                    // Pokud hrajeme, kresli ptáčka, trubky a tlačítko pro pauzu.
                    bird.draw(canvas);
                    for (Obstacle obstacle : obstacles) {
                        obstacle.draw(canvas);
                    }
                    drawPauseButton(canvas);
                }

                holder.unlockCanvasAndPost(canvas);
            }
        }
    }

    // Vykreslení úvodního menu.
    private void drawMenu(Canvas canvas) {
        textPaint.setTextSize(150);
        canvas.drawText("FLAPPY BIRD", screenWidth / 2, screenHeight / 3, textPaint);

        textPaint.setTextSize(80);
        String startText = "TAP TO START";
        canvas.drawText(startText, screenWidth / 2, screenHeight / 2, textPaint);
        
        // Oblast pro kliknutí na start (střed obrazovky).
        startButtonRect.set(screenWidth/2 - 300, screenHeight/2 - 100, screenWidth/2 + 300, screenHeight/2 + 50);
    }

    // Vykreslení tlačítka STOP - posunuto dál od kraje kvůli liště.
    private void drawPauseButton(Canvas canvas) {
        textPaint.setTextSize(60);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        
        // Posunuto na X: screenWidth - 100 a Y: 180 (aby to nebylo pod hodinami).
        canvas.drawText("STOP", screenWidth - 100, 180, textPaint);
        
        // Zvětšená oblast pro kliknutí (neviditelný obdélník kolem textu).
        menuButtonRect.set(screenWidth - 350, 50, screenWidth, 250);
        
        textPaint.setTextAlign(Paint.Align.CENTER); // Vrátíme nastavení textu.
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            if (!isPlaying) {
                // Kliknutí kdekoli v oblasti startu spustí hru.
                if (startButtonRect.contains(x, y)) {
                    isPlaying = true;
                }
            } else {
                // Pokud klikneme do pravého horního rohu (oblast STOP), jdeme do menu.
                if (menuButtonRect.contains(x, y)) {
                    resetGame();
                } else {
                    // Kdekoli jinde ptáček skočí.
                    bird.jump();
                }
            }
        }
        return true;
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
            e.printStackTrace();
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
