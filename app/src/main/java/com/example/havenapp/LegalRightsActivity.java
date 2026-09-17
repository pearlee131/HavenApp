package com.example.havenapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.havenapp.adapter.LegalArticleAdapter;
import com.example.havenapp.api.LegalArticleAPI;
import com.example.havenapp.api.SupabaseClient;
import com.example.havenapp.model.LegalArticle;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class LegalRightsActivity extends AppCompatActivity {


    // =====================================================
    // TAG
    // =====================================================

    private static final String TAG =
            "LegalRightsActivity";


    // =====================================================
    // UI
    // =====================================================

    private RecyclerView recyclerLegalArticles;

    private ProgressBar progressLegalArticles;

    private View layoutLegalEmpty;


    // =====================================================
    // ARTICLES
    // =====================================================

    private final List<LegalArticle> legalArticles =
            new ArrayList<>();

    private LegalArticleAdapter legalArticleAdapter;

    private LegalArticleAPI legalArticleAPI;


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
                R.layout.activity_legal_rights
        );


        // -------------------------------------------------
        // BACK BUTTON
        // -------------------------------------------------

        setupBackButton();


        // -------------------------------------------------
        // LEGAL ARTICLES
        // -------------------------------------------------

        setupLegalArticles();
    }


    // =====================================================
    // BACK BUTTON
    // =====================================================

    private void setupBackButton() {

        findViewById(
                R.id.btnBack
        ).setOnClickListener(v ->

                finish()
        );
    }


    // =====================================================
    // SETUP LEGAL ARTICLES
    // =====================================================

    private void setupLegalArticles() {


        // -------------------------------------------------
        // FIND VIEWS
        // -------------------------------------------------

        recyclerLegalArticles =
                findViewById(
                        R.id.recyclerLegalArticles
                );


        progressLegalArticles =
                findViewById(
                        R.id.progressLegalArticles
                );


        layoutLegalEmpty =
                findViewById(
                        R.id.layoutLegalEmpty
                );


        // -------------------------------------------------
        // RECYCLER VIEW
        // -------------------------------------------------

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(
                        this,
                        LinearLayoutManager.VERTICAL,
                        false
                );


        recyclerLegalArticles.setLayoutManager(
                layoutManager
        );


        recyclerLegalArticles.setNestedScrollingEnabled(
                false
        );


        // -------------------------------------------------
        // ADAPTER
        // -------------------------------------------------

        legalArticleAdapter =
                new LegalArticleAdapter(
                        legalArticles,
                        article -> {

                            /*
                             * The selected article ID is passed
                             * to the details screen.
                             *
                             * We will create
                             * LegalArticleDetailsActivity next.
                             */

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
                                        "Selected article has no ID"
                                );

                                return;
                            }


                            Intent intent =
                                    new Intent(
                                            LegalRightsActivity.this,
                                            LegalArticleDetailsActivity.class
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


        recyclerLegalArticles.setAdapter(
                legalArticleAdapter
        );


        // -------------------------------------------------
        // SUPABASE API
        // -------------------------------------------------

        legalArticleAPI =
                SupabaseClient
                        .getClient()
                        .create(
                                LegalArticleAPI.class
                        );


        // -------------------------------------------------
        // INITIAL LOAD
        // -------------------------------------------------

        loadLegalArticles();
    }


    // =====================================================
    // LOAD LEGAL ARTICLES
    // =====================================================

    private void loadLegalArticles() {


        // -------------------------------------------------
        // API CHECK
        // -------------------------------------------------

        if (legalArticleAPI == null) {

            Log.e(
                    TAG,
                    "LEGAL ARTICLES: API is null"
            );


            showEmptyState();

            return;
        }


        // -------------------------------------------------
        // GET ACCESS TOKEN
        // -------------------------------------------------

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


        // -------------------------------------------------
        // ACCESS TOKEN CHECK
        // -------------------------------------------------

        if (
                accessToken == null
                        ||
                        accessToken
                                .trim()
                                .isEmpty()
        ) {

            Log.e(
                    TAG,
                    "LEGAL ARTICLES: access token is missing"
            );


            showEmptyState();

            return;
        }


        // -------------------------------------------------
        // SHOW LOADING
        // -------------------------------------------------

        showLoading();


        Log.d(
                TAG,
                "Loading Legal Rights articles..."
        );


        // -------------------------------------------------
        // CALL SUPABASE
        // -------------------------------------------------

        legalArticleAPI
                .getLegalArticles(
                        "Bearer "
                                + accessToken
                )
                .enqueue(

                        new Callback<List<LegalArticle>>() {


                            // =========================================
                            // RESPONSE
                            // =========================================

                            @Override
                            public void onResponse(
                                    Call<List<LegalArticle>> call,
                                    Response<List<LegalArticle>> response
                            ) {


                                // ---------------------------------
                                // ACTIVITY CHECK
                                // ---------------------------------

                                if (
                                        isFinishing()
                                                ||
                                                isDestroyed()
                                ) {

                                    return;
                                }


                                // ---------------------------------
                                // HTTP ERROR
                                // ---------------------------------

                                if (
                                        !response.isSuccessful()
                                ) {

                                    Log.e(
                                            TAG,
                                            "LEGAL ARTICLES HTTP ERROR: "
                                                    + response.code()
                                    );


                                    showEmptyState();

                                    return;
                                }


                                // ---------------------------------
                                // BODY
                                // ---------------------------------

                                List<LegalArticle> result =
                                        response.body();


                                if (
                                        result == null
                                ) {

                                    Log.w(
                                            TAG,
                                            "LEGAL ARTICLES response body is null"
                                    );


                                    showEmptyState();

                                    return;
                                }


                                // ---------------------------------
                                // UPDATE ARTICLE LIST
                                // ---------------------------------

                                legalArticles.clear();


                                legalArticles.addAll(
                                        result
                                );


                                // ---------------------------------
                                // EMPTY CHECK
                                // ---------------------------------

                                if (
                                        legalArticles.isEmpty()
                                ) {

                                    Log.d(
                                            TAG,
                                            "No Legal Rights articles found"
                                    );


                                    showEmptyState();

                                    return;
                                }


                                // ---------------------------------
                                // UPDATE RECYCLER VIEW
                                // ---------------------------------

                                if (
                                        legalArticleAdapter != null
                                ) {

                                    legalArticleAdapter
                                            .notifyDataSetChanged();
                                }


                                showArticles();


                                Log.d(
                                        TAG,
                                        "Legal Rights articles loaded: "
                                                + legalArticles.size()
                                );
                            }


                            // =========================================
                            // NETWORK FAILURE
                            // =========================================

                            @Override
                            public void onFailure(
                                    Call<List<LegalArticle>> call,
                                    Throwable t
                            ) {


                                // ---------------------------------
                                // ACTIVITY CHECK
                                // ---------------------------------

                                if (
                                        isFinishing()
                                                ||
                                                isDestroyed()
                                ) {

                                    return;
                                }


                                Log.e(
                                        TAG,
                                        "LEGAL ARTICLES NETWORK FAILURE",
                                        t
                                );


                                /*
                                 * Keep the screen usable even if
                                 * Supabase/network temporarily fails.
                                 */

                                showEmptyState();
                            }
                        }
                );
    }


    // =====================================================
    // SHOW LOADING
    // =====================================================

    private void showLoading() {

        if (progressLegalArticles != null) {

            progressLegalArticles.setVisibility(
                    View.VISIBLE
            );
        }


        if (recyclerLegalArticles != null) {

            recyclerLegalArticles.setVisibility(
                    View.GONE
            );
        }


        if (layoutLegalEmpty != null) {

            layoutLegalEmpty.setVisibility(
                    View.GONE
            );
        }
    }


    // =====================================================
    // SHOW ARTICLES
    // =====================================================

    private void showArticles() {

        if (progressLegalArticles != null) {

            progressLegalArticles.setVisibility(
                    View.GONE
            );
        }


        if (layoutLegalEmpty != null) {

            layoutLegalEmpty.setVisibility(
                    View.GONE
            );
        }


        if (recyclerLegalArticles != null) {

            recyclerLegalArticles.setVisibility(
                    View.VISIBLE
            );
        }
    }


    // =====================================================
    // SHOW EMPTY STATE
    // =====================================================

    private void showEmptyState() {

        if (progressLegalArticles != null) {

            progressLegalArticles.setVisibility(
                    View.GONE
            );
        }


        if (recyclerLegalArticles != null) {

            recyclerLegalArticles.setVisibility(
                    View.GONE
            );
        }


        if (layoutLegalEmpty != null) {

            layoutLegalEmpty.setVisibility(
                    View.VISIBLE
            );
        }
    }
}