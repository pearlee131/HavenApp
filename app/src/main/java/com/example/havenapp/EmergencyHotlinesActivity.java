package com.example.havenapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.havenapp.adapter.EmergencyHotlineAdapter;
import com.example.havenapp.api.EmergencyHotlineAPI;
import com.example.havenapp.api.SupabaseClient;
import com.example.havenapp.model.EmergencyHotline;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmergencyHotlinesActivity extends AppCompatActivity {

    private static final String TAG =
            "EmergencyHotlines";

    private RecyclerView recyclerEmergencyHotlines;
    private ProgressBar progressEmergencyHotlines;
    private LinearLayout layoutEmergencyEmpty;

    private final List<EmergencyHotline> hotlineList =
            new ArrayList<>();

    private EmergencyHotlineAdapter adapter;
    private EmergencyHotlineAPI emergencyHotlineAPI;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_emergency_hotlines
        );

        initializeViews();

        setupBackButton();

        setupRecyclerView();

        setupAPI();

        loadEmergencyHotlines();
    }


    // =====================================================
    // INITIALIZE VIEWS
    // =====================================================

    private void initializeViews() {

        recyclerEmergencyHotlines =
                findViewById(
                        R.id.recyclerEmergencyHotlines
                );

        progressEmergencyHotlines =
                findViewById(
                        R.id.progressEmergencyHotlines
                );

        layoutEmergencyEmpty =
                findViewById(
                        R.id.layoutEmergencyEmpty
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
    // RECYCLER VIEW
    // =====================================================

    private void setupRecyclerView() {

        adapter =
                new EmergencyHotlineAdapter(
                        this,
                        hotlineList
                );

        recyclerEmergencyHotlines.setLayoutManager(
                new LinearLayoutManager(
                        this,
                        LinearLayoutManager.VERTICAL,
                        false
                )
        );

        recyclerEmergencyHotlines.setAdapter(
                adapter
        );

        recyclerEmergencyHotlines.setNestedScrollingEnabled(
                false
        );
    }


    // =====================================================
    // API
    // =====================================================

    private void setupAPI() {

        emergencyHotlineAPI =
                SupabaseClient
                        .getClient()
                        .create(
                                EmergencyHotlineAPI.class
                        );
    }


    // =====================================================
    // LOAD HOTLINES
    // =====================================================

    private void loadEmergencyHotlines() {

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


        if (accessToken == null
                || accessToken.trim().isEmpty()) {

            Log.e(
                    TAG,
                    "Access token is missing"
            );

            showEmptyState();

            Toast.makeText(
                    this,
                    "Unable to load emergency hotlines",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        emergencyHotlineAPI
                .getEmergencyHotlines(
                        "Bearer " + accessToken
                )
                .enqueue(
                        new Callback<List<EmergencyHotline>>() {

                            @Override
                            public void onResponse(
                                    Call<List<EmergencyHotline>> call,
                                    Response<List<EmergencyHotline>> response
                            ) {

                                if (isFinishing()
                                        || isDestroyed()) {

                                    return;
                                }


                                Log.d(
                                        TAG,
                                        "Response code: "
                                                + response.code()
                                );


                                if (response.isSuccessful()) {

                                    List<EmergencyHotline> result =
                                            response.body();


                                    hotlineList.clear();


                                    if (result != null) {

                                        hotlineList.addAll(
                                                result
                                        );
                                    }


                                    adapter.notifyDataSetChanged();


                                    if (hotlineList.isEmpty()) {

                                        showEmptyState();

                                    } else {

                                        showContentState();
                                    }


                                    Log.d(
                                            TAG,
                                            "Loaded hotlines: "
                                                    + hotlineList.size()
                                    );

                                } else {

                                    Log.e(
                                            TAG,
                                            "Failed to load hotlines. HTTP "
                                                    + response.code()
                                    );

                                    showEmptyState();

                                    Toast.makeText(
                                            EmergencyHotlinesActivity.this,
                                            "Unable to load emergency hotlines",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            }


                            @Override
                            public void onFailure(
                                    Call<List<EmergencyHotline>> call,
                                    Throwable t
                            ) {

                                if (isFinishing()
                                        || isDestroyed()) {

                                    return;
                                }


                                Log.e(
                                        TAG,
                                        "Hotline request failed",
                                        t
                                );


                                showEmptyState();


                                Toast.makeText(
                                        EmergencyHotlinesActivity.this,
                                        "Unable to load emergency hotlines",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                );
    }


    // =====================================================
    // LOADING STATE
    // =====================================================

    private void showLoadingState() {

        progressEmergencyHotlines.setVisibility(
                View.VISIBLE
        );

        recyclerEmergencyHotlines.setVisibility(
                View.GONE
        );

        layoutEmergencyEmpty.setVisibility(
                View.GONE
        );
    }


    // =====================================================
    // CONTENT STATE
    // =====================================================

    private void showContentState() {

        progressEmergencyHotlines.setVisibility(
                View.GONE
        );

        recyclerEmergencyHotlines.setVisibility(
                View.VISIBLE
        );

        layoutEmergencyEmpty.setVisibility(
                View.GONE
        );
    }


    // =====================================================
    // EMPTY STATE
    // =====================================================

    private void showEmptyState() {

        progressEmergencyHotlines.setVisibility(
                View.GONE
        );

        recyclerEmergencyHotlines.setVisibility(
                View.GONE
        );

        layoutEmergencyEmpty.setVisibility(
                View.VISIBLE
        );
    }
}