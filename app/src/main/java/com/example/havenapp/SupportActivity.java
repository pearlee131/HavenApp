package com.example.havenapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;

import com.example.havenapp.api.IncidentReportAPI;
import com.example.havenapp.api.RefreshTokenRequest;
import com.example.havenapp.api.SupabaseAuthAPI;
import com.example.havenapp.api.SupabaseClient;
import com.example.havenapp.api.ReportNotificationAPI;

import com.example.havenapp.model.AuthResponse;
import com.example.havenapp.model.ReportNotification;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SupportActivity extends AppCompatActivity {

    private static final String TAG = "SUPPORT_OVERVIEW";

    // =====================================================
    // AUTO REFRESH
    // =====================================================

    private static final long AUTO_REFRESH_INTERVAL_MS = 3000L;

    private final Handler autoRefreshHandler =
            new Handler(
                    Looper.getMainLooper()
            );

    private final Runnable autoRefreshRunnable =
            new Runnable() {

                @Override
                public void run() {

                    loadReportsOverview();
                    loadRecentReports();
                    loadUnreadNotificationCount();

                    autoRefreshHandler.postDelayed(
                            this,
                            AUTO_REFRESH_INTERVAL_MS
                    );
                }
            };


    // =====================================================
    // MY REPORTS OVERVIEW
    // =====================================================

    private TextView textTotalReports;
    private TextView textActiveReports;
    private TextView textResolvedReports;
    private TextView textDraftReports;


    // =====================================================
    // RECENT REPORTS
    // =====================================================

    private LinearLayout recentReportsContainer;
    private TextView textNoRecentReports;


    // =====================================================
    // SUPABASE
    // =====================================================

    private IncidentReportAPI incidentReportAPI;
    private SupabaseAuthAPI authAPI;
    private ReportNotificationAPI notificationAPI;
    // =====================================================
    // SESSION REFRESH STATE
    // =====================================================

    private boolean isRefreshingToken = false;
    private boolean retryOverviewAfterRefresh = false;
    private boolean retryRecentAfterRefresh = false;


    // =====================================================
    // ACTIVITY
    // =====================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.support_activity);

        // =====================================================
// NOTIFICATION BADGE
// =====================================================

        txtSupportNotificationBadge =
                findViewById(R.id.txtSupportNotificationBadge);


        // =====================================================
        // MY REPORTS OVERVIEW
        // =====================================================

        textTotalReports =
                findViewById(R.id.textTotalReports);

        textActiveReports =
                findViewById(R.id.textActiveReports);

        textResolvedReports =
                findViewById(R.id.textResolvedReports);

        textDraftReports =
                findViewById(R.id.textDraftReports);


        // =====================================================
        // RECENT REPORTS
        // =====================================================

        recentReportsContainer =
                findViewById(R.id.recentReportsContainer);

        textNoRecentReports =
                findViewById(R.id.textNoRecentReports);


        // =====================================================
        // SUPABASE API
        // =====================================================

        incidentReportAPI =
                SupabaseClient
                        .getClient()
                        .create(IncidentReportAPI.class);

        authAPI =
                SupabaseClient
                        .getClient()
                        .create(SupabaseAuthAPI.class);

        notificationAPI =
                SupabaseClient
                        .getClient()
                        .create(ReportNotificationAPI.class);


        // =====================================================
        // EXISTING SUPPORT FUNCTIONS
        // =====================================================

        setupSupportActions();
        setupNotification();
        setupHopeAssistant();
        setupNavigation();
    }

    // =====================================================
// FLOATING / MOVABLE HOPE ASSISTANT
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
    // REFRESH SUPPORT DATA WHEN RETURNING TO PAGE
    // =====================================================

    @Override
    protected void onResume() {

        super.onResume();

        startAutoRefresh();
    }


    // =====================================================
    // STOP AUTO REFRESH WHEN SUPPORT PAGE IS NOT VISIBLE
    // =====================================================

    @Override
    protected void onPause() {

        super.onPause();

        stopAutoRefresh();
    }


    // =====================================================
    // START AUTO REFRESH
    //
    // Refreshes immediately, then every 3 seconds while the
    // Support page is visible.
    // =====================================================

    private void startAutoRefresh() {

        autoRefreshHandler.removeCallbacks(
                autoRefreshRunnable
        );

        autoRefreshHandler.post(
                autoRefreshRunnable
        );
    }


    // =====================================================
    // STOP AUTO REFRESH
    // =====================================================

    private void stopAutoRefresh() {

        autoRefreshHandler.removeCallbacks(
                autoRefreshRunnable
        );
    }

// =====================================================
// NOTIFICATION BADGE
// =====================================================

    private TextView txtSupportNotificationBadge;

    // =====================================================
    // LOAD MY REPORTS OVERVIEW
    // =====================================================

    private void loadReportsOverview() {

        loadReportsOverview(
                false
        );
    }


    private void loadReportsOverview(
            boolean alreadyRetriedAfterRefresh
    ) {

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

            setOverviewCounts(
                    0,
                    0,
                    0,
                    0
            );

            return;
        }


        // =====================================================
        // GET REPORTS OF CURRENT LOGGED-IN USER ONLY
        // =====================================================

        incidentReportAPI
                .getUserReports(
                        "Bearer " + accessToken,
                        "eq." + userId,
                        "id,report_status"
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
                                            "REPORT OVERVIEW HTTP ERROR: "
                                                    + response.code()
                                    );

                                    if (response.code() == 401
                                            && !alreadyRetriedAfterRefresh) {

                                        requestSessionRefresh(
                                                true,
                                                false
                                        );

                                        return;
                                    }

                                    return;
                                }


                                List<Map<String, Object>> reports =
                                        response.body();


                                if (reports == null) {

                                    setOverviewCounts(
                                            0,
                                            0,
                                            0,
                                            0
                                    );

                                    return;
                                }


                                int totalReports =
                                        0;

                                int activeReports =
                                        0;

                                int resolvedReports =
                                        0;

                                int draftReports =
                                        0;


                                // =================================================
                                // COUNT REPORT STATUS
                                // =================================================

                                for (Map<String, Object> report : reports) {

                                    Object statusObject =
                                            report.get(
                                                    "report_status"
                                            );

                                    if (statusObject == null) {
                                        continue;
                                    }


                                    String status =
                                            String.valueOf(
                                                            statusObject
                                                    )
                                                    .trim()
                                                    .toLowerCase(
                                                            Locale.US
                                                    );


                                    switch (status) {

                                        // =========================================
                                        // DRAFT REPORTS
                                        // Drafts stay separate from the submitted
                                        // report counts and progress workflow.
                                        // =========================================

                                        case "draft":

                                            draftReports++;

                                            break;


                                        // =========================================
                                        // ACTIVE REPORTS
                                        // =========================================

                                        case "submitted":
                                        case "under_review":
                                        case "validated":
                                        case "referred":

                                            totalReports++;
                                            activeReports++;

                                            break;


                                        // =========================================
                                        // RESOLVED REPORTS
                                        // =========================================

                                        case "resolved":
                                        case "closed":

                                            totalReports++;
                                            resolvedReports++;

                                            break;


                                        // =========================================
                                        // OTHER NON-DRAFT REPORT STATUS
                                        // =========================================

                                        default:

                                            totalReports++;

                                            break;
                                    }
                                }


                                setOverviewCounts(
                                        totalReports,
                                        activeReports,
                                        resolvedReports,
                                        draftReports
                                );


                                Log.d(
                                        TAG,
                                        "TOTAL: "
                                                + totalReports
                                                + " | ACTIVE: "
                                                + activeReports
                                                + " | RESOLVED: "
                                                + resolvedReports
                                                + " | DRAFTS: "
                                                + draftReports
                                );
                            }


                            @Override
                            public void onFailure(
                                    Call<List<Map<String, Object>>> call,
                                    Throwable t
                            ) {

                                Log.e(
                                        TAG,
                                        "REPORT OVERVIEW NETWORK FAILURE",
                                        t
                                );
                            }
                        }
                );
    }


    // =====================================================
    // LOAD RECENT REPORTS
    //
    // Shows the 3 newest reports.
    // ALL 3 recent report cards include the same 5-step
    // progress tracker shown in the reference design.
    // =====================================================

    private void loadRecentReports() {

        loadRecentReports(
                false
        );
    }


    private void loadRecentReports(
            boolean alreadyRetriedAfterRefresh
    ) {

        if (recentReportsContainer == null
                || textNoRecentReports == null) {

            Log.e(
                    TAG,
                    "RECENT REPORT VIEWS ARE MISSING"
            );

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

            showNoRecentReports();

            return;
        }


        // =====================================================
        // LOAD USER REPORTS NEWEST FIRST
        // =====================================================

        incidentReportAPI
                .getMyReports(
                        "Bearer " + accessToken,
                        "eq." + userId,
                        "id,case_number,report_status,created_at",
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
                                            "RECENT REPORTS HTTP ERROR: "
                                                    + response.code()
                                    );

                                    if (response.code() == 401
                                            && !alreadyRetriedAfterRefresh) {

                                        requestSessionRefresh(
                                                false,
                                                true
                                        );

                                        return;
                                    }

                                    showNoRecentReports();

                                    return;
                                }


                                List<Map<String, Object>> reports =
                                        response.body();


                                if (reports == null
                                        || reports.isEmpty()) {

                                    showNoRecentReports();

                                    return;
                                }


                                displayRecentReports(
                                        reports
                                );
                            }


                            @Override
                            public void onFailure(
                                    Call<List<Map<String, Object>>> call,
                                    Throwable t
                            ) {

                                Log.e(
                                        TAG,
                                        "RECENT REPORTS NETWORK FAILURE",
                                        t
                                );

                                showNoRecentReports();
                            }
                        }
                );
    }


    // =====================================================
    // REFRESH EXPIRED SUPABASE SESSION
    //
    // Called only after a 401 response.
    // Uses the saved refresh_token, saves the newly returned
    // access/refresh tokens, then retries the failed report
    // request once.
    // =====================================================

    private void requestSessionRefresh(
            boolean retryOverview,
            boolean retryRecent
    ) {

        if (retryOverview) {
            retryOverviewAfterRefresh = true;
        }

        if (retryRecent) {
            retryRecentAfterRefresh = true;
        }


        // =====================================================
        // PREVENT TWO REFRESH REQUESTS AT THE SAME TIME
        // =====================================================

        if (isRefreshingToken) {

            Log.d(
                    TAG,
                    "SESSION REFRESH ALREADY IN PROGRESS"
            );

            return;
        }


        SharedPreferences preferences =
                getSharedPreferences(
                        "HavenUser",
                        MODE_PRIVATE
                );

        String refreshToken =
                preferences.getString(
                        "refresh_token",
                        ""
                );


        if (refreshToken == null
                || refreshToken.trim().isEmpty()) {

            Log.e(
                    TAG,
                    "REFRESH TOKEN IS MISSING"
            );

            retryOverviewAfterRefresh = false;
            retryRecentAfterRefresh = false;

            return;
        }


        isRefreshingToken = true;


        authAPI
                .refreshToken(
                        "refresh_token",
                        new RefreshTokenRequest(
                                refreshToken
                        )
                )
                .enqueue(
                        new Callback<AuthResponse>() {

                            @Override
                            public void onResponse(
                                    Call<AuthResponse> call,
                                    Response<AuthResponse> response
                            ) {

                                isRefreshingToken = false;


                                if (!response.isSuccessful()
                                        || response.body() == null) {

                                    Log.e(
                                            TAG,
                                            "SESSION REFRESH HTTP ERROR: "
                                                    + response.code()
                                    );

                                    retryOverviewAfterRefresh = false;
                                    retryRecentAfterRefresh = false;

                                    return;
                                }


                                AuthResponse authResponse =
                                        response.body();

                                String newAccessToken =
                                        authResponse.getAccess_token();

                                String newRefreshToken =
                                        authResponse.getRefresh_token();


                                if (newAccessToken == null
                                        || newAccessToken.trim().isEmpty()) {

                                    Log.e(
                                            TAG,
                                            "SESSION REFRESH RETURNED EMPTY ACCESS TOKEN"
                                    );

                                    retryOverviewAfterRefresh = false;
                                    retryRecentAfterRefresh = false;

                                    return;
                                }


                                SharedPreferences.Editor editor =
                                        preferences.edit();

                                editor.putString(
                                        "access_token",
                                        newAccessToken
                                );


                                // Supabase normally rotates the refresh token.
                                // Keep the old saved refresh token only if the
                                // response unexpectedly does not contain a new one.
                                if (newRefreshToken != null
                                        && !newRefreshToken.trim().isEmpty()) {

                                    editor.putString(
                                            "refresh_token",
                                            newRefreshToken
                                    );
                                }

                                editor.apply();


                                Log.d(
                                        TAG,
                                        "SESSION REFRESH SUCCESSFUL"
                                );


                                boolean shouldRetryOverview =
                                        retryOverviewAfterRefresh;

                                boolean shouldRetryRecent =
                                        retryRecentAfterRefresh;

                                retryOverviewAfterRefresh = false;
                                retryRecentAfterRefresh = false;


                                if (shouldRetryOverview) {

                                    loadReportsOverview(
                                            true
                                    );
                                }


                                if (shouldRetryRecent) {

                                    loadRecentReports(
                                            true
                                    );
                                }
                            }


                            @Override
                            public void onFailure(
                                    Call<AuthResponse> call,
                                    Throwable t
                            ) {

                                isRefreshingToken = false;

                                retryOverviewAfterRefresh = false;
                                retryRecentAfterRefresh = false;

                                Log.e(
                                        TAG,
                                        "SESSION REFRESH NETWORK FAILURE",
                                        t
                                );
                            }
                        }
                );
    }


    // =====================================================
    // DISPLAY RECENT REPORTS
    // =====================================================

    private void displayRecentReports(
            List<Map<String, Object>> reports
    ) {

        recentReportsContainer.removeAllViews();

        textNoRecentReports.setVisibility(
                View.GONE
        );


        int displayedReports =
                0;


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


            // Drafts belong only to the Drafts section.
            // Never show them in Recent Reports or its progress tracker.
            if (reportStatus.equals("draft")) {

                continue;
            }


            boolean showProgressIndicator =
                    true;


            CardView reportCard =
                    createRecentReportCard(
                            report,
                            showProgressIndicator
                    );


            recentReportsContainer.addView(
                    reportCard
            );


            displayedReports++;


            if (displayedReports >= 3) {

                break;
            }
        }


        if (displayedReports == 0) {

            showNoRecentReports();
        }
    }


    // =====================================================
    // CREATE RECENT REPORT CARD
    // =====================================================

    private CardView createRecentReportCard(
            Map<String, Object> report,
            boolean showProgressIndicator
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
                        ""
                );


        // =====================================================
        // CARD
        // =====================================================

        CardView cardView =
                new CardView(
                        SupportActivity.this
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
                dpToPx(10)
        );

        cardView.setLayoutParams(
                cardParams
        );

        cardView.setRadius(
                dpToPx(17)
        );

        cardView.setCardElevation(
                dpToPx(2)
        );

        cardView.setCardBackgroundColor(
                Color.WHITE
        );

        cardView.setUseCompatPadding(
                false
        );

        cardView.setClickable(
                true
        );

        cardView.setFocusable(
                true
        );


        // =====================================================
        // CARD CONTENT
        // =====================================================

        LinearLayout cardContent =
                new LinearLayout(
                        SupportActivity.this
                );

        cardContent.setOrientation(
                LinearLayout.VERTICAL
        );

        cardContent.setPadding(
                dpToPx(14),
                dpToPx(13),
                dpToPx(14),
                showProgressIndicator
                        ? dpToPx(12)
                        : dpToPx(13)
        );


        // =====================================================
        // TOP REPORT INFORMATION ROW
        // =====================================================

        LinearLayout topRow =
                new LinearLayout(
                        SupportActivity.this
                );

        topRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        topRow.setGravity(
                Gravity.TOP
        );


        // =====================================================
        // REPORT ICON
        // =====================================================

        LinearLayout iconCircle =
                new LinearLayout(
                        SupportActivity.this
                );

        LinearLayout.LayoutParams iconCircleParams =
                new LinearLayout.LayoutParams(
                        dpToPx(46),
                        dpToPx(46)
                );

        iconCircleParams.setMargins(
                0,
                0,
                dpToPx(11),
                0
        );

        iconCircle.setLayoutParams(
                iconCircleParams
        );

        iconCircle.setGravity(
                Gravity.CENTER
        );

        iconCircle.setBackground(
                createRoundedBackground(
                        "#F1E9FF",
                        23
                )
        );


        ImageView reportIcon =
                new ImageView(
                        SupportActivity.this
                );

        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(
                        dpToPx(27),
                        dpToPx(27)
                );

        reportIcon.setLayoutParams(
                iconParams
        );

        reportIcon.setScaleType(
                ImageView.ScaleType.CENTER_INSIDE
        );

        reportIcon.setImageResource(
                R.drawable.ic_report
        );


        iconCircle.addView(
                reportIcon
        );


        // =====================================================
        // MAIN REPORT TEXT
        // =====================================================

        LinearLayout reportTextContainer =
                new LinearLayout(
                        SupportActivity.this
                );

        LinearLayout.LayoutParams reportTextParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );

        reportTextContainer.setLayoutParams(
                reportTextParams
        );

        reportTextContainer.setOrientation(
                LinearLayout.VERTICAL
        );


        TextView caseNumberText =
                createTextView(
                        getCaseDisplayText(
                                caseNumber
                        ),
                        13.5f,
                        "#24134C",
                        true
                );


        TextView dateText =
                createTextView(
                        formatRecentDateTime(
                                createdAt
                        ),
                        9f,
                        "#827894",
                        false
                );

        LinearLayout.LayoutParams dateParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        dateParams.setMargins(
                0,
                dpToPx(4),
                0,
                0
        );

        dateText.setLayoutParams(
                dateParams
        );


        reportTextContainer.addView(
                caseNumberText
        );


        // =====================================================
        // STATUS BELOW REPORT NAME
        // =====================================================

        TextView statusBadge =
                createStatusBadge(
                        reportStatus
                );


        LinearLayout.LayoutParams badgeParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        badgeParams.setMargins(
                0,
                dpToPx(4),
                0,
                0
        );

        statusBadge.setLayoutParams(
                badgeParams
        );


        reportTextContainer.addView(
                statusBadge
        );

        reportTextContainer.addView(
                dateText
        );


        // =====================================================
        // ADD TOP ROW
        // =====================================================

        topRow.addView(
                iconCircle
        );

        topRow.addView(
                reportTextContainer
        );


        cardContent.addView(
                topRow
        );


        // =====================================================
        // PROGRESS INDICATOR FOR NEWEST REPORT
        // =====================================================

        if (showProgressIndicator) {

            LinearLayout progressContainer =
                    createProgressIndicator(
                            reportStatus
                    );

            LinearLayout.LayoutParams progressParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            progressParams.setMargins(
                    0,
                    dpToPx(15),
                    0,
                    0
            );

            progressContainer.setLayoutParams(
                    progressParams
            );


            cardContent.addView(
                    progressContainer
            );
        }


        cardView.addView(
                cardContent
        );


        // =====================================================
        // OPEN REPORT DETAILS
        // =====================================================

        if (!reportId.isEmpty()) {

            cardView.setOnClickListener(
                    v -> {

                        Intent intent =
                                new Intent(
                                        SupportActivity.this,
                                        ReportDetailsActivity.class
                                );

                        intent.putExtra(
                                "report_id",
                                reportId
                        );

                        startActivity(
                                intent
                        );
                    }
            );
        }


        return cardView;
    }


    // =====================================================
    // CREATE 5-STEP PROGRESS INDICATOR
    //
    // Exact 5 statuses used in the app:
    // Submitted -> Under Review -> Validated
    // -> Referred -> Resolved
    //
    // Database -> visual progress mapping:
    // submitted    = Step 1  Submitted
    // under_review = Step 2  Under Review
    // validated    = Step 3  Validated
    // referred     = Step 4  Referred
    // resolved     = Step 5  Resolved
    // closed       = Step 5  Resolved
    // =====================================================

    private LinearLayout createProgressIndicator(
            String reportStatus
    ) {

        int currentStep =
                getProgressStep(
                        reportStatus
                );


        LinearLayout outer =
                new LinearLayout(
                        SupportActivity.this
                );

        outer.setOrientation(
                LinearLayout.VERTICAL
        );


        // =====================================================
        // CIRCLES + CONNECTOR LINES
        // =====================================================

        LinearLayout progressRow =
                new LinearLayout(
                        SupportActivity.this
                );

        progressRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        progressRow.setGravity(
                Gravity.CENTER_VERTICAL
        );


        for (int step = 1;
             step <= 5;
             step++) {


            boolean completed =
                    step < currentStep;

            boolean current =
                    step == currentStep;

            boolean resolvedAll =
                    currentStep == 5;


            TextView circle =
                    new TextView(
                            SupportActivity.this
                    );

            LinearLayout.LayoutParams circleParams =
                    new LinearLayout.LayoutParams(
                            dpToPx(22),
                            dpToPx(22)
                    );

            circle.setLayoutParams(
                    circleParams
            );

            circle.setGravity(
                    Gravity.CENTER
            );

            circle.setTextSize(
                    8.5f
            );

            circle.setTypeface(
                    getPoppinsSemibold(),
                    Typeface.BOLD
            );


            if (resolvedAll) {

                circle.setText(
                        "✓"
                );

                circle.setTextColor(
                        Color.WHITE
                );

                circle.setBackground(
                        createRoundedBackground(
                                "#8B4CF6",
                                11
                        )
                );

            } else if (completed) {

                circle.setText(
                        "✓"
                );

                circle.setTextColor(
                        Color.WHITE
                );

                circle.setBackground(
                        createRoundedBackground(
                                "#8B4CF6",
                                11
                        )
                );

            } else if (current) {

                circle.setText(
                        String.valueOf(
                                step
                        )
                );

                circle.setTextColor(
                        Color.WHITE
                );

                circle.setBackground(
                        createRoundedBackground(
                                getCurrentStepColor(
                                        reportStatus
                                ),
                                11
                        )
                );

            } else {

                circle.setText(
                        String.valueOf(
                                step
                        )
                );

                circle.setTextColor(
                        Color.parseColor(
                                "#8E879B"
                        )
                );

                GradientDrawable futureCircle =
                        createRoundedBackground(
                                "#F3F1F6",
                                11
                        );

                futureCircle.setStroke(
                        dpToPx(1),
                        Color.parseColor(
                                "#DFDBE7"
                        )
                );

                circle.setBackground(
                        futureCircle
                );
            }


            progressRow.addView(
                    circle
            );


            if (step < 5) {

                View connector =
                        new View(
                                SupportActivity.this
                        );

                LinearLayout.LayoutParams connectorParams =
                        new LinearLayout.LayoutParams(
                                0,
                                dpToPx(2),
                                1f
                        );

                connectorParams.setMargins(
                        dpToPx(2),
                        0,
                        dpToPx(2),
                        0
                );

                connector.setLayoutParams(
                        connectorParams
                );


                boolean connectorReached =
                        resolvedAll
                                || step < currentStep;


                connector.setBackgroundColor(
                        Color.parseColor(
                                connectorReached
                                        ? "#A96CFF"
                                        : "#E3DFE9"
                        )
                );


                progressRow.addView(
                        connector
                );
            }
        }


        outer.addView(
                progressRow
        );


        // =====================================================
        // LABELS
        // =====================================================

        String[] labels = {
                "Submitted",
                "Under\nReview",
                "Validated",
                "Referred",
                "Resolved"
        };


        LinearLayout labelRow =
                new LinearLayout(
                        SupportActivity.this
                );

        LinearLayout.LayoutParams labelRowParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        labelRowParams.setMargins(
                0,
                dpToPx(5),
                0,
                0
        );

        labelRow.setLayoutParams(
                labelRowParams
        );

        labelRow.setOrientation(
                LinearLayout.HORIZONTAL
        );


        for (int index = 0;
             index < labels.length;
             index++) {


            int stepNumber =
                    index + 1;


            TextView label =
                    createTextView(
                            labels[index],
                            7.2f,
                            stepNumber <= currentStep
                                    ? "#4E3A6D"
                                    : "#AAA4B3",
                            false
                    );


            LinearLayout.LayoutParams labelParams =
                    new LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                    );

            label.setLayoutParams(
                    labelParams
            );

            label.setGravity(
                    Gravity.TOP
                            | Gravity.CENTER_HORIZONTAL
            );

            label.setTextAlignment(
                    View.TEXT_ALIGNMENT_CENTER
            );

            label.setLineSpacing(
                    0,
                    0.9f
            );


            if (stepNumber == currentStep) {

                label.setTypeface(
                        getPoppinsSemibold(),
                        Typeface.BOLD
                );
            }


            labelRow.addView(
                    label
            );
        }


        outer.addView(
                labelRow
        );


        return outer;
    }


    // =====================================================
    // STATUS -> PROGRESS STEP
    // =====================================================

    private int getProgressStep(
            String reportStatus
    ) {

        String status =
                reportStatus == null
                        ? ""
                        : reportStatus
                          .trim()
                          .toLowerCase(
                                  Locale.US
                          );


        switch (status) {

            case "submitted":
                return 1;

            case "under_review":
                return 2;

            case "validated":
                return 3;

            case "referred":
                return 4;

            case "resolved":
            case "closed":
                return 5;

            default:
                return 1;
        }
    }


    // =====================================================
    // CREATE STATUS BADGE
    // =====================================================

    private TextView createStatusBadge(
            String reportStatus
    ) {

        String status =
                reportStatus == null
                        ? ""
                        : reportStatus
                          .trim()
                          .toLowerCase(
                                  Locale.US
                          );


        String label;
        String textColor;
        String backgroundColor;


        switch (status) {

            case "submitted":

                label =
                        "Submitted";

                textColor =
                        "#6C2BD9";

                backgroundColor =
                        "#F1E9FF";

                break;


            case "under_review":

                label =
                        "Under Review";

                textColor =
                        "#F28C18";

                backgroundColor =
                        "#FFF2DF";

                break;


            case "validated":

                label =
                        "Validated";

                textColor =
                        "#6C2BD9";

                backgroundColor =
                        "#F1E9FF";

                break;


            case "referred":

                label =
                        "Referred";

                textColor =
                        "#21A65B";

                backgroundColor =
                        "#E7F8EC";

                break;


            case "resolved":
            case "closed":

                label =
                        "Resolved ✓";

                textColor =
                        "#21A65B";

                backgroundColor =
                        "#E7F8EC";

                break;


            default:

                label =
                        formatDatabaseText(
                                status
                        );

                textColor =
                        "#6C2BD9";

                backgroundColor =
                        "#F1E9FF";

                break;
        }


        TextView badge =
                createTextView(
                        label,
                        8.8f,
                        textColor,
                        true
                );


        badge.setGravity(
                Gravity.CENTER
        );

        badge.setPadding(
                dpToPx(10),
                dpToPx(5),
                dpToPx(10),
                dpToPx(5)
        );

        badge.setSingleLine(
                true
        );

        badge.setBackground(
                createRoundedBackground(
                        backgroundColor,
                        15
                )
        );


        return badge;
    }


    // =====================================================
    // STATUS CURRENT STEP COLOR
    // =====================================================

    private String getCurrentStepColor(
            String reportStatus
    ) {

        String status =
                reportStatus == null
                        ? ""
                        : reportStatus
                          .trim()
                          .toLowerCase(
                                  Locale.US
                          );


        // Progress tracker palette:
        // Purple = completed steps / Submitted / Resolved
        // Orange = current middle status
        // Gray = future steps

        switch (status) {

            case "under_review":
            case "validated":
            case "referred":
                return "#FFA126";

            case "resolved":
            case "closed":
            case "submitted":
            default:
                return "#7B3FF2";
        }
    }


    // =====================================================
    // NO RECENT REPORTS
    // =====================================================

    private void showNoRecentReports() {

        if (recentReportsContainer == null
                || textNoRecentReports == null) {

            return;
        }


        recentReportsContainer.removeAllViews();


        textNoRecentReports.setVisibility(
                View.VISIBLE
        );


        recentReportsContainer.addView(
                textNoRecentReports
        );
    }


    // =====================================================
    // UPDATE OVERVIEW TEXT
    // =====================================================

    private void setOverviewCounts(
            int total,
            int active,
            int resolved,
            int drafts
    ) {

        textTotalReports.setText(
                String.valueOf(total)
        );

        textActiveReports.setText(
                String.valueOf(active)
        );

        textResolvedReports.setText(
                String.valueOf(resolved)
        );

        textDraftReports.setText(
                String.valueOf(drafts)
        );
    }


    // =====================================================
    // SUPPORT PAGE ACTIONS
    // =====================================================

    private void setupSupportActions() {

        setupFileNewReportButton();
        setupMyReportsButton();
        setupDraftsButton();
        setupNeedHelpButton();
    }


    // =====================================================
    // FILE A NEW REPORT
    // =====================================================

    private void setupFileNewReportButton() {

        findViewById(R.id.btnFileNewReport)
                .setOnClickListener(v -> {

                    Intent intent =
                            new Intent(
                                    SupportActivity.this,
                                    AddReportActivity.class
                            );

                    startActivity(intent);
                });
    }


    // =====================================================
    // MY REPORTS
    // =====================================================

    private void setupMyReportsButton() {

        findViewById(R.id.btnMyReports)
                .setOnClickListener(v -> {

                    Intent intent =
                            new Intent(
                                    SupportActivity.this,
                                    MyReportsActivity.class
                            );

                    startActivity(intent);
                });
    }


    // =====================================================
    // DRAFTS
    // =====================================================

    private void setupDraftsButton() {

        findViewById(R.id.btnDrafts)
                .setOnClickListener(v -> {

                    Intent intent =
                            new Intent(
                                    SupportActivity.this,
                                    DraftReportsActivity.class
                            );

                    startActivity(intent);
                });
    }


    // =====================================================
// NEED HELP / HOPE AI ASSISTANT
// =====================================================

    private void setupNeedHelpButton() {

        findViewById(R.id.btnNeedHelp)
                .setOnClickListener(v -> {

                    Intent intent =
                            new Intent(
                                    SupportActivity.this,
                                    ChatbotActivity.class
                            );

                    startActivity(intent);
                });
    }


    // =====================================================
    // NOTIFICATION BELL
    // =====================================================

    private void setupNotification() {

        View notificationBell =
                findViewById(R.id.notificationBell);

        if (notificationBell == null) {

            Log.e(
                    TAG,
                    "NOTIFICATION BELL VIEW IS MISSING"
            );

            return;
        }

        notificationBell.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            SupportActivity.this,
                            NotificationsActivity.class
                    );

            startActivity(intent);
        });
    }


    // =====================================================
    // BOTTOM NAVIGATION
    // =====================================================

    private void setupNavigation() {


        // =================================================
        // HOME
        // =================================================

        findViewById(R.id.navHome)
                .setOnClickListener(v -> {

                    Intent intent =
                            new Intent(
                                    SupportActivity.this,
                                    HomeActivity.class
                            );

                    startActivity(intent);

                    finish();
                });


        // =================================================
        // SUPPORT
        // =================================================

        findViewById(R.id.navSupport)
                .setOnClickListener(v -> {

                    // Already on SupportActivity.
                    // No action needed.

                });


        // =================================================
        // EXPLORE
        // =================================================

        findViewById(R.id.navExplore)
                .setOnClickListener(v -> {

                    Intent intent =
                            new Intent(
                                    SupportActivity.this,
                                    ExploreActivity.class
                            );

                    startActivity(intent);

                    finish();
                });


        // =================================================
        // PROFILE
        // =================================================

        findViewById(R.id.navProfile)
                .setOnClickListener(v -> {

                    Intent intent =
                            new Intent(
                                    SupportActivity.this,
                                    ProfileActivity.class
                            );

                    startActivity(intent);

                    finish();
                });
    }


    // =====================================================
    // LOAD UNREAD NOTIFICATION COUNT
    // =====================================================

    private void loadUnreadNotificationCount() {

        if (txtSupportNotificationBadge == null
                || notificationAPI == null) {

            return;
        }

        SharedPreferences preferences =
                getSharedPreferences(
                        "HavenUser",
                        MODE_PRIVATE
                );

        String userId =
                preferences.getString(
                        "user_id",
                        ""
                );

        if (userId == null
                || userId.trim().isEmpty()) {

            userId =
                    preferences.getString(
                            "id",
                            ""
                    );
        }

        String accessToken =
                preferences.getString(
                        "access_token",
                        ""
                );

        if (userId == null
                || userId.trim().isEmpty()
                || accessToken == null
                || accessToken.trim().isEmpty()) {

            txtSupportNotificationBadge.setVisibility(
                    View.GONE
            );

            return;
        }

        notificationAPI
                .getUnreadNotifications(
                        "Bearer " + accessToken,
                        "eq." + userId,
                        "eq.false",
                        "id",
                        "created_at.desc"
                )
                .enqueue(
                        new Callback<List<ReportNotification>>() {

                            @Override
                            public void onResponse(
                                    Call<List<ReportNotification>> call,
                                    Response<List<ReportNotification>> response
                            ) {

                                if (!response.isSuccessful()) {

                                    Log.e(
                                            TAG,
                                            "UNREAD NOTIFICATION COUNT HTTP ERROR: "
                                                    + response.code()
                                    );

                                    return;
                                }

                                List<ReportNotification> notifications =
                                        response.body();

                                int unreadCount =
                                        notifications == null
                                                ? 0
                                                : notifications.size();

                                updateNotificationBadge(
                                        unreadCount
                                );
                            }

                            @Override
                            public void onFailure(
                                    Call<List<ReportNotification>> call,
                                    Throwable t
                            ) {

                                Log.e(
                                        TAG,
                                        "UNREAD NOTIFICATION COUNT NETWORK FAILURE",
                                        t
                                );

                                // Keep the current badge during a temporary
                                // network failure instead of making it flicker.
                            }
                        }
                );
    }


    // =====================================================
    // UPDATE NOTIFICATION BADGE
    // =====================================================

    private void updateNotificationBadge(
            int unreadCount
    ) {

        if (txtSupportNotificationBadge == null) {

            return;
        }

        if (unreadCount <= 0) {

            txtSupportNotificationBadge.setVisibility(
                    View.GONE
            );

            return;
        }

        String badgeText =
                unreadCount > 99
                        ? "99+"
                        : String.valueOf(unreadCount);

        txtSupportNotificationBadge.setText(
                badgeText
        );

        txtSupportNotificationBadge.setVisibility(
                View.VISIBLE
        );
    }


    // =====================================================
    // SAFE MAP VALUE
    // =====================================================

    private String getSafeValue(
            Map<String, Object> data,
            String key,
            String defaultValue
    ) {

        if (data == null) {

            return defaultValue;
        }


        Object value =
                data.get(
                        key
                );


        if (value == null) {

            return defaultValue;
        }


        String result =
                String.valueOf(
                        value
                ).trim();


        if (result.isEmpty()
                || result.equalsIgnoreCase(
                "null"
        )) {

            return defaultValue;
        }


        return result;
    }


    // =====================================================
    // CASE DISPLAY TEXT
    // =====================================================

    private String getCaseDisplayText(
            String caseNumber
    ) {

        if (caseNumber == null
                || caseNumber.trim().isEmpty()
                || caseNumber.equalsIgnoreCase(
                "Pending"
        )) {

            return "Report Pending";
        }


        return caseNumber.trim();
    }


    // =====================================================
    // FORMAT DATABASE TEXT
    // =====================================================

    private String formatDatabaseText(
            String value
    ) {

        if (value == null
                || value.trim().isEmpty()) {

            return "Submitted";
        }


        String cleaned =
                value
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
                            word.charAt(0)
                    )
            );


            if (word.length() > 1) {

                builder.append(
                        word.substring(1)
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
    // FORMAT RECENT REPORT DATE/TIME
    // =====================================================

    private String formatRecentDateTime(
            String databaseTimestamp
    ) {

        if (databaseTimestamp == null
                || databaseTimestamp.trim().isEmpty()) {

            return "Date not available";
        }


        String cleaned =
                normalizeTimestamp(
                        databaseTimestamp.trim()
                );


        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss"
        };


        for (String pattern : patterns) {

            try {

                SimpleDateFormat input =
                        new SimpleDateFormat(
                                pattern,
                                Locale.US
                        );

                input.setLenient(
                        false
                );


                if (pattern.endsWith(
                        "'Z'"
                )) {

                    input.setTimeZone(
                            TimeZone.getTimeZone(
                                    "UTC"
                            )
                    );
                }


                Date parsedDate =
                        input.parse(
                                cleaned
                        );


                if (parsedDate != null) {

                    SimpleDateFormat output =
                            new SimpleDateFormat(
                                    "MMM d, yyyy • h:mm a",
                                    Locale.US
                            );

                    output.setTimeZone(
                            TimeZone.getDefault()
                    );


                    return output.format(
                            parsedDate
                    );
                }

            } catch (ParseException ignored) {
            }
        }


        if (databaseTimestamp.length() >= 10) {

            return databaseTimestamp.substring(
                    0,
                    10
            );
        }


        return databaseTimestamp;
    }


    // =====================================================
    // NORMALIZE SUPABASE TIMESTAMP FRACTION
    // =====================================================

    private String normalizeTimestamp(
            String timestamp
    ) {

        if (timestamp == null
                || timestamp.trim().isEmpty()) {

            return "";
        }


        String cleaned =
                timestamp.trim();


        int dotIndex =
                cleaned.indexOf(
                        '.'
                );


        if (dotIndex < 0) {

            return cleaned;
        }


        int fractionEnd =
                dotIndex + 1;


        while (fractionEnd < cleaned.length()
                && Character.isDigit(
                cleaned.charAt(
                        fractionEnd
                )
        )) {

            fractionEnd++;
        }


        String fraction =
                cleaned.substring(
                        dotIndex + 1,
                        fractionEnd
                );


        if (fraction.length() > 3) {

            fraction =
                    fraction.substring(
                            0,
                            3
                    );

        } else {

            while (fraction.length() < 3) {

                fraction +=
                        "0";
            }
        }


        return cleaned.substring(
                0,
                dotIndex + 1
        )
                + fraction
                + cleaned.substring(
                fractionEnd
        );
    }


    // =====================================================
    // CREATE TEXT VIEW
    // =====================================================

    private TextView createTextView(
            String text,
            float textSize,
            String textColor,
            boolean semibold
    ) {

        TextView textView =
                new TextView(
                        SupportActivity.this
                );

        textView.setText(
                text
        );

        textView.setTextSize(
                textSize
        );

        textView.setTextColor(
                Color.parseColor(
                        textColor
                )
        );


        if (semibold) {

            textView.setTypeface(
                    getPoppinsSemibold(),
                    Typeface.BOLD
            );

        } else {

            textView.setTypeface(
                    getPoppinsRegular(),
                    Typeface.NORMAL
            );
        }


        return textView;
    }


    // =====================================================
    // POPPINS FONTS
    // =====================================================

    private Typeface getPoppinsRegular() {

        Typeface typeface =
                ResourcesCompat.getFont(
                        SupportActivity.this,
                        R.font.poppins_regular
                );

        return typeface != null
                ? typeface
                : Typeface.DEFAULT;
    }


    private Typeface getPoppinsSemibold() {

        Typeface typeface =
                ResourcesCompat.getFont(
                        SupportActivity.this,
                        R.font.poppins_semibold
                );

        return typeface != null
                ? typeface
                : Typeface.DEFAULT_BOLD;
    }


    // =====================================================
    // ROUNDED BACKGROUND
    // =====================================================

    private GradientDrawable createRoundedBackground(
            String color,
            int radiusDp
    ) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(
                Color.parseColor(
                        color
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
    // DP TO PX
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
