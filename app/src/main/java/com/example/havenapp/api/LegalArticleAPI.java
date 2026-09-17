package com.example.havenapp.api;

import com.example.havenapp.model.LegalArticle;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;


public interface LegalArticleAPI {


    // =====================================================
    // GET ALL LEGAL RIGHTS ARTICLES
    // =====================================================

    @GET(
            "rest/v1/legal_articles" +
                    "?select=*" +
                    "&category=eq.Legal%20Rights" +
                    "&order=published_at.desc"
    )
    Call<List<LegalArticle>> getLegalArticles(
            @Header("Authorization")
            String authorization
    );


    // =====================================================
    // GET ONE ARTICLE BY ID
    // =====================================================

    @GET("rest/v1/legal_articles")
    Call<List<LegalArticle>> getArticleById(

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