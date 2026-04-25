package com.example.flappy_bird;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import java.util.List;

public class NetworkManager {
    // SEM ZAPIŠ IP ADRESU SVÉHO NOTEBOOKU (např. http://192.168.1.15:8000/)
    // 10.0.2.2 funguje pouze pro Android Emulátor (ukazuje na localhost tvého PC)
    private static final String BASE_URL = "http://192.168.1.20:8000/";

    private static FlappyApi api;

    public interface FlappyApi {
        @GET("players/")
        Call<List<PlayerModel>> getPlayers();

        @POST("players/add/")
        Call<Void> addPlayer(@Body PlayerModel player);

        @POST("players/update_score/")
        Call<Void> updateScore(@Body ScoreUpdateModel update);

        @POST("players/delete/")
        Call<Void> deletePlayer(@Body PlayerModel player);
    }

    public static FlappyApi getApi() {
        if (api == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            api = retrofit.create(FlappyApi.class);
        }
        return api;
    }

    public static class PlayerModel {
        public String name;
        public int score_easy, score_normal, score_hard;
        
        public PlayerModel(String name) {
            this.name = name;
        }
    }

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
