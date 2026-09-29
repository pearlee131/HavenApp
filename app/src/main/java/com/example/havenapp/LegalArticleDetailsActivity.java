package com.example.havenapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.example.havenapp.api.LegalArticleAPI;
import com.example.havenapp.api.SupabaseClient;
import com.example.havenapp.model.LegalArticle;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class LegalArticleDetailsActivity
        extends AppCompatActivity {


    // =====================================================
    // TAG / INTENT KEY
    // =====================================================

    private static final String TAG =
            "LegalArticleDetails";

    private static final String EXTRA_ARTICLE_ID =
            "article_id";


    // =====================================================
    // UI
    // =====================================================

    private View progressArticleDetails;

    private NestedScrollView articleDetailsScroll;

    private View layoutArticleError;

    private TextView txtArticleCategory;

    private TextView txtArticleTitle;

    private TextView txtArticleSubtitle;

    private TextView txtArticleSource;

    private TextView txtArticleContent;

    private TextView txtOfficialSourceName;

    private View cardOfficialSource;


    // =====================================================
    // API / DATA
    // =====================================================

    private LegalArticleAPI legalArticleAPI;

    private String articleId;

    private String officialSourceUrl;


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
                R.layout.activity_legal_article_details
        );


        // -------------------------------------------------
        // FIND VIEWS
        // -------------------------------------------------

        setupViews();


        // -------------------------------------------------
        // BACK BUTTON
        // -------------------------------------------------

        setupBackButton();


        // -------------------------------------------------
        // GET ARTICLE ID
        // -------------------------------------------------

        articleId =
                getIntent().getStringExtra(
                        EXTRA_ARTICLE_ID
                );


        if (
                articleId == null
                        ||
                        articleId.trim().isEmpty()
        ) {

            Log.e(
                    TAG,
                    "Article ID is missing"
            );


            showErrorState();

            return;
        }


        // -------------------------------------------------
        // CREATE SUPABASE API
        // -------------------------------------------------

        legalArticleAPI =
                SupabaseClient
                        .getClient()
                        .create(
                                LegalArticleAPI.class
                        );


        // -------------------------------------------------
        // LOAD ARTICLE
        // -------------------------------------------------

        loadArticle();
    }


    // =====================================================
    // SETUP VIEWS
    // =====================================================

    private void setupViews() {

        progressArticleDetails =
                findViewById(
                        R.id.progressArticleDetails
                );


        articleDetailsScroll =
                findViewById(
                        R.id.articleDetailsScroll
                );


        layoutArticleError =
                findViewById(
                        R.id.layoutArticleError
                );


        txtArticleCategory =
                findViewById(
                        R.id.txtArticleCategory
                );


        txtArticleTitle =
                findViewById(
                        R.id.txtArticleTitle
                );


        txtArticleSubtitle =
                findViewById(
                        R.id.txtArticleSubtitle
                );


        txtArticleSource =
                findViewById(
                        R.id.txtArticleSource
                );


        txtArticleContent =
                findViewById(
                        R.id.txtArticleContent
                );


        txtOfficialSourceName =
                findViewById(
                        R.id.txtOfficialSourceName
                );


        cardOfficialSource =
                findViewById(
                        R.id.cardOfficialSource
                );
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
    // LOAD ARTICLE
    // =====================================================

    private void loadArticle() {


        // -------------------------------------------------
        // API CHECK
        // -------------------------------------------------

        if (legalArticleAPI == null) {

            Log.e(
                    TAG,
                    "LEGAL ARTICLE DETAILS: API is null"
            );


            showErrorState();

            return;
        }


        // -------------------------------------------------
        // ACCESS TOKEN
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


        if (
                accessToken == null
                        ||
                        accessToken.trim().isEmpty()
        ) {

            Log.e(
                    TAG,
                    "LEGAL ARTICLE DETAILS: access token is missing"
            );


            showErrorState();

            return;
        }


        // -------------------------------------------------
        // LOADING STATE
        // -------------------------------------------------

        showLoading();


        Log.d(
                TAG,
                "Loading article: "
                        + articleId
        );


        // -------------------------------------------------
        // SUPABASE REQUEST
        // -------------------------------------------------

        legalArticleAPI
                .getArticleById(
                        "Bearer "
                                + accessToken,
                        "*",
                        "eq."
                                + articleId,
                        1
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
                                            "LEGAL ARTICLE DETAILS HTTP ERROR: "
                                                    + response.code()
                                    );


                                    showErrorState();

                                    return;
                                }


                                // ---------------------------------
                                // RESPONSE BODY
                                // ---------------------------------

                                List<LegalArticle> result =
                                        response.body();


                                if (
                                        result == null
                                                ||
                                                result.isEmpty()
                                ) {

                                    Log.w(
                                            TAG,
                                            "Legal article was not found"
                                    );


                                    showErrorState();

                                    return;
                                }


                                // ---------------------------------
                                // GET FIRST / EXACT ARTICLE
                                // ---------------------------------

                                LegalArticle article =
                                        result.get(0);


                                if (article == null) {

                                    showErrorState();

                                    return;
                                }


                                // ---------------------------------
                                // DISPLAY
                                // ---------------------------------

                                displayArticle(
                                        article
                                );


                                showContent();


                                Log.d(
                                        TAG,
                                        "Legal article loaded: "
                                                + article.getTitle()
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

                                if (
                                        isFinishing()
                                                ||
                                                isDestroyed()
                                ) {

                                    return;
                                }


                                Log.e(
                                        TAG,
                                        "LEGAL ARTICLE DETAILS NETWORK FAILURE",
                                        t
                                );


                                showErrorState();
                            }
                        }
                );
    }


    // =====================================================
    // DISPLAY ARTICLE
    // =====================================================

    private void displayArticle(
            LegalArticle article
    ) {


        // -------------------------------------------------
        // TITLE
        // -------------------------------------------------

        String title =
                article.getTitle();


        if (
                title == null
                        ||
                        title.trim().isEmpty()
        ) {

            title =
                    "Legal Rights Article";
        }


        txtArticleTitle.setText(
                title
        );


        // -------------------------------------------------
        // SUBTITLE
        // -------------------------------------------------

        String subtitle =
                article.getSubtitle();


        if (
                subtitle == null
                        ||
                        subtitle.trim().isEmpty()
        ) {

            txtArticleSubtitle.setVisibility(
                    View.GONE
            );

        } else {

            txtArticleSubtitle.setVisibility(
                    View.VISIBLE
            );


            txtArticleSubtitle.setText(
                    subtitle
            );
        }


        // -------------------------------------------------
        // CATEGORY
        // -------------------------------------------------

        String category =
                article.getCategory();


        if (
                category == null
                        ||
                        category.trim().isEmpty()
        ) {

            txtArticleCategory.setVisibility(
                    View.GONE
            );

        } else {

            txtArticleCategory.setVisibility(
                    View.VISIBLE
            );


            txtArticleCategory.setText(
                    category
                            .trim()
                            .toUpperCase(
                                    Locale.getDefault()
                            )
            );
        }


        // -------------------------------------------------
        // ARTICLE CONTENT
        // -------------------------------------------------

        String content =
                article.getContent();


        if (
                content == null
                        ||
                        content.trim().isEmpty()
        ) {

            txtArticleContent.setText(
                    "Article information is currently unavailable."
            );

        } else {

            txtArticleContent.setText(
                    content.trim()
            );
        }


        // -------------------------------------------------
        // SOURCE NAME
        // -------------------------------------------------

        String sourceName =
                article.getSourceName();


        if (
                sourceName == null
                        ||
                        sourceName.trim().isEmpty()
        ) {

            txtArticleSource.setVisibility(
                    View.GONE
            );


            txtOfficialSourceName.setText(
                    "Official source"
            );

        } else {

            txtArticleSource.setVisibility(
                    View.VISIBLE
            );


            txtArticleSource.setText(
                    sourceName
            );


            txtOfficialSourceName.setText(
                    sourceName
            );
        }


        // -------------------------------------------------
        // OFFICIAL SOURCE URL
        // -------------------------------------------------

        officialSourceUrl =
                article.getSourceUrl();


        if (
                officialSourceUrl == null
                        ||
                        officialSourceUrl
                                .trim()
                                .isEmpty()
        ) {

            cardOfficialSource.setVisibility(
                    View.GONE
            );

        } else {

            cardOfficialSource.setVisibility(
                    View.VISIBLE
            );


            cardOfficialSource.setOnClickListener(
                    v -> openOfficialSource()
            );
        }
    }


    // =====================================================
    // OPEN OFFICIAL SOURCE
    // =====================================================

    private void openOfficialSource() {

        if (
                officialSourceUrl == null
                        ||
                        officialSourceUrl
                                .trim()
                                .isEmpty()
        ) {

            return;
        }


        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                    officialSourceUrl
                            )
                    );


            startActivity(
                    intent
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Unable to open official source",
                    e
            );
        }
    }


    // =====================================================
    // SHOW LOADING
    // =====================================================

    private void showLoading() {

        if (progressArticleDetails != null) {

            progressArticleDetails.setVisibility(
                    View.VISIBLE
            );
        }


        if (articleDetailsScroll != null) {

            articleDetailsScroll.setVisibility(
                    View.GONE
            );
        }


        if (layoutArticleError != null) {

            layoutArticleError.setVisibility(
                    View.GONE
            );
        }
    }


    // =====================================================
    // SHOW CONTENT
    // =====================================================

    private void showContent() {

        if (progressArticleDetails != null) {

            progressArticleDetails.setVisibility(
                    View.GONE
            );
        }


        if (layoutArticleError != null) {

            layoutArticleError.setVisibility(
                    View.GONE
            );
        }


        if (articleDetailsScroll != null) {

            articleDetailsScroll.setVisibility(
                    View.VISIBLE
            );


            articleDetailsScroll.scrollTo(
                    0,
                    0
            );
        }
    }


    // =====================================================
    // SHOW ERROR
    // =====================================================

    private void showErrorState() {

        if (progressArticleDetails != null) {

            progressArticleDetails.setVisibility(
                    View.GONE
            );
        }


        if (articleDetailsScroll != null) {

            articleDetailsScroll.setVisibility(
                    View.GONE
            );
        }


        if (layoutArticleError != null) {

            layoutArticleError.setVisibility(
                    View.VISIBLE
            );
        }
    }
}