package com.example.havenapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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
import com.example.havenapp.api.SupabaseClient;

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

public class DraftReportsActivity extends AppCompatActivity {

    private static final String TAG = "DRAFT_REPORTS";

    private IncidentReportAPI incidentReportAPI;

    private LinearLayout draftsContainer;
    private LinearLayout emptyDraftsContainer;

    // =====================================================
    // HOPE ASSISTANT
    // =====================================================

    private View cardHopeAssistant;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.draftreports_activity
        );


        // =====================================================
        // HOPE ASSISTANT
        // =====================================================

        cardHopeAssistant =
                findViewById(
                        R.id.cardHopeAssistant
                );

        if (cardHopeAssistant != null) {

            ChatbotFloatingHelper.setup(
                    this,
                    cardHopeAssistant
            );
        }


        // =====================================================
        // INITIALIZE VIEWS
        // =====================================================

        draftsContainer =
                findViewById(
                        R.id.draftsContainer
                );

        emptyDraftsContainer =
                findViewById(
                        R.id.emptyDraftsContainer
                );


        // =====================================================
        // INITIALIZE API
        // =====================================================

        incidentReportAPI =
                SupabaseClient
                        .getClient()
                        .create(
                                IncidentReportAPI.class
                        );


        setupBackButton();
        setupCreateReportButton();
    }


    @Override
    protected void onResume() {

        super.onResume();

        loadDraftReports();
    }


    // =====================================================
    // BACK BUTTON
    // =====================================================

    private void setupBackButton() {

        findViewById(R.id.btnBack)
                .setOnClickListener(
                        v -> finish()
                );
    }


    // =====================================================
    // CREATE REPORT BUTTON
    // =====================================================

    private void setupCreateReportButton() {

        findViewById(R.id.btnCreateReport)
                .setOnClickListener(
                        v -> {

                            Intent intent =
                                    new Intent(
                                            DraftReportsActivity.this,
                                            AddReportActivity.class
                                    );

                            startActivity(intent);
                        }
                );
    }


    // =====================================================
    // LOAD DRAFT REPORTS
    // =====================================================

    private void loadDraftReports() {

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


        if (accessToken == null
                || accessToken.trim().isEmpty()
                || userId == null
                || userId.trim().isEmpty()) {

            Log.e(
                    TAG,
                    "ACCESS TOKEN OR USER ID IS MISSING"
            );

            showEmptyState();

            return;
        }


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
                                            DraftReportsActivity.this,
                                            "Unable to load your drafts.",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    showEmptyState();

                                    return;
                                }


                                draftsContainer.removeAllViews();

                                List<Map<String, Object>> reports =
                                        response.body();

                                int draftCount =
                                        0;


                                if (reports != null) {

                                    for (
                                            Map<String, Object> report
                                            : reports
                                    ) {

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

                                        if (
                                                reportStatus.equals(
                                                        "draft"
                                                )
                                        ) {

                                            draftCount++;
                                        }
                                    }


                                    int currentDraftNumber =
                                            draftCount;

                                    for (
                                            Map<String, Object> report
                                            : reports
                                    ) {

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


                                        if (
                                                !reportStatus.equals(
                                                        "draft"
                                                )
                                        ) {

                                            continue;
                                        }


                                        addDraftCard(
                                                report,
                                                currentDraftNumber
                                        );

                                        currentDraftNumber--;
                                    }
                                }


                                if (draftCount == 0) {

                                    showEmptyState();

                                } else {

                                    emptyDraftsContainer.setVisibility(
                                            View.GONE
                                    );

                                    draftsContainer.setVisibility(
                                            View.VISIBLE
                                    );
                                }


                                Log.d(
                                        TAG,
                                        "DRAFTS DISPLAYED: "
                                                + draftCount
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
                                        DraftReportsActivity.this,
                                        "Unable to load your drafts.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                showEmptyState();
                            }
                        }
                );
    }


    // =====================================================
    // ADD DRAFT CARD
    // =====================================================

    private void addDraftCard(
            Map<String, Object> report,
            int draftNumber
    ) {

        String reportId =
                getSafeValue(
                        report,
                        "id",
                        ""
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
        // MAIN ROW
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

        ImageView iconView =
                new ImageView(
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
                ImageView.ScaleType.FIT_CENTER
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
        // MIDDLE DETAILS
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


        String draftTitle =
                String.format(
                        Locale.US,
                        "Draft %05d",
                        draftNumber
                );


        TextView title =
                createTextView(
                        draftTitle,
                        14f,
                        "#24134C",
                        true
                );


        TextView savedDateText =
                createTextView(
                        "Saved " + formatDateTime(createdAt),
                        10f,
                        "#827894",
                        false
                );

        LinearLayout.LayoutParams savedParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        savedParams.topMargin =
                dpToPx(2);

        savedDateText.setLayoutParams(
                savedParams
        );


        details.addView(
                title
        );

        details.addView(
                savedDateText
        );


        // =====================================================
        // RIGHT SIDE: CHEVRON ONLY
        // =====================================================

        TextView arrowView =
                createTextView(
                        "›",
                        28f,
                        "#6C2BD9",
                        true
                );

        LinearLayout.LayoutParams arrowParams =
                new LinearLayout.LayoutParams(
                        dpToPx(30),
                        dpToPx(40)
                );

        arrowView.setLayoutParams(
                arrowParams
        );

        arrowView.setGravity(
                Gravity.CENTER
        );


        row.addView(
                iconView
        );

        row.addView(
                details
        );

        row.addView(
                arrowView
        );

        cardView.addView(
                row
        );


        // =====================================================
        // OPEN DRAFT
        // =====================================================

        if (!reportId.isEmpty()) {

            cardView.setOnClickListener(
                    v -> {

                        Intent intent =
                                new Intent(
                                        DraftReportsActivity.this,
                                        AddReportActivity.class
                                );

                        intent.putExtra(
                                "draft_id",
                                reportId
                        );

                        startActivity(
                                intent
                        );
                    }
            );
        }


        draftsContainer.addView(
                cardView
        );
    }


    // =====================================================
    // EMPTY STATE
    // =====================================================

    private void showEmptyState() {

        draftsContainer.removeAllViews();

        draftsContainer.setVisibility(
                View.GONE
        );

        emptyDraftsContainer.setVisibility(
                View.VISIBLE
        );
    }


    // =====================================================
    // SAFE VALUE
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


        if (
                result.isEmpty()
                        || result.equalsIgnoreCase(
                        "null"
                )
        ) {

            return defaultValue;
        }


        return result;
    }


    // =====================================================
    // FORMAT DATE TIME
    // =====================================================

    private String formatDateTime(
            String databaseTimestamp
    ) {

        if (
                databaseTimestamp == null
                        || databaseTimestamp.trim().isEmpty()
        ) {

            return "date not available";
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


                if (
                        pattern.endsWith(
                                "'Z'"
                        )
                                || pattern.equals(
                                "yyyy-MM-dd HH:mm:ss"
                        )
                ) {

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
                            TimeZone.getTimeZone(
                                    "Asia/Manila"
                            )
                    );


                    return output.format(
                            parsedDate
                    );
                }

            } catch (ParseException ignored) {
            }
        }


        return databaseTimestamp;
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
                        this
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


        Typeface typeface =
                ResourcesCompat.getFont(
                        this,
                        semibold
                                ? R.font.poppins_semibold
                                : R.font.poppins_regular
                );

        textView.setTypeface(
                typeface != null
                        ? typeface
                        : semibold
                          ? Typeface.DEFAULT_BOLD
                          : Typeface.DEFAULT
        );


        return textView;
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
    // NORMALIZE TIMESTAMP
    // =====================================================

    private String normalizeTimestamp(
            String timestamp
    ) {

        if (
                timestamp == null
                        || timestamp.trim().isEmpty()
        ) {

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


        while (
                fractionEnd < cleaned.length()
                        && Character.isDigit(
                        cleaned.charAt(
                                fractionEnd
                        )
                )
        ) {

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