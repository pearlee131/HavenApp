package com.example.havenapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.havenapp.api.IncidentReportAPI;
import com.example.havenapp.api.SupabaseClient;
import com.google.android.material.button.MaterialButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyReportsActivity extends AppCompatActivity {

    private static final String TAG = "MY_REPORTS";

    private IncidentReportAPI incidentReportAPI;

    private LinearLayout reportsContainer;
    private LinearLayout emptyReportsContainer;

    private AutoCompleteTextView monthFilterDropdown;

    private MaterialButton tabAll;
    private MaterialButton tabActive;
    private MaterialButton tabResolved;

    private View filterOverlay;
    private View filterSheet;
    private View activeFilterCard;

    private TextView textActiveFilter;
    private TextView textNoReports;
    private TextView textNoReportsSubtitle;

    private final List<Map<String, Object>> allReports = new ArrayList<>();

    private String selectedStatusTab = "all";
    private String selectedMonth = "All Months";
    private boolean newestFirst = true;
    private String searchQuery = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.myreports_activity);


        // =====================================================
        // VIEW REFERENCES
        // =====================================================

        reportsContainer =
                findViewById(R.id.reportsContainer);

        emptyReportsContainer =
                findViewById(R.id.emptyReportsContainer);

        monthFilterDropdown =
                findViewById(R.id.monthFilterDropdown);

        tabAll =
                findViewById(R.id.tabAll);

        tabActive =
                findViewById(R.id.tabActive);

        tabResolved =
                findViewById(R.id.tabResolved);

        filterOverlay =
                findViewById(R.id.filterOverlay);

        filterSheet =
                findViewById(R.id.filterSheet);

        activeFilterCard =
                findViewById(R.id.activeFilterCard);

        textActiveFilter =
                findViewById(R.id.textActiveFilter);

        textNoReports =
                findViewById(R.id.textNoReports);

        textNoReportsSubtitle =
                findViewById(R.id.textNoReportsSubtitle);


        // =====================================================
        // SUPABASE API
        // =====================================================

        incidentReportAPI =
                SupabaseClient
                        .getClient()
                        .create(IncidentReportAPI.class);


        // =====================================================
        // SETUP
        // =====================================================

        setupBackButton();
        setupSearch();
        setupStatusTabs();
        setupFilter();
        setupEmptyStateButton();


        // =====================================================
        // LOAD REPORTS
        // =====================================================

        loadMyReports();
    }


    // =====================================================
    // CLOSE / EXIT
    // =====================================================

    private void setupBackButton() {

        findViewById(R.id.btnBack)
                .setOnClickListener(v -> finish());
    }





    // =====================================================
    // SEARCH
    // =====================================================

    private void setupSearch() {

        TextView searchInput =
                findViewById(R.id.searchReportsInput);

        searchInput.addTextChangedListener(
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

                        searchQuery =
                                s == null
                                        ? ""
                                        : s.toString()
                                          .trim()
                                          .toLowerCase(Locale.US);

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
    // STATUS TABS
    // =====================================================

    private void setupStatusTabs() {

        tabAll.setOnClickListener(v -> {

            selectedStatusTab = "all";
            updateTabStyles();
            applyFilters();
        });


        tabActive.setOnClickListener(v -> {

            selectedStatusTab = "active";
            updateTabStyles();
            applyFilters();
        });


        tabResolved.setOnClickListener(v -> {

            selectedStatusTab = "resolved";
            updateTabStyles();
            applyFilters();
        });


        updateTabStyles();
    }


    private void updateTabStyles() {

        styleTab(
                tabAll,
                selectedStatusTab.equals("all")
        );

        styleTab(
                tabActive,
                selectedStatusTab.equals("active")
        );

        styleTab(
                tabResolved,
                selectedStatusTab.equals("resolved")
        );
    }


    private void styleTab(
            MaterialButton button,
            boolean selected
    ) {

        if (selected) {

            button.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.parseColor("#6C2BD9")
                    )
            );

            button.setTextColor(
                    Color.WHITE
            );

            button.setStrokeWidth(
                    0
            );

        } else {

            button.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.parseColor("#FAF8FF")
                    )
            );

            button.setTextColor(
                    Color.parseColor("#21115D")
            );

            button.setStrokeColor(
                    android.content.res.ColorStateList.valueOf(
                            Color.parseColor("#DDD2F4")
                    )
            );

            button.setStrokeWidth(
                    dpToPx(1)
            );
        }
    }


    // =====================================================
    // FILTER SHEET
    // =====================================================

    private void setupFilter() {

        findViewById(R.id.btnFilter)
                .setOnClickListener(v -> openFilter());


        findViewById(R.id.btnCloseFilter)
                .setOnClickListener(v -> closeFilter());


        filterOverlay.setOnClickListener(v -> closeFilter());


        findViewById(R.id.btnResetFilter)
                .setOnClickListener(v -> {

                    selectedMonth = "All Months";
                    newestFirst = true;

                    monthFilterDropdown.setText(
                            "All Months",
                            false
                    );

                    RadioButton radioNewest =
                            findViewById(R.id.radioNewest);

                    radioNewest.setChecked(
                            true
                    );

                    updateActiveFilterSummary();
                });


        findViewById(R.id.btnApplyFilter)
                .setOnClickListener(v -> {

                    String month =
                            monthFilterDropdown
                                    .getText()
                                    .toString()
                                    .trim();

                    selectedMonth =
                            month.isEmpty()
                                    ? "All Months"
                                    : month;
                    RadioButton radioNewest =
                            findViewById(R.id.radioNewest);

                    newestFirst =
                            radioNewest.isChecked();

                    closeFilter();
                    updateActiveFilterSummary();
                    applyFilters();
                });


        findViewById(R.id.btnClearFilter)
                .setOnClickListener(v -> {

                    selectedMonth = "All Months";
                    newestFirst = true;

                    monthFilterDropdown.setText(
                            "All Months",
                            false
                    );

                    RadioButton radioNewest =
                            findViewById(R.id.radioNewest);

                    radioNewest.setChecked(
                            true
                    );

                    updateActiveFilterSummary();
                    applyFilters();
                });
    }


    private void openFilter() {

        filterOverlay.setVisibility(
                View.VISIBLE
        );

        filterSheet.setVisibility(
                View.VISIBLE
        );
    }


    private void closeFilter() {

        filterOverlay.setVisibility(
                View.GONE
        );

        filterSheet.setVisibility(
                View.GONE
        );
    }


    private void updateActiveFilterSummary() {

        boolean hasMonthFilter =
                !selectedMonth.equals(
                        "All Months"
                );

        boolean hasSortFilter =
                !newestFirst;

        if (!hasMonthFilter
                && !hasSortFilter) {

            activeFilterCard.setVisibility(
                    View.GONE
            );

            return;
        }


        activeFilterCard.setVisibility(
                View.VISIBLE
        );


        StringBuilder text =
                new StringBuilder(
                        "Filtered by: "
                );

        if (hasMonthFilter) {

            text.append(
                    selectedMonth
            );
        }


        if (hasMonthFilter
                && hasSortFilter) {

            text.append(
                    " • "
            );
        }


        if (hasSortFilter) {

            text.append(
                    "Oldest First"
            );

        } else if (hasMonthFilter) {

            text.append(
                    " • Newest First"
            );
        }


        textActiveFilter.setText(
                text.toString()
        );
    }


    // =====================================================
    // EMPTY STATE BUTTON
    // =====================================================

    private void setupEmptyStateButton() {

        findViewById(R.id.btnFileNewReport)
                .setOnClickListener(v -> {

                    Intent intent =
                            new Intent(
                                    MyReportsActivity.this,
                                    AddReportActivity.class
                            );

                    startActivity(intent);
                });
    }


    // =====================================================
    // LOAD MY REPORTS
    // =====================================================

    private void loadMyReports() {

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


        String userId =
                preferences.getString(
                        "user_id",
                        ""
                );


        // =====================================================
        // SAFETY CHECK
        // =====================================================

        if (accessToken == null
                || accessToken.trim().isEmpty()
                || userId == null
                || userId.trim().isEmpty()) {

            Log.e(
                    TAG,
                    "ACCESS TOKEN OR USER ID IS MISSING"
            );

            showEmptyState(
                    false
            );

            return;
        }


        // =====================================================
        // GET CURRENT USER'S REPORTS
        // =====================================================

        incidentReportAPI
                .getMyReports(
                        "Bearer " + accessToken,

                        "eq." + userId,

                        "id,case_number,incident_type,"
                                + "incident_date,report_status,created_at",

                        "created_at.desc"
                )
                .enqueue(
                        new Callback<List<Map<String, Object>>>() {

                            @Override
                            public void onResponse(
                                    Call<List<Map<String, Object>>> call,
                                    Response<List<Map<String, Object>>> response
                            ) {

                                if (!response.isSuccessful()) {

                                    Log.e(
                                            TAG,
                                            "HTTP ERROR: "
                                                    + response.code()
                                    );

                                    Toast.makeText(
                                            MyReportsActivity.this,
                                            "Unable to load your reports.",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    showEmptyState(
                                            false
                                    );

                                    return;
                                }


                                List<Map<String, Object>> reports =
                                        response.body();


                                allReports.clear();


                                if (reports != null) {

                                    for (Map<String, Object> report : reports) {

                                        String reportStatus =
                                                getSafeValue(
                                                        report,
                                                        "report_status",
                                                        ""
                                                )
                                                        .trim()
                                                        .toLowerCase(
                                                                Locale.US
                                                        );


                                        // Draft reports belong only to Drafts.
                                        // Never include them in My Reports.
                                        if (reportStatus.equals("draft")) {

                                            continue;
                                        }


                                        allReports.add(
                                                report
                                        );
                                    }
                                }


                                setupMonthOptions();
                                applyFilters();


                                Log.d(
                                        TAG,
                                        "REPORTS LOADED: "
                                                + allReports.size()
                                );
                            }


                            @Override
                            public void onFailure(
                                    Call<List<Map<String, Object>>> call,
                                    Throwable t
                            ) {

                                Log.e(
                                        TAG,
                                        "NETWORK FAILURE",
                                        t
                                );

                                Toast.makeText(
                                        MyReportsActivity.this,
                                        "Unable to load your reports.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                showEmptyState(
                                        false
                                );
                            }
                        }
                );
    }


    // =====================================================
    // MONTH OPTIONS
    // =====================================================

    private void setupMonthOptions() {

        Set<String> months =
                new LinkedHashSet<>();


        for (Map<String, Object> report : allReports) {

            String createdAt =
                    getSafeValue(
                            report,
                            "created_at",
                            ""
                    );

            String monthLabel =
                    getMonthLabel(
                            createdAt
                    );

            if (!monthLabel.isEmpty()) {

                months.add(
                        monthLabel
                );
            }
        }


        List<String> options =
                new ArrayList<>();

        options.add(
                "All Months"
        );

        options.addAll(
                months
        );


        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        R.layout.dropdown_item_compact,
                        options
                );

        monthFilterDropdown.setAdapter(
                adapter
        );

        monthFilterDropdown.setText(
                selectedMonth,
                false
        );
    }


    // =====================================================
    // APPLY FILTERS
    // =====================================================

    private void applyFilters() {

        List<Map<String, Object>> filtered =
                new ArrayList<>();


        for (Map<String, Object> report : allReports) {

            if (!matchesStatusTab(
                    report
            )) {

                continue;
            }


            if (!matchesMonth(
                    report
            )) {

                continue;
            }


            if (!matchesSearch(
                    report
            )) {

                continue;
            }


            filtered.add(
                    report
            );
        }


        Collections.sort(
                filtered,
                new Comparator<Map<String, Object>>() {

                    @Override
                    public int compare(
                            Map<String, Object> first,
                            Map<String, Object> second
                    ) {

                        String firstCreated =
                                getSafeValue(
                                        first,
                                        "created_at",
                                        ""
                                );

                        String secondCreated =
                                getSafeValue(
                                        second,
                                        "created_at",
                                        ""
                                );

                        int comparison =
                                firstCreated.compareTo(
                                        secondCreated
                                );

                        return newestFirst
                                ? -comparison
                                : comparison;
                    }
                }
        );


        clearReportCards();


        if (filtered.isEmpty()) {

            showEmptyState(
                    !allReports.isEmpty()
            );

            return;
        }


        emptyReportsContainer.setVisibility(
                View.GONE
        );


        for (Map<String, Object> report : filtered) {

            addReportCard(
                    report
            );
        }
    }


    private boolean matchesStatusTab(
            Map<String, Object> report
    ) {

        String status =
                getSafeValue(
                        report,
                        "report_status",
                        "submitted"
                )
                        .trim()
                        .toLowerCase(
                                Locale.US
                        );


        // Drafts are never part of My Reports.
        if (status.equals(
                "draft"
        )) {

            return false;
        }


        if (selectedStatusTab.equals(
                "all"
        )) {

            return true;
        }


        boolean resolved =
                status.equals(
                        "resolved"
                )
                        || status.equals(
                        "closed"
                )
                        || status.equals(
                        "completed"
                );


        if (selectedStatusTab.equals(
                "resolved"
        )) {

            return resolved;
        }


        // Everything not resolved remains active.
        return !resolved;
    }


    private boolean matchesMonth(
            Map<String, Object> report
    ) {

        if (selectedMonth.equals(
                "All Months"
        )) {

            return true;
        }


        String createdAt =
                getSafeValue(
                        report,
                        "created_at",
                        ""
                );


        return selectedMonth.equals(
                getMonthLabel(
                        createdAt
                )
        );
    }


    private boolean matchesSearch(
            Map<String, Object> report
    ) {

        if (searchQuery.isEmpty()) {

            return true;
        }


        String caseNumber =
                getSafeValue(
                        report,
                        "case_number",
                        ""
                )
                        .toLowerCase(
                                Locale.US
                        );


        String incidentType =
                getSafeValue(
                        report,
                        "incident_type",
                        ""
                )
                        .toLowerCase(
                                Locale.US
                        );


        String incidentDate =
                getSafeValue(
                        report,
                        "incident_date",
                        ""
                )
                        .toLowerCase(
                                Locale.US
                        );


        String status =
                formatStatus(
                        getSafeValue(
                                report,
                                "report_status",
                                ""
                        )
                )
                        .toLowerCase(
                                Locale.US
                        );


        return caseNumber.contains(
                searchQuery
        )
                || incidentType.contains(
                searchQuery
        )
                || incidentDate.contains(
                searchQuery
        )
                || status.contains(
                searchQuery
        );
    }


    // =====================================================
    // CLEAR DYNAMIC REPORT CARDS
    // =====================================================

    private void clearReportCards() {

        for (int index =
             reportsContainer.getChildCount() - 1;
             index >= 0;
             index--) {

            View child =
                    reportsContainer.getChildAt(
                            index
                    );

            if (child != emptyReportsContainer) {

                reportsContainer.removeViewAt(
                        index
                );
            }
        }
    }


    // =====================================================
    // ADD REPORT CARD
    // =====================================================

    private void addReportCard(
            Map<String, Object> report
    ) {

        String reportId =
                getSafeValue(
                        report,
                        "id",
                        ""
                );


        String caseNumber =
                getSafeValue(
                        report,
                        "case_number",
                        "Pending"
                );


        String incidentType =
                getSafeValue(
                        report,
                        "incident_type",
                        "Not specified"
                );


        String incidentDate =
                getSafeValue(
                        report,
                        "incident_date",
                        "Not specified"
                );


        String reportStatus =
                getSafeValue(
                        report,
                        "report_status",
                        "submitted"
                );


        String createdAt =
                getSafeValue(
                        report,
                        "created_at",
                        "Not available"
                );


        // =====================================================
        // CARD
        // =====================================================

        CardView cardView =
                new CardView(
                        this
                );


        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );


        cardParams.setMargins(
                0,
                0,
                0,
                dpToPx(12)
        );


        cardView.setLayoutParams(
                cardParams
        );


        cardView.setRadius(
                dpToPx(18)
        );


        cardView.setCardElevation(
                dpToPx(2)
        );


        cardView.setCardBackgroundColor(
                Color.WHITE
        );


        // =====================================================
        // MAIN HORIZONTAL CONTENT
        // =====================================================

        LinearLayout row =
                new LinearLayout(
                        this
                );

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        row.setGravity(
                Gravity.CENTER_VERTICAL
        );

        row.setPadding(
                dpToPx(14),
                dpToPx(14),
                dpToPx(12),
                dpToPx(14)
        );


        // =====================================================
        // LEFT ICON
        // =====================================================

        android.widget.ImageView iconView =
                new android.widget.ImageView(
                        this
                );

        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(
                        dpToPx(46),
                        dpToPx(46)
                );

        iconParams.setMargins(
                0,
                0,
                dpToPx(12),
                0
        );

        iconView.setLayoutParams(
                iconParams
        );

        iconView.setPadding(
                dpToPx(7),
                dpToPx(7),
                dpToPx(7),
                dpToPx(7)
        );

        iconView.setScaleType(
                android.widget.ImageView.ScaleType.FIT_CENTER
        );

        iconView.setImageResource(
                R.drawable.ic_report
        );

        iconView.setBackground(
                createRoundedBackground(
                        "#F0E9FF",
                        12
                )
        );


        // =====================================================
        // MIDDLE CONTENT
        // =====================================================

        LinearLayout details =
                new LinearLayout(
                        this
                );

        LinearLayout.LayoutParams detailsParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );

        details.setLayoutParams(
                detailsParams
        );

        details.setOrientation(
                LinearLayout.VERTICAL
        );


        TextView caseNumberView =
                new TextView(
                        this
                );

        caseNumberView.setText(
                caseNumber
        );

        caseNumberView.setTextSize(
                14
        );

        caseNumberView.setTextColor(
                Color.parseColor("#21115D")
        );

        caseNumberView.setTypeface(
                null,
                Typeface.BOLD
        );


        TextView submittedDateView =
                createSmallDetailTextView(
                        formatCreatedAtForCard(
                                createdAt
                        )
                );


        TextView typeView =
                new TextView(
                        this
                );

        LinearLayout.LayoutParams typeParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        typeParams.topMargin =
                dpToPx(6);

        typeView.setLayoutParams(
                typeParams
        );

        typeView.setText(
                incidentType
        );

        typeView.setTextSize(
                12
        );

        typeView.setTextColor(
                Color.parseColor("#21115D")
        );

        typeView.setTypeface(
                null,
                Typeface.BOLD
        );


        TextView incidentDateView =
                createSmallDetailTextView(
                        "Incident Date: "
                                + incidentDate
                );


        details.addView(
                caseNumberView
        );

        details.addView(
                submittedDateView
        );

        details.addView(
                typeView
        );

        details.addView(
                incidentDateView
        );


        // =====================================================
        // RIGHT SIDE: STATUS + CHEVRON
        // =====================================================

        LinearLayout rightSide =
                new LinearLayout(
                        this
                );

        rightSide.setOrientation(
                LinearLayout.VERTICAL
        );

        rightSide.setGravity(
                Gravity.END
        );


        TextView statusView =
                new TextView(
                        this
                );

        statusView.setText(
                getDisplayStatus(
                        reportStatus
                )
        );

        statusView.setTextSize(
                10
        );

        statusView.setTypeface(
                null,
                Typeface.BOLD
        );

        statusView.setPadding(
                dpToPx(11),
                dpToPx(6),
                dpToPx(11),
                dpToPx(6)
        );


        applyStatusStyle(
                statusView,
                reportStatus
        );


        TextView arrowView =
                new TextView(
                        this
                );

        LinearLayout.LayoutParams arrowParams =
                new LinearLayout.LayoutParams(
                        dpToPx(30),
                        dpToPx(34)
                );

        arrowParams.topMargin =
                dpToPx(8);

        arrowView.setLayoutParams(
                arrowParams
        );

        arrowView.setGravity(
                Gravity.CENTER
        );

        arrowView.setText(
                "›"
        );

        arrowView.setTextColor(
                Color.parseColor("#6C2BD9")
        );

        arrowView.setTextSize(
                28
        );


        // Status badge intentionally hidden.
        // Status data remains available for filtering and report logic.

        rightSide.addView(
                arrowView
        );


        row.addView(
                iconView
        );

        row.addView(
                details
        );

        row.addView(
                rightSide
        );


        cardView.addView(
                row
        );


        // =====================================================
        // OPEN REPORT DETAILS
        // =====================================================

        if (!reportId.isEmpty()) {

            cardView.setOnClickListener(v -> {

                Intent intent =
                        new Intent(
                                MyReportsActivity.this,
                                ReportDetailsActivity.class
                        );

                intent.putExtra(
                        "report_id",
                        reportId
                );

                startActivity(
                        intent
                );
            });
        }


        int emptyIndex =
                reportsContainer.indexOfChild(
                        emptyReportsContainer
                );


        if (emptyIndex >= 0) {

            reportsContainer.addView(
                    cardView,
                    emptyIndex
            );

        } else {

            reportsContainer.addView(
                    cardView
            );
        }
    }


    // =====================================================
    // STATUS STYLING
    // =====================================================

    private void applyStatusStyle(
            TextView statusView,
            String status
    ) {

        String normalized =
                status == null
                        ? ""
                        : status.trim()
                          .toLowerCase(
                                  Locale.US
                          );


        if (normalized.equals(
                "resolved"
        )
                || normalized.equals(
                "closed"
        )
                || normalized.equals(
                "completed"
        )) {

            statusView.setTextColor(
                    Color.parseColor("#188641")
            );

            statusView.setBackground(
                    createRoundedBackground(
                            "#E6F7EB",
                            14
                    )
            );

        } else {

            statusView.setTextColor(
                    Color.parseColor("#247BCB")
            );

            statusView.setBackground(
                    createRoundedBackground(
                            "#E8F4FF",
                            14
                    )
            );
        }
    }


    private String getDisplayStatus(
            String status
    ) {

        String normalized =
                status == null
                        ? ""
                        : status.trim()
                          .toLowerCase(
                                  Locale.US
                          );


        if (normalized.equals(
                "resolved"
        )
                || normalized.equals(
                "closed"
        )
                || normalized.equals(
                "completed"
        )) {

            return "Resolved";
        }


        return "Active";
    }


    // =====================================================
    // CREATE SMALL DETAIL TEXT
    // =====================================================

    private TextView createSmallDetailTextView(
            String text
    ) {

        TextView textView =
                new TextView(
                        this
                );


        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.topMargin =
                dpToPx(2);

        textView.setLayoutParams(
                params
        );

        textView.setText(
                text
        );

        textView.setTextSize(
                10
        );

        textView.setTextColor(
                Color.parseColor("#766D8A")
        );

        return textView;
    }


    // =====================================================
    // EMPTY STATE
    // =====================================================

    private void showEmptyState(
            boolean filteredEmpty
    ) {

        clearReportCards();

        emptyReportsContainer.setVisibility(
                View.VISIBLE
        );


        if (filteredEmpty) {

            textNoReports.setText(
                    "No Reports Found"
            );

            textNoReportsSubtitle.setText(
                    "No reports match your current search or filter."
            );

        } else {

            textNoReports.setText(
                    "No Reports Yet"
            );

            textNoReportsSubtitle.setText(
                    "You haven't submitted any reports yet. "
                            + "When you file a report, it will appear here."
            );
        }
    }


    // =====================================================
    // DATE HELPERS
    // =====================================================

    private String getMonthLabel(
            String createdAt
    ) {

        if (createdAt == null
                || createdAt.trim().isEmpty()) {

            return "";
        }


        String datePart =
                createdAt.length() >= 10
                        ? createdAt.substring(
                        0,
                        10
                )
                        : createdAt;


        SimpleDateFormat input =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                );

        SimpleDateFormat output =
                new SimpleDateFormat(
                        "MMMM yyyy",
                        Locale.US
                );


        try {

            Date date =
                    input.parse(
                            datePart
                    );

            return date == null
                    ? ""
                    : output.format(
                    date
            );

        } catch (ParseException exception) {

            return "";
        }
    }


    private String formatCreatedAtForCard(
            String createdAt
    ) {

        if (createdAt == null
                || createdAt.trim().isEmpty()
                || createdAt.equals(
                "Not available"
        )) {

            return "Date filed not available";
        }


        String cleaned =
                normalizeReportTimestamp(
                        createdAt.trim()
                );


        String[] possibleFormats = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss"
        };


        for (String pattern : possibleFormats) {

            try {

                SimpleDateFormat input =
                        new SimpleDateFormat(
                                pattern,
                                Locale.US
                        );

                input.setLenient(false);

                if (pattern.endsWith("'Z'")
                        || pattern.equals("yyyy-MM-dd HH:mm:ss")) {

                    input.setTimeZone(
                            TimeZone.getTimeZone("UTC")
                    );
                }

                Date date = input.parse(cleaned);

                if (date != null) {

                    SimpleDateFormat output =
                            new SimpleDateFormat(
                                    "MMM dd, yyyy • h:mm a",
                                    Locale.US
                            );

                    output.setTimeZone(
                            TimeZone.getTimeZone("Asia/Manila")
                    );

                    return output.format(date);
                }

            } catch (ParseException ignored) {
            }
        }

        if (createdAt.length() >= 10) {
            return createdAt.substring(0, 10);
        }

        return createdAt;
    }


    private String normalizeReportTimestamp(
            String timestamp
    ) {

        if (timestamp == null
                || timestamp.trim().isEmpty()) {

            return "";
        }

        String cleaned = timestamp.trim();
        int dotIndex = cleaned.indexOf('.');

        if (dotIndex < 0) {
            return cleaned;
        }

        int fractionEnd = dotIndex + 1;

        while (fractionEnd < cleaned.length()
                && Character.isDigit(cleaned.charAt(fractionEnd))) {
            fractionEnd++;
        }

        String fraction = cleaned.substring(dotIndex + 1, fractionEnd);

        if (fraction.length() > 3) {
            fraction = fraction.substring(0, 3);
        } else {
            while (fraction.length() < 3) {
                fraction += "0";
            }
        }

        return cleaned.substring(0, dotIndex + 1)
                + fraction
                + cleaned.substring(fractionEnd);
    }


    // =====================================================
    // SAFE MAP VALUE
    // =====================================================

    private String getSafeValue(
            Map<String, Object> report,
            String key,
            String defaultValue
    ) {

        Object value =
                report.get(
                        key
                );


        if (value == null) {

            return defaultValue;
        }


        String result =
                String.valueOf(
                                value
                        )
                        .trim();


        if (result.isEmpty()
                || result.equalsIgnoreCase(
                "null"
        )) {

            return defaultValue;
        }


        return result;
    }


    // =====================================================
    // FORMAT STATUS
    // =====================================================

    private String formatStatus(
            String status
    ) {

        if (status == null
                || status.trim().isEmpty()) {

            return "Submitted";
        }


        String cleaned =
                status
                        .trim()
                        .toLowerCase(
                                Locale.US
                        )
                        .replace(
                                "_",
                                " "
                        );


        String[] words =
                cleaned.split(
                        " "
                );


        StringBuilder builder =
                new StringBuilder();


        for (String word : words) {

            if (word.isEmpty()) {

                continue;
            }


            builder.append(
                    Character.toUpperCase(
                            word.charAt(
                                    0
                            )
                    )
            );


            if (word.length() > 1) {

                builder.append(
                        word.substring(
                                1
                        )
                );
            }


            builder.append(
                    " "
            );
        }


        return builder
                .toString()
                .trim();
    }


    // =====================================================
    // ROUNDED BACKGROUND
    // =====================================================

    private GradientDrawable createRoundedBackground(
            String colorHex,
            int radiusDp
    ) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(
                Color.parseColor(
                        colorHex
                )
        );

        drawable.setCornerRadius(
                dpToPx(
                        radiusDp
                )
        );

        return drawable;
    }


    // =====================================================
    // DP TO PIXELS
    // =====================================================

    private int dpToPx(
            int dp
    ) {

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return Math.round(
                dp * density
        );
    }
}



