package com.example.havenapp.api;

import com.example.havenapp.model.TinigEpisode;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface TinigEpisodeAPI {

    // =====================================================
    // GET LATEST 10 TNK EPISODES
    // Used on Explore page
    // =====================================================

    @GET(
            "rest/v1/tinig_episodes" +
                    "?select=*" +
                    "&order=published_at.desc" +
                    "&limit=10"
    )
    Call<List<TinigEpisode>> getLatestEpisodes(

            @Header("Authorization")
            String authorization
    );


    // =====================================================
    // GET ALL TNK EPISODES
    // Used on See All page
    // =====================================================

    @GET(
            "rest/v1/tinig_episodes" +
                    "?select=*" +
                    "&order=published_at.desc"
    )
    Call<List<TinigEpisode>> getAllEpisodes(

            @Header("Authorization")
            String authorization
    );
}