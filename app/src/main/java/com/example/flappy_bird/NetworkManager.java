package com.example.flappy_bird;

import android.content.Context;
import android.content.SharedPreferences;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import java.util.List;

/**
 * Třída zajišťující konfiguraci a poskytování rozhraní pro síťovou komunikaci.
 * Využívá knihovnu Retrofit pro transformaci HTTP požadavků na Java rozhraní.
 */
public class NetworkManager {

    // Základní URL adresa Django serveru běžícího v lokální síti.
    // Pro komunikaci z fyzického zařízení je nutné použít hostname (.local) nebo aktuální IP adresu.
    private static String baseUrl = "http://MarekLenovo.local:8000/";

    // Statická instance API rozhraní pro globální přístup v rámci aplikace.
    private static FlappyApi api;

    /**
     * Rozhraní definující dostupné endpointy na serveru.
     * Anotace určují typ HTTP metody a relativní cestu k endpointu.
     */
    public interface FlappyApi {
        // Získání seznamu všech hráčů a jejich rekordů.
        @GET("players/")
        Call<List<PlayerModel>> getPlayers();

        // Přidání nového hráče do databáze serveru.
        @POST("players/add/")
        Call<Void> addPlayer(@Body PlayerModel player);

        // Aktualizace nejvyššího dosaženého skóre pro konkrétního hráče a obtížnost.
        @POST("players/update_score/")
        Call<Void> updateScore(@Body ScoreUpdateModel update);

        // Odstranění hráče z databáze na základě jména.
        @POST("players/delete/")
        Call<Void> deletePlayer(@Body PlayerModel player);
    }

    /**
     * Inicializuje adresu serveru z paměti telefonu.
     */
    public static void init(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("FlappyBirdPrefs", Context.MODE_PRIVATE);
        baseUrl = prefs.getString("server_url", "http://MarekLenovo.local:8000/");
        api = null; // Vynutíme znovuvytvoření při příštím volání getApi.
    }

    /**
     * Aktualizuje adresu serveru na základě zadaného hostitele (IP nebo hostname).
     * Automaticky doplňuje protokol http:// a port :8000.
     * 
     * @param context Kontext aplikace pro přístup k SharedPreferences.
     * @param host Zadáný text (např. "192.168.1.15" nebo "MarekLenovo.local").
     */
    public static void updateBaseUrl(Context context, String host) {
        String formattedUrl = host;
        
        // Odstranění případného stávajícího protokolu pro jednotné zpracování.
        if (formattedUrl.startsWith("http://")) formattedUrl = formattedUrl.replace("http://", "");
        if (formattedUrl.startsWith("https://")) formattedUrl = formattedUrl.replace("https://", "");
        
        // Odstranění lomítka na konci, pokud existuje.
        if (formattedUrl.endsWith("/")) formattedUrl = formattedUrl.substring(0, formattedUrl.length() - 1);
        
        // Automatické doplnění portu 8000, pokud v řetězci chybí dvojtečka.
        if (!formattedUrl.contains(":")) {
            formattedUrl += ":8000";
        }
        
        // Sestavení finální URL adresy s protokolem a koncovým lomítkem.
        baseUrl = "http://" + formattedUrl + "/";
        
        // Uložení nové adresy do paměti a resetování instance Retrofit.
        SharedPreferences prefs = context.getSharedPreferences("FlappyBirdPrefs", Context.MODE_PRIVATE);
        prefs.edit().putString("server_url", baseUrl).apply();
        api = null;
    }

    /**
     * Vrátí aktuálně nastavenou základní URL adresu serveru.
     */
    public static String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Inicializuje a vrací instanci síťového rozhraní.
     * Používá Singleton pattern k zajištění existence pouze jedné instance Retrofit klienta.
     * 
     * @return Inicializované rozhraní FlappyApi.
     */
    public static FlappyApi getApi() {
        if (api == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create()) // Automatický převod JSON na Java objekty.
                    .build();
            api = retrofit.create(FlappyApi.class);
        }
        return api;
    }

    /**
     * Datový model reprezentující entitu hráče v databázi.
     */
    public static class PlayerModel {
        public String name;
        public int score_easy, score_normal, score_hard;
        
        public PlayerModel(String name) {
            this.name = name;
        }
    }

    /**
     * Pomocný model pro odesílání dat o aktualizaci skóre.
     */
    public static class ScoreUpdateModel {
        public String name;
        public int difficulty;
        public int score;

        public ScoreUpdateModel(String name, int difficulty, int score) {
            this.name = name;
            this.difficulty = difficulty;
            this.score = score;
        }
    }
}
