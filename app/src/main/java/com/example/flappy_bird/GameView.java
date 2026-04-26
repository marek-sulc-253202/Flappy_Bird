package com.example.flappy_bird;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Hlavní třída herního zobrazení využívající SurfaceView pro plynulé vykreslování v samostatném vlákně.
 * Zajišťuje herní smyčku, fyziku, kolize, vstupy od uživatele a synchronizaci se serverem.
 */
public class GameView extends SurfaceView implements Runnable, SurfaceHolder.Callback {

    // Herní vlákno a stavové příznaky.
    private Thread thread;
    private boolean isRunning;
    private boolean isPlaying = false; // Příznak aktivní hratelné scény.
    private boolean isDying = false; // Příznak stavu po kolizi pro doznění efektů.
    
    private final SurfaceHolder holder;
    private final Bird bird;
    private final List<Obstacle> obstacles;
    private int screenWidth, screenHeight; 
    
    // Definice interaktivních oblastí uživatelského rozhraní v menu.
    private final Rect startButtonRect = new Rect();
    private final Rect arrowLeftRect = new Rect(); 
    private final Rect arrowRightRect = new Rect();
    private final Rect easyBtnRect = new Rect();
    private final Rect normalBtnRect = new Rect();
    private final Rect hardBtnRect = new Rect();
    private final Rect soundBtnRect = new Rect();
    private final Rect addPlayerBtnRect = new Rect();
    private final Rect deletePlayerBtnRect = new Rect();
    private final Rect skinLeftRect = new Rect();
    private final Rect skinRightRect = new Rect();
    
    // Oblasti pro přepínání online/offline režimu.
    private final Rect onlineToggleRect = new Rect();
    private final Rect offlineToggleRect = new Rect();
    
    private GameRenderer renderer;
    private final SoundManager soundManager;

    // Proměnné pro skóre a lokální nastavení.
    private int score = 0; 
    private final int[] highScores = new int[3]; 
    private int skinIndex = 0; 
    private int difficulty = 1; 
    private final SharedPreferences prefs;

    // Datové struktury pro online režim.
    private List<NetworkManager.PlayerModel> onlinePlayers = new ArrayList<>();
    private int currentPlayerIndex = -1;
    private boolean isOnline = false;
    private boolean forcedOffline = true; // Pokud je true, aplikace se nepokouší o připojení.

    /**
     * Konstruktor inicializující herní objekty, načítající lokální data a iniciující spojení se serverem.
     */
    public GameView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
        this.bird = new Bird(200, 500);
        this.obstacles = new ArrayList<>();
        this.soundManager = new SoundManager(context);

        // Inicializace síťového manažera z paměti.
        NetworkManager.init(context);

        prefs = context.getSharedPreferences("FlappyBirdPrefs", Context.MODE_PRIVATE);
        loadLocalData();
        
        // Pokud není vynucen offline režim, zkusíme server.
        if (!forcedOffline) {
            refreshPlayersFromServer();
        }
    }

    /**
     * Načte uložená data z lokálního úložiště SharedPreferences.
     */
    private void loadLocalData() {
        forcedOffline = prefs.getBoolean("forcedOffline", true);
        
        // V offline režimu načítáme lokální rekordy, v online se přepíší daty ze serveru.
        if (forcedOffline) {
            highScores[0] = prefs.getInt("highScore_0", 0);
            highScores[1] = prefs.getInt("highScore_1", 0);
            highScores[2] = prefs.getInt("highScore_2", 0);
        }
        
        skinIndex = prefs.getInt("skinIndex", 0);
        difficulty = prefs.getInt("difficulty", 1); 
        soundManager.setMuted(prefs.getBoolean("isMuted", false));
    }

    /**
     * Asynchronně stahuje aktuální seznam hráčů ze serveru a aktualizuje stav připojení.
     */
    private void refreshPlayersFromServer() {
        if (forcedOffline) return;

        NetworkManager.getApi().getPlayers().enqueue(new Callback<List<NetworkManager.PlayerModel>>() {
            @Override
            public void onResponse(@NonNull Call<List<NetworkManager.PlayerModel>> call, @NonNull Response<List<NetworkManager.PlayerModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    onlinePlayers = response.body();
                    isOnline = true;
                    
                    if (onlinePlayers.isEmpty()) {
                        currentPlayerIndex = -1;
                    } else if (currentPlayerIndex == -1 || currentPlayerIndex >= onlinePlayers.size()) {
                        currentPlayerIndex = 0;
                    }
                    updateScoresFromCurrentPlayer();
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<NetworkManager.PlayerModel>> call, @NonNull Throwable t) {
                isOnline = false; 
                Log.e("GameView", "Server offline: " + t.getMessage());
            }
        });
    }

    /**
     * Aktualizuje zobrazené rekordy v menu podle aktuálně vybraného online profilu.
     */
    private void updateScoresFromCurrentPlayer() {
        if (isOnline && !forcedOffline && currentPlayerIndex >= 0 && currentPlayerIndex < onlinePlayers.size()) {
            NetworkManager.PlayerModel p = onlinePlayers.get(currentPlayerIndex);
            highScores[0] = p.score_easy;
            highScores[1] = p.score_normal;
            highScores[2] = p.score_hard;
        }
    }

    /**
     * Hlavní metoda herního vlákna zajišťující periodické volání update a draw.
     */
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

    /**
     * Provádí aktualizaci stavu všech herních entit v každém snímku.
     */
    private void update() {
        bird.applyPhysics(); 

        if (bird.getY() + bird.getRadius() > screenHeight - 100 || bird.getY() - bird.getRadius() < 0) {
            handleGameOver();
        }

        int baseSpeed = (difficulty == 0) ? 7 : (difficulty == 2 ? 14 : 10);
        int speedStep = (difficulty == 0) ? 15 : (difficulty == 2 ? 7 : 10);
        int currentSpeed = baseSpeed + (score / speedStep);
        if (currentSpeed > 30) currentSpeed = 30;

        if (screenWidth > 0) {
            int obstacleDistance = (difficulty == 0) ? 1000 : (difficulty == 2 ? 650 : 800);
            if (obstacles.isEmpty() || obstacles.get(obstacles.size() - 1).getX() < screenWidth - obstacleDistance) {
                int lastHeight = -1;
                if (!obstacles.isEmpty()) lastHeight = obstacles.get(obstacles.size() - 1).getTopPipeHeight();
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

            if (obstacle.getX() + obstacle.getWidth() < 0) iterator.remove();
        }
    }

    /**
     * Zajišťuje sekvenci akcí po kolizi (zvuk, pauza) před návratem do menu.
     */
    private void handleGameOver() {
        if (isDying) return; 
        isDying = true; 
        soundManager.playHit(); 

        // Spuštění časovače pro prodlevu bez blokování vykreslovacího vlákna.
        new Thread(() -> {
            try { Thread.sleep(600); } catch (InterruptedException e) { Log.e("GameView", "Sleep error", e); }
            post(() -> {
                gameOver();
                isDying = false; 
            });
        }).start();
    }

    /**
     * Ukončí hru a vyhodnotí/uloží dosažené skóre.
     */
    private void gameOver() {
        isPlaying = false;
        if (score > highScores[difficulty]) {
            highScores[difficulty] = score;
            if (isOnline && !forcedOffline && currentPlayerIndex != -1) {
                sendScoreToServer();
            } else {
                saveLocalHighScore();
            }
        }
        bird.reset(screenHeight / 2);
        obstacles.clear();
    }

    /**
     * Odešle aktualizovaný rekord na server pro vybraného hráče.
     */
    private void sendScoreToServer() {
        if (currentPlayerIndex < 0 || currentPlayerIndex >= onlinePlayers.size()) return;
        String name = onlinePlayers.get(currentPlayerIndex).name;
        NetworkManager.getApi().updateScore(new NetworkManager.ScoreUpdateModel(name, difficulty, score)).enqueue(new Callback<>() {
            @Override public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) { refreshPlayersFromServer(); }
            @Override public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) { Log.e("GameView", "Update fail", t); }
        });
    }

    /**
     * Uloží rekord lokálně do paměti telefonu.
     */
    private void saveLocalHighScore() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("highScore_" + difficulty, highScores[difficulty]);
        editor.apply();
    }

    /**
     * Deleguje vykreslovací úlohy na třídu GameRenderer.
     */
    private void draw() {
        if (holder.getSurface().isValid()) {
            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                if (renderer != null) {
                    renderer.drawBackground(canvas);
                    if (!isPlaying) {
                        String playerName = (isOnline && !forcedOffline && currentPlayerIndex >= 0 && currentPlayerIndex < onlinePlayers.size()) 
                                            ? onlinePlayers.get(currentPlayerIndex).name : "OFFLINE";
                        renderer.drawMenu(canvas, score, highScores[difficulty], skinIndex, difficulty, 
                                        soundManager.isMuted(), playerName, isOnline, startButtonRect, arrowLeftRect, arrowRightRect,
                                        easyBtnRect, normalBtnRect, hardBtnRect, soundBtnRect,
                                        addPlayerBtnRect, deletePlayerBtnRect, skinLeftRect, skinRightRect,
                                        onlineToggleRect, offlineToggleRect, forcedOffline);
                    } else {
                        renderer.drawGame(canvas, bird, obstacles, score, skinIndex, soundManager.isMuted(), soundBtnRect);
                    }
                }
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }

    /**
     * Reaguje na dotykové události a zajišťuje interakci s menu i ovládání skoku.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            performClick(); 
            int x = (int) event.getX();
            int y = (int) event.getY();

            if (soundBtnRect.contains(x, y)) {
                soundManager.toggleMute();
                saveMuteState();
                return true;
            }

            if (!isPlaying) {
                // Přepínání online/offline režimu.
                if (offlineToggleRect.contains(x, y)) {
                    forcedOffline = true;
                    isOnline = false;
                    saveOfflineState();
                    loadLocalData(); // Načteme lokální rekordy.
                    return true;
                }
                if (onlineToggleRect.contains(x, y)) {
                    showServerAddressDialog();
                    return true;
                }

                // Interakce s online profily (jen pokud není vynucen offline).
                if (!forcedOffline && isOnline && !onlinePlayers.isEmpty()) {
                    if (arrowLeftRect.contains(x, y)) {
                        currentPlayerIndex = (currentPlayerIndex - 1 + onlinePlayers.size()) % onlinePlayers.size();
                        updateScoresFromCurrentPlayer();
                        score = 0;
                        return true;
                    } else if (arrowRightRect.contains(x, y)) {
                        currentPlayerIndex = (currentPlayerIndex + 1) % onlinePlayers.size();
                        updateScoresFromCurrentPlayer();
                        score = 0;
                        return true;
                    }
                }
                
                if (!forcedOffline && isOnline) {
                    if (addPlayerBtnRect.contains(x, y)) { showAddPlayerDialog(); return true; }
                    if (deletePlayerBtnRect.contains(x, y)) { deleteCurrentPlayer(); return true; }
                }

                if (skinLeftRect.contains(x, y)) {
                    skinIndex = (skinIndex - 1 + renderer.getSkinsCount()) % renderer.getSkinsCount();
                    saveSkin();
                    return true;
                } else if (skinRightRect.contains(x, y)) {
                    skinIndex = (skinIndex + 1) % renderer.getSkinsCount();
                    saveSkin();
                    return true;
                }

                if (easyBtnRect.contains(x, y)) { difficulty = 0; score = 0; if(!forcedOffline) updateScoresFromCurrentPlayer(); saveDifficulty(); }
                else if (normalBtnRect.contains(x, y)) { difficulty = 1; score = 0; if(!forcedOffline) updateScoresFromCurrentPlayer(); saveDifficulty(); }
                else if (hardBtnRect.contains(x, y)) { difficulty = 2; score = 0; if(!forcedOffline) updateScoresFromCurrentPlayer(); saveDifficulty(); }
                else if (startButtonRect.contains(x, y)) { score = 0; isPlaying = true; }
            } else if (!isDying) {
                bird.jump();
                soundManager.playJump();
            }
        }
        return true;
    }

    /**
     * Zobrazí dialog pro zadání adresy serveru a pokusí se o připojení.
     */
    private void showServerAddressDialog() {
        EditText input = new EditText(getContext());
        input.setText(NetworkManager.getBaseUrl());
        input.setHint("http://192.168.1.15:8000/");
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(getContext())
            .setTitle("Připojit k serveru")
            .setMessage("Zadej IP adresu nebo hostname notebooku:")
            .setView(input)
            .setPositiveButton("Připojit", (dialog, which) -> {
                String url = input.getText().toString().trim();
                if (!url.isEmpty()) {
                    NetworkManager.updateBaseUrl(getContext(), url);
                    forcedOffline = false;
                    saveOfflineState();
                    refreshPlayersFromServer(); // Zkusíme se hned připojit.
                }
            })
            .setNegativeButton("Zrušit", null)
            .show();
    }

    /**
     * Zobrazí dialogové okno pro zadání jména nového hráče na serveru.
     */
    private void showAddPlayerDialog() {
        EditText input = new EditText(getContext());
        new AlertDialog.Builder(getContext())
            .setTitle("Nový hráč")
            .setMessage("Zadej jméno:")
            .setView(input)
            .setPositiveButton("OK", (dialog, which) -> {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) {
                    NetworkManager.getApi().addPlayer(new NetworkManager.PlayerModel(name)).enqueue(new Callback<>() {
                        @Override public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) { refreshPlayersFromServer(); }
                        @Override public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) { Log.e("GameView", "Add fail", t); }
                    });
                }
            })
            .setNegativeButton("Zrušit", null)
            .show();
    }

    private void deleteCurrentPlayer() {
        if (isOnline && !forcedOffline && currentPlayerIndex >= 0 && currentPlayerIndex < onlinePlayers.size()) {
            String name = onlinePlayers.get(currentPlayerIndex).name;
            NetworkManager.getApi().deletePlayer(new NetworkManager.PlayerModel(name)).enqueue(new Callback<>() {
                @Override 
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) { 
                    currentPlayerIndex = -1;
                    refreshPlayersFromServer(); 
                }
                @Override public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) { Log.e("GameView", "Delete fail", t); }
            });
        }
    }

    private void saveOfflineState() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("forcedOffline", forcedOffline);
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

    private void saveSkin() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("skinIndex", skinIndex);
        editor.apply();
    }

    // Metody životního cyklu SurfaceView.
    @Override public boolean performClick() { return super.performClick(); }
    public void resume() { isRunning = true; if (holder.getSurface().isValid()) { thread = new Thread(this); thread.start(); } }
    public void pause() { isRunning = false; try { if (thread != null) thread.join(); } catch (InterruptedException e) { Log.e("GameView", "Pause error", e); } }
    @Override public void surfaceCreated(@NonNull SurfaceHolder h) { resume(); }
    @Override public void surfaceChanged(@NonNull SurfaceHolder h, int f, int w, int h1) {
        this.screenWidth = w; this.screenHeight = h1;
        this.renderer = new GameRenderer(getContext(), w, h1);
    }
    @Override public void surfaceDestroyed(@NonNull SurfaceHolder h) { pause(); }
}
