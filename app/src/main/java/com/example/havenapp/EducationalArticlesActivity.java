package com.example.havenapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.havenapp.adapter.EducationalArticleAdapter;
import com.example.havenapp.api.EducationalArticleAPI;
import com.example.havenapp.api.SupabaseClient;
import com.example.havenapp.model.EducationalArticle;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class EducationalArticlesActivity
        extends AppCompatActivity {


    // =====================================================
    // TAG
    // =====================================================

    private static final String TAG =
            "EducationalArticles";


    // =====================================================
    // VIEWS
    // =====================================================

    private RecyclerView recyclerEducationalArticles;

    private View progressEducationalArticles;

    private View layoutEducationalEmpty;

    private View cardHopeAssistant;


    // =====================================================
    // DATA
    // =====================================================

    private final List<EducationalArticle>
            educationalArticleList =
            new ArrayList<>();


    private EducationalArticleAdapter
            educationalArticleAdapter;


    // =====================================================
    // API
    // =====================================================

    private EducationalArticleAPI
            educationalArticleAPI;


    // =====================================================
    // ON CREATE
    // =====================================================

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(
                savedInstanceState
        );


        setContentView(
                R.layout.activity_educational_articles
        );


        initializeViews();

        setupBackButton();

        setupRecyclerView();

        setupAPI();

        setupHopeAssistant();

        loadEducationalArticles();
    }


    // =====================================================
    // INITIALIZE VIEWS
    // =====================================================

    private void initializeViews() {

        recyclerEducationalArticles =
                findViewById(
                        R.id.recyclerEducationalArticles
                );


        progressEducationalArticles =
                findViewById(
                        R.id.progressEducationalArticles
                );


        layoutEducationalEmpty =
                findViewById(
                        R.id.layoutEducationalEmpty
                );


        cardHopeAssistant =
                findViewById(
                        R.id.cardHopeAssistant
                );
    }


    // =====================================================
    // BACK BUTTON
    // =====================================================

    private void setupBackButton() {

        findViewById(
                R.id.btnBack
        ).setOnClickListener(v -> {

            finish();
        });
    }


    // =====================================================
    // HOPE ASSISTANT
    // =====================================================

    private void setupHopeAssistant() {

        if (cardHopeAssistant == null) {

            return;
        }


        ChatbotFloatingHelper.setup(
                this,
                cardHopeAssistant
        );
    }


    // =====================================================
    // RECYCLER VIEW
    // =====================================================

    private void setupRecyclerView() {

        recyclerEducationalArticles
                .setLayoutManager(
                        new LinearLayoutManager(
                                this,
                                LinearLayoutManager.VERTICAL,
                                false
                        )
                );


        educationalArticleAdapter =
                new EducationalArticleAdapter(
                        educationalArticleList,
                        article -> {

                            if (
                                    article == null
                                            ||
                                            article.getId() == null
                                            ||
                                            article.getId()
                                                    .trim()
                                                    .isEmpty()
                            ) {

                                Log.e(
                                        TAG,
                                        "ARTICLE ID IS EMPTY"
                                );

                                return;
                            }


                            /*
                             * The details activity will be
                             * created in the next step.
                             */
                            Intent intent =
                                    new Intent(
                                            EducationalArticlesActivity.this,
                                            EducationalArticleDetailsActivity.class
                                    );


                            intent.putExtra(
                                    "article_id",
                                    article.getId()
                            );


                            startActivity(
                                    intent
                            );
                        }
                );


        recyclerEducationalArticles
                .setAdapter(
                        educationalArticleAdapter
                );


        recyclerEducationalArticles
                .setNestedScrollingEnabled(
                        false
                );
    }


    // =====================================================
    // SETUP API
    // =====================================================

    private void setupAPI() {

        educationalArticleAPI =
                SupabaseClient
                        .getClient()
                        .create(
                                EducationalArticleAPI.class
                        );
    }


    // =====================================================
    // LOAD EDUCATIONAL ARTICLES
    // =====================================================

    private void loadEducationalArticles() {

        showLoadingState();


        SharedPreferences preferences =
                getSharedPreferences(
                        "HavenUser",
                        MODE_PRIVATE
                );


        String accessToken =
                preferences.getString(
                        "access_token",
                        null
                );


        if (
                accessToken == null
                        ||
                        accessToken.trim().isEmpty()
        ) {

            Log.e(
                    TAG,
                    "ACCESS TOKEN IS EMPTY"
            );


            showEmptyState();

            return;
        }


        if (
                educationalArticleAPI == null
        ) {

            Log.e(
                    TAG,
                    "EDUCATIONAL ARTICLE API IS NULL"
            );


            showEmptyState();

            return;
        }


        educationalArticleAPI
                .getEducationalArticles(
                        "Bearer " + accessToken
                )
                .enqueue(
                        new Callback<List<EducationalArticle>>() {


                            // =================================
                            // RESPONSE
                            // =================================

                            @Override
                            public void onResponse(
                                    Call<List<EducationalArticle>> call,
                                    Response<List<EducationalArticle>> response
                            ) {

                                if (
                                        response.isSuccessful()
                                ) {

                                    List<EducationalArticle>
                                            articles =
                                            response.body();


                                    educationalArticleList
                                            .clear();


                                    if (
                                            articles != null
                                                    &&
                                                    !articles.isEmpty()
                                    ) {

                                        educationalArticleList
                                                .addAll(
                                                        articles
                                                );


                                        educationalArticleAdapter
                                                .notifyDataSetChanged();


                                        showArticlesState();


                                        Log.d(
                                                TAG,
                                                "Educational articles loaded: "
                                                        +
                                                        educationalArticleList.size()
                                        );

                                    } else {

                                        educationalArticleAdapter
                                                .notifyDataSetChanged();


                                        showEmptyState();


                                        Log.d(
                                                TAG,
                                                "No educational articles found"
                                        );
                                    }

                                } else {

                                    Log.e(
                                            TAG,
                                            "LOAD ARTICLES FAILED. CODE: "
                                                    +
                                                    response.code()
                                    );


                                    showEmptyState();
                                }
                            }


                            // =================================
                            // NETWORK FAILURE
                            // =================================

                            @Override
                            public void onFailure(
                                    Call<List<EducationalArticle>> call,
                                    Throwable t
                            ) {

                                Log.e(
                                        TAG,
                                        "LOAD EDUCATIONAL ARTICLES NETWORK FAILURE",
                                        t
                                );


                                showEmptyState();
                            }
                        }
                );
    }


    // =====================================================
    // LOADING STATE
    // =====================================================

    private void showLoadingState() {

        progressEducationalArticles
                .setVisibility(
                        View.VISIBLE
                );


        recyclerEducationalArticles
                .setVisibility(
                        View.GONE
                );


        layoutEducationalEmpty
                .setVisibility(
                        View.GONE
                );
    }


    // =====================================================
    // ARTICLES STATE
    // =====================================================

    private void showArticlesState() {

        progressEducationalArticles
                .setVisibility(
                        View.GONE
                );


        layoutEducationalEmpty
                .setVisibility(
                        View.GONE
                );


        recyclerEducationalArticles
                .setVisibility(
                        View.VISIBLE
                );
    }


    // =====================================================
    // EMPTY STATE
    // =====================================================

    private void showEmptyState() {

        progressEducationalArticles
                .setVisibility(
                        View.GONE
                );


        recyclerEducationalArticles
                .setVisibility(
                        View.GONE
                );


        layoutEducationalEmpty
                .setVisibility(
                        View.VISIBLE
                );
    }
}