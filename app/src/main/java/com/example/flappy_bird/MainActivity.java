package com.example.flappy_bird;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private GameView gameView; // Herní plocha pro zobrazení.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // Nastavení na celou obrazovku.
        
        gameView = new GameView(this); // Vytvoření instance hry.
        setContentView(gameView); // Nastevení hry jako hlavní obsah okna.
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
