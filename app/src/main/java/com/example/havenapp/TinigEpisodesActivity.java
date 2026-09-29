package com.example.havenapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.havenapp.adapter.TinigAllEpisodeAdapter;
import com.example.havenapp.api.SupabaseClient;
import com.example.havenapp.api.TinigEpisodeAPI;
import com.example.havenapp.model.TinigEpisode;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TinigEpisodesActivity extends AppCompatActivity {

    // =====================================================
    // VIEWS
    // =====================================================

    /*
     * IMPORTANT:
     *
     * btnBack is a MaterialCardView because the XML uses
     * a MaterialCardView for the My Reports-style
     * exit/back button.
     */
    private MaterialCardView btnBack;

    private TextInputEditText edtSearchEpisodes;

    private RecyclerView recyclerAllEpisodes;
    private RecyclerView recyclerYearFilters;

    private TextView txtEpisodeCount;
    private TextView txtEmptyEpisodes;

    /*
     * FLOATING HOPE ASSISTANT
     */
    private View cardHopeAssistant;


    // =====================================================
    // DATA
    // =====================================================

    private final List<TinigEpisode> allEpisodes =
            new ArrayList<>();

    private final List<TinigEpisode> filteredEpisodes =
            new ArrayList<>();


    // =====================================================
    // ADAPTERS
    // =====================================================

    private TinigAllEpisodeAdapter episodeAdapter;

    private YearFilterAdapter yearFilterAdapter;


    // =====================================================
    // API
    // =====================================================

    private TinigEpisodeAPI tinigEpisodeAPI;


    // =====================================================
    // FILTER STATE
    // =====================================================

    private String selectedYear = "All";

    private String currentSearch = "";


    // =====================================================
    // ACTIVITY
    // =====================================================

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(
                savedInstanceState
        );

        setContentView(
                R.layout.activity_tinig_episodes
        );


        // =================================================
        // INITIALIZE VIEWS
        // =================================================

        initializeViews();


        // =================================================
        // API
        // =================================================

        setupApi();


        // =================================================
        // BACK BUTTON
        // =================================================

        setupBackButton();


        // =================================================
        // EPISODE RECYCLER
        // =================================================

        setupEpisodeRecycler();


        // =================================================
        // YEAR FILTER RECYCLER
        // =================================================

        setupYearRecycler();


        // =================================================
        // SEARCH
        // =================================================

        setupSearch();


        // =================================================
        // FLOATING HOPE ASSISTANT
        // Same implementation used by ExploreActivity
        // =================================================

        setupHopeAssistant();


        // =================================================
        // LOAD EPISODES
        // =================================================

        loadAllEpisodes();
    }


    // =====================================================
    // INITIALIZE VIEWS
    // =====================================================

    private void initializeViews() {

        btnBack =
                findViewById(
                        R.id.btnBack
                );

        edtSearchEpisodes =
                findViewById(
                        R.id.edtSearchEpisodes
                );

        recyclerAllEpisodes =
                findViewById(
                        R.id.recyclerAllEpisodes
                );

        recyclerYearFilters =
                findViewById(
                        R.id.recyclerYearFilters
                );

        txtEpisodeCount =
                findViewById(
                        R.id.txtEpisodeCount
                );

        txtEmptyEpisodes =
                findViewById(
                        R.id.txtEmptyEpisodes
                );


        // =================================================
        // HOPE ASSISTANT
        // =================================================

        cardHopeAssistant =
                findViewById(
                        R.id.cardHopeAssistant
                );
    }


    // =====================================================
    // HOPE ASSISTANT
    // =====================================================

    private void setupHopeAssistant() {

        View floatingChatbot =
                findViewById(
                        R.id.cardHopeAssistant
                );

        if (floatingChatbot == null) {

            return;
        }

        ChatbotFloatingHelper.setup(
                this,
                floatingChatbot
        );
    }


    // =====================================================
    // SETUP API
    // =====================================================

    private void setupApi() {

        tinigEpisodeAPI =
                SupabaseClient
                        .getClient()
                        .create(
                                TinigEpisodeAPI.class
                        );
    }


    // =====================================================
    // BACK BUTTON
    // =====================================================

    private void setupBackButton() {

        btnBack.setOnClickListener(
                v -> finish()
        );
    }


    // =====================================================
    // EPISODE RECYCLER
    // =====================================================

    private void setupEpisodeRecycler() {

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(
                        this,
                        LinearLayoutManager.VERTICAL,
                        false
                );

        recyclerAllEpisodes.setLayoutManager(
                layoutManager
        );

        recyclerAllEpisodes.setHasFixedSize(
                false
        );

        recyclerAllEpisodes.setNestedScrollingEnabled(
                true
        );

        episodeAdapter =
                new TinigAllEpisodeAdapter(
                        this,
                        filteredEpisodes
                );

        recyclerAllEpisodes.setAdapter(
                episodeAdapter
        );
    }


    // =====================================================
    // YEAR FILTER RECYCLER
    // =====================================================

    private void setupYearRecycler() {

        recyclerYearFilters.setLayoutManager(
                new LinearLayoutManager(
                        this,
                        LinearLayoutManager.HORIZONTAL,
                        false
                )
        );

        yearFilterAdapter =
                new YearFilterAdapter(
                        new ArrayList<>(),
                        year -> {

                            selectedYear = year;

                            applyFilters();
                        }
                );

        recyclerYearFilters.setAdapter(
                yearFilterAdapter
        );
    }


    // =====================================================
    // SEARCH
    // =====================================================

    private void setupSearch() {

        edtSearchEpisodes.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {

                    }


                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        currentSearch =
                                s == null
                                        ? ""
                                        : s.toString()
                                          .trim()
                                          .toLowerCase(
                                                  Locale.ROOT
                                          );

                        applyFilters();
                    }


                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {

                    }
                }
        );
    }


    // =====================================================
    // LOAD ALL EPISODES FROM SUPABASE
    // =====================================================

    private void loadAllEpisodes() {

        SharedPreferences preferences =
                getSharedPreferences(
                        "HavenUser",
                        MODE_PRIVATE
                );

        String accessToken =
                preferences.getString(
                        "access_token",
                        ""
                );

        if (accessToken == null
                || accessToken.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Unable to load episodes. Please log in again.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        tinigEpisodeAPI
                .getAllEpisodes(
                        "Bearer " + accessToken
                )
                .enqueue(
                        new Callback<List<TinigEpisode>>() {

                            @Override
                            public void onResponse(
                                    Call<List<TinigEpisode>> call,
                                    Response<List<TinigEpisode>> response
                            ) {

                                if (isFinishing()
                                        || isDestroyed()) {

                                    return;
                                }

                                if (!response.isSuccessful()) {

                                    Toast.makeText(
                                            TinigEpisodesActivity.this,
                                            "Unable to load episodes.",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    return;
                                }

                                List<TinigEpisode> result =
                                        response.body();

                                allEpisodes.clear();

                                if (result != null) {

                                    for (
                                            TinigEpisode episode
                                            : result
                                    ) {

                                        if (episode != null) {

                                            allEpisodes.add(
                                                    episode
                                            );
                                        }
                                    }
                                }

                                buildYearFilters();

                                applyFilters();
                            }


                            @Override
                            public void onFailure(
                                    Call<List<TinigEpisode>> call,
                                    Throwable t
                            ) {

                                if (isFinishing()
                                        || isDestroyed()) {

                                    return;
                                }

                                Toast.makeText(
                                        TinigEpisodesActivity.this,
                                        "Unable to connect. Please check your internet connection.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                );
    }


    // =====================================================
    // BUILD YEAR FILTERS
    // =====================================================

    private void buildYearFilters() {

        List<String> years =
                new ArrayList<>();

        years.add(
                "All"
        );

        Set<String> uniqueYears =
                new LinkedHashSet<>();

        for (TinigEpisode episode : allEpisodes) {

            if (episode == null) {

                continue;
            }

            String year =
                    extractYear(
                            episode.getPublished_at()
                    );

            if (!year.isEmpty()) {

                uniqueYears.add(
                        year
                );
            }
        }

        years.addAll(
                uniqueYears
        );


        /*
         * If the database is temporarily empty,
         * still show useful year options.
         */
        if (years.size() == 1) {

            int currentYear =
                    Calendar
                            .getInstance()
                            .get(
                                    Calendar.YEAR
                            );

            for (int i = 0; i < 4; i++) {

                years.add(
                        String.valueOf(
                                currentYear - i
                        )
                );
            }
        }

        yearFilterAdapter.updateYears(
                years
        );
    }


    // =====================================================
    // APPLY SEARCH + YEAR FILTER
    // =====================================================

    private void applyFilters() {

        filteredEpisodes.clear();

        for (TinigEpisode episode : allEpisodes) {

            if (episode == null) {

                continue;
            }


            // =================================================
            // YEAR FILTER
            // =================================================

            boolean matchesYear =
                    selectedYear.equals(
                            "All"
                    );

            if (!matchesYear) {

                String episodeYear =
                        extractYear(
                                episode.getPublished_at()
                        );

                matchesYear =
                        selectedYear.equals(
                                episodeYear
                        );
            }

            if (!matchesYear) {

                continue;
            }


            // =================================================
            // SEARCH TITLE ONLY
            // =================================================

            boolean matchesSearch =
                    currentSearch.isEmpty();

            if (!matchesSearch) {

                String title =
                        safeLower(
                                episode.getTitle()
                        );

                matchesSearch =
                        title.contains(
                                currentSearch
                        );
            }


            // =================================================
            // ADD MATCH
            // =================================================

            if (matchesSearch) {

                filteredEpisodes.add(
                        episode
                );
            }
        }


        // =====================================================
        // UPDATE RECYCLER
        // =====================================================

        episodeAdapter.notifyDataSetChanged();

        updateEpisodeCount();


        // =====================================================
        // EMPTY STATE
        // =====================================================

        if (filteredEpisodes.isEmpty()) {

            txtEmptyEpisodes.setVisibility(
                    View.VISIBLE
            );

            recyclerAllEpisodes.setVisibility(
                    View.GONE
            );

        } else {

            txtEmptyEpisodes.setVisibility(
                    View.GONE
            );

            recyclerAllEpisodes.setVisibility(
                    View.VISIBLE
            );
        }
    }


    // =====================================================
    // EPISODE COUNT
    // =====================================================

    private void updateEpisodeCount() {

        int count =
                filteredEpisodes.size();

        if (count == 1) {

            txtEpisodeCount.setText(
                    "1 episode"
            );

        } else {

            txtEpisodeCount.setText(
                    count + " episodes"
            );
        }
    }


    // =====================================================
    // EXTRACT YEAR
    // =====================================================

    private String extractYear(
            String publishedAt
    ) {

        if (publishedAt == null) {

            return "";
        }

        String value =
                publishedAt.trim();

        if (value.length() < 4) {

            return "";
        }

        String year =
                value.substring(
                        0,
                        4
                );

        if (!year.matches("\\d{4}")) {

            return "";
        }

        return year;
    }


    // =====================================================
    // SAFE LOWERCASE
    // =====================================================

    private String safeLower(
            String value
    ) {

        if (value == null) {

            return "";
        }

        return value
                .trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }


    // =====================================================
    // YEAR FILTER ADAPTER
    // =====================================================

    private class YearFilterAdapter
            extends RecyclerView.Adapter<
            YearFilterAdapter.YearViewHolder
            > {

        private final List<String> years;

        private final OnYearSelectedListener listener;


        YearFilterAdapter(
                List<String> years,
                OnYearSelectedListener listener
        ) {

            this.years = years;

            this.listener = listener;
        }


        void updateYears(
                List<String> newYears
        ) {

            years.clear();

            years.addAll(
                    newYears
            );

            notifyDataSetChanged();
        }


        @Override
        public YearViewHolder onCreateViewHolder(
                android.view.ViewGroup parent,
                int viewType
        ) {

            View view =
                    getLayoutInflater()
                            .inflate(
                                    R.layout.item_tinig_year_filter,
                                    parent,
                                    false
                            );

            return new YearViewHolder(
                    view
            );
        }


        @Override
        public void onBindViewHolder(
                YearViewHolder holder,
                int position
        ) {

            String year =
                    years.get(
                            position
                    );

            holder.txtYear.setText(
                    year
            );

            boolean selected =
                    year.equals(
                            selectedYear
                    );


            if (selected) {

                holder.txtYear.setBackgroundResource(
                        R.drawable.bg_tinig_year_selected
                );

                holder.txtYear.setTextColor(
                        getColor(
                                android.R.color.white
                        )
                );

            } else {

                holder.txtYear.setBackgroundResource(
                        R.drawable.bg_tinig_year_unselected
                );

                holder.txtYear.setTextColor(
                        getColor(
                                R.color.tinig_purple
                        )
                );
            }


            holder.txtYear.setOnClickListener(
                    v -> {

                        selectedYear = year;

                        notifyDataSetChanged();

                        listener.onYearSelected(
                                year
                        );
                    }
            );
        }


        @Override
        public int getItemCount() {

            return years.size();
        }


        class YearViewHolder
                extends RecyclerView.ViewHolder {

            TextView txtYear;


            YearViewHolder(
                    View itemView
            ) {

                super(
                        itemView
                );

                txtYear =
                        itemView.findViewById(
                                R.id.txtYear
                        );
            }
        }
    }


    // =====================================================
    // YEAR CALLBACK
    // =====================================================

    private interface OnYearSelectedListener {

        void onYearSelected(
                String year
        );
    }
}