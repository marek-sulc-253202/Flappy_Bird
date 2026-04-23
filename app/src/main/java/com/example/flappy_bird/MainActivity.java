package com.example.flappy_bird;

import android.graphics.Color;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private GameView gameView; // Herní plocha pro zobrazení.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this); // Nastavení na celou obrazovku.

        // Nastavení černé barvy pro horní lištu (Status Bar).
        getWindow().setStatusBarColor(Color.BLACK);
        
        gameView = new GameView(this); // Vytvoření instance naší hry.
        setContentView(gameView); // Nastavíme hru jako hlavní obsah okna.
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.resume(); // Při vrácení do aplikace => spuštění hry.
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.pause(); // Stopnutí hry při opuštění aplikace.
    }
}
