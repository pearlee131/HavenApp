package com.example.havenapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.ImageViewCompat;
import androidx.core.widget.NestedScrollView;

import com.bumptech.glide.Glide;
import com.example.havenapp.api.EducationalArticleAPI;
import com.example.havenapp.api.SupabaseClient;
import com.example.havenapp.model.EducationalArticle;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class EducationalArticleDetailsActivity
        extends AppCompatActivity {


    // =====================================================
    // TAG
    // =====================================================

    private static final String TAG =
            "EducationalDetails";


    // =====================================================
    // VIEWS
    // =====================================================

    private View progressArticleDetails;

    private NestedScrollView articleDetailsScroll;

    private View layoutArticleError;

    private ImageView imgArticleHero;

    private TextView txtArticleCategory;

    private TextView txtArticleTitle;

    private TextView txtArticleSubtitle;

    private TextView txtArticleSource;

    private TextView txtArticleContent;

    private TextView txtOfficialSourceName;

    private View cardOfficialSource;


    // =====================================================
    // API
    // =====================================================

    private EducationalArticleAPI
            educationalArticleAPI;


    // =====================================================
    // ARTICLE
    // =====================================================

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
                R.layout.activity_educational_article_details
        );


        initializeViews();

        setupBackButton();

        setupOfficialSource();

        setupAPI();

        getArticleId();

        loadArticle();
    }


    // =====================================================
    // INITIALIZE VIEWS
    // =====================================================

    private void initializeViews() {

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


        imgArticleHero =
                findViewById(
                        R.id.imgArticleHero
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
        ).setOnClickListener(v -> {

            finish();
        });
    }


    // =====================================================
    // OFFICIAL SOURCE CLICK
    // =====================================================

    private void setupOfficialSource() {

        cardOfficialSource
                .setOnClickListener(v -> {

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

                    } catch (
                            Exception e
                    ) {

                        Log.e(
                                TAG,
                                "FAILED TO OPEN OFFICIAL SOURCE",
                                e
                        );
                    }
                });
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
    // GET ARTICLE ID
    // =====================================================

    private void getArticleId() {

        Intent intent =
                getIntent();


        if (
                intent != null
        ) {

            articleId =
                    intent.getStringExtra(
                            "article_id"
                    );
        }
    }


    // =====================================================
    // LOAD ARTICLE
    // =====================================================

    private void loadArticle() {

        showLoadingState();


        if (
                articleId == null
                        ||
                        articleId.trim().isEmpty()
        ) {

            Log.e(
                    TAG,
                    "ARTICLE ID IS EMPTY"
            );


            showErrorState();

            return;
        }


        if (
                educationalArticleAPI == null
        ) {

            Log.e(
                    TAG,
                    "EDUCATIONAL ARTICLE API IS NULL"
            );


            showErrorState();

            return;
        }


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


            showErrorState();

            return;
        }


        educationalArticleAPI
                .getArticleById(
                        "Bearer " + accessToken,
                        "*",
                        "eq." + articleId,
                        1
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
                                        !response.isSuccessful()
                                ) {

                                    Log.e(
                                            TAG,
                                            "LOAD ARTICLE FAILED. CODE: "
                                                    +
                                                    response.code()
                                    );


                                    showErrorState();

                                    return;
                                }


                                List<EducationalArticle>
                                        articles =
                                        response.body();


                                if (
                                        articles == null
                                                ||
                                                articles.isEmpty()
                                ) {

                                    Log.e(
                                            TAG,
                                            "ARTICLE NOT FOUND"
                                    );


                                    showErrorState();

                                    return;
                                }


                                EducationalArticle article =
                                        articles.get(
                                                0
                                        );


                                bindArticle(
                                        article
                                );


                                showContentState();
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
                                        "LOAD ARTICLE NETWORK FAILURE",
                                        t
                                );


                                showErrorState();
                            }
                        }
                );
    }


    // =====================================================
    // BIND ARTICLE
    // =====================================================

    private void bindArticle(
            EducationalArticle article
    ) {

        if (
                article == null
        ) {

            showErrorState();

            return;
        }


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
                    "Educational Article";
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

            txtArticleSubtitle
                    .setVisibility(
                            View.GONE
                    );

        } else {

            txtArticleSubtitle
                    .setVisibility(
                            View.VISIBLE
                    );


            txtArticleSubtitle
                    .setText(
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

            category =
                    "Educational Articles";
        }


        txtArticleCategory
                .setText(
                        category
                                .toUpperCase(
                                        Locale.getDefault()
                                )
                );


        // -------------------------------------------------
        // CONTENT
        // -------------------------------------------------

        String content =
                article.getContent();


        if (
                content == null
                        ||
                        content.trim().isEmpty()
        ) {

            content =
                    "Educational content is currently unavailable.";
        }


        txtArticleContent
                .setText(
                        content
                );


        // -------------------------------------------------
        // SOURCE
        // -------------------------------------------------

        String sourceName =
                article.getSourceName();


        if (
                sourceName == null
                        ||
                        sourceName.trim().isEmpty()
        ) {

            sourceName =
                    "Official Source";
        }


        txtArticleSource
                .setText(
                        "Source: "
                                +
                                sourceName
                );


        txtOfficialSourceName
                .setText(
                        sourceName
                );


        // -------------------------------------------------
        // SOURCE URL
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

            cardOfficialSource
                    .setVisibility(
                            View.GONE
                    );

        } else {

            cardOfficialSource
                    .setVisibility(
                            View.VISIBLE
                    );
        }


        // -------------------------------------------------
        // THUMBNAIL
        // -------------------------------------------------

        loadThumbnail(
                article.getThumbnailUrl()
        );
    }


    // =====================================================
    // LOAD THUMBNAIL
    // =====================================================

    private void loadThumbnail(
            String thumbnailUrl
    ) {

        if (
                thumbnailUrl != null
                        &&
                        !thumbnailUrl
                                .trim()
                                .isEmpty()
        ) {

            /*
             * Remove the purple tint before displaying
             * a real image.
             */
            ImageViewCompat.setImageTintList(
                    imgArticleHero,
                    null
            );


            imgArticleHero
                    .setPadding(
                            0,
                            0,
                            0,
                            0
                    );


            imgArticleHero
                    .setScaleType(
                            ImageView.ScaleType.CENTER_CROP
                    );


            Glide.with(
                            this
                    )
                    .load(
                            thumbnailUrl
                    )
                    .error(
                            R.drawable.ic_articles
                    )
                    .into(
                            imgArticleHero
                    );

        } else {

            showDefaultThumbnail();
        }
    }


    // =====================================================
    // DEFAULT THUMBNAIL
    // =====================================================

    private void showDefaultThumbnail() {

        imgArticleHero
                .setImageResource(
                        R.drawable.ic_articles
                );


        ImageViewCompat.setImageTintList(
                imgArticleHero,
                ColorStateList.valueOf(
                        Color.parseColor(
                                "#6C2BD9"
                        )
                )
        );


        int padding =
                (int) (
                        45
                                *
                                getResources()
                                        .getDisplayMetrics()
                                        .density
                );


        imgArticleHero
                .setPadding(
                        padding,
                        padding,
                        padding,
                        padding
                );


        imgArticleHero
                .setScaleType(
                        ImageView.ScaleType.CENTER_INSIDE
                );
    }


    // =====================================================
    // LOADING STATE
    // =====================================================

    private void showLoadingState() {

        progressArticleDetails
                .setVisibility(
                        View.VISIBLE
                );


        articleDetailsScroll
                .setVisibility(
                        View.GONE
                );


        layoutArticleError
                .setVisibility(
                        View.GONE
                );
    }


    // =====================================================
    // CONTENT STATE
    // =====================================================

    private void showContentState() {

        progressArticleDetails
                .setVisibility(
                        View.GONE
                );


        layoutArticleError
                .setVisibility(
                        View.GONE
                );


        articleDetailsScroll
                .setVisibility(
                        View.VISIBLE
                );
    }


    // =====================================================
    // ERROR STATE
    // =====================================================

    private void showErrorState() {

        progressArticleDetails
                .setVisibility(
                        View.GONE
                );


        articleDetailsScroll
                .setVisibility(
                        View.GONE
                );


        layoutArticleError
                .setVisibility(
                        View.VISIBLE
                );
    }
}