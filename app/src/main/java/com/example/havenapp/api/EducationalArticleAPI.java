package com.example.havenapp.api;

import com.example.havenapp.model.EducationalArticle;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;


public interface EducationalArticleAPI {


    // =====================================================
    // GET ALL EDUCATIONAL ARTICLES
    // =====================================================

    @GET(
            "rest/v1/educational_articles" +
                    "?select=*" +
                    "&category=eq.Educational%20Articles" +
                    "&order=published_at.desc"
    )
    Call<List<EducationalArticle>> getEducationalArticles(
            @Header("Authorization")
            String authorization
    );


    // =====================================================
    // GET ONE EDUCATIONAL ARTICLE BY ID
    // =====================================================

    @GET("rest/v1/educational_articles")
    Call<List<EducationalArticle>> getArticleById(

            @Header("Authorization")
            String authorization,

            @Query("select")
            String select,

            @Query("id")
            String articleId,

            @Query("limit")
            int limit
    );
}