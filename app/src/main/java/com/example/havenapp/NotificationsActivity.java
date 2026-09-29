package com.example.havenapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.havenapp.adapter.ReportNotificationAdapter;
import com.example.havenapp.api.RefreshTokenRequest;
import com.example.havenapp.api.ReportNotificationAPI;
import com.example.havenapp.api.SupabaseAuthAPI;
import com.example.havenapp.api.SupabaseClient;
import com.example.havenapp.model.AuthResponse;
import com.example.havenapp.model.ReportNotification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsActivity extends AppCompatActivity {

    private static final String TAG =
            "REPORT_NOTIFICATIONS";


    // =========================================================
    // VIEWS
    // =========================================================

    private View btnNotificationsBack;

    private TextView txtMarkAllAsRead;

    private RecyclerView recyclerNotifications;

    private ProgressBar progressNotifications;

    private View layoutNotificationsEmpty;

    private View cardHopeAssistant;


    // =========================================================
    // ADAPTER
    // =========================================================

    private ReportNotificationAdapter notificationAdapter;


    // =========================================================
    // API
    // =========================================================

    private ReportNotificationAPI notificationAPI;

    private SupabaseAuthAPI authAPI;


    // =========================================================
    // USER SESSION
    // =========================================================

    /*
     * IMPORTANT:
     *
     * userId here is public.users.id.
     *
     * It is NOT auth.uid().
     *
     * report_notifications.user_id stores public.users.id,
     * so the REST filter must use this value.
     */

    private String userId = "";

    private String accessToken = "";


    // =========================================================
    // SESSION REFRESH
    // =========================================================

    private boolean isRefreshingToken = false;

    private boolean retryLoadAfterRefresh = false;

    private boolean retryMarkAllAfterRefresh = false;

    private ReportNotification
            retryNotificationAfterRefresh = null;


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_notifications
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


        initializeViews();

        initializeAPI();

        loadSession();

        setupRecyclerView();

        setupActions();

        loadNotifications(false);
    }


    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initializeViews() {

        btnNotificationsBack =
                findViewById(
                        R.id.btnNotificationsBack
                );

        txtMarkAllAsRead =
                findViewById(
                        R.id.txtMarkAllAsRead
                );

        recyclerNotifications =
                findViewById(
                        R.id.recyclerNotifications
                );

        progressNotifications =
                findViewById(
                        R.id.progressNotifications
                );

        layoutNotificationsEmpty =
                findViewById(
                        R.id.layoutNotificationsEmpty
                );
    }


    // =========================================================
    // INITIALIZE API
    // =========================================================

    private void initializeAPI() {

        notificationAPI =
                SupabaseClient
                        .getClient()
                        .create(
                                ReportNotificationAPI.class
                        );

        authAPI =
                SupabaseClient
                        .getClient()
                        .create(
                                SupabaseAuthAPI.class
                        );
    }


    // =========================================================
    // LOAD SESSION
    // =========================================================

    private void loadSession() {

        SharedPreferences preferences =
                getSharedPreferences(
                        "HavenUser",
                        MODE_PRIVATE
                );


        // -----------------------------------------------------
        // ACCESS TOKEN
        // -----------------------------------------------------

        accessToken =
                preferences.getString(
                        "access_token",
                        ""
                );


        // -----------------------------------------------------
        // HAVENAPP USER ID
        //
        // report_notifications.user_id references users.id,
        // not users.auth_id.
        // -----------------------------------------------------

        userId =
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


        if (userId == null) {
            userId = "";
        }

        if (accessToken == null) {
            accessToken = "";
        }


        userId =
                userId.trim();

        accessToken =
                accessToken.trim();


        /*
         * Safe debugging only.
         *
         * Never log the actual access token.
         */

        Log.d(
                TAG,
                "USER ID AVAILABLE: "
                        + !userId.isEmpty()
        );

        Log.d(
                TAG,
                "ACCESS TOKEN AVAILABLE: "
                        + !accessToken.isEmpty()
        );
    }


    // =========================================================
    // RELOAD ACCESS TOKEN FROM STORAGE
    // =========================================================

    private void reloadAccessToken() {

        SharedPreferences preferences =
                getSharedPreferences(
                        "HavenUser",
                        MODE_PRIVATE
                );


        String savedAccessToken =
                preferences.getString(
                        "access_token",
                        ""
                );


        if (savedAccessToken == null) {
            savedAccessToken = "";
        }


        accessToken =
                savedAccessToken.trim();
    }


    // =========================================================
    // SETUP RECYCLER VIEW
    // =========================================================

    private void setupRecyclerView() {

        recyclerNotifications.setLayoutManager(
                new LinearLayoutManager(this)
        );


        notificationAdapter =
                new ReportNotificationAdapter(
                        this::handleNotificationClick
                );


        recyclerNotifications.setAdapter(
                notificationAdapter
        );
    }


    // =========================================================
    // SETUP ACTIONS
    // =========================================================

    private void setupActions() {

        btnNotificationsBack
                .setOnClickListener(
                        v -> finish()
                );


        txtMarkAllAsRead
                .setOnClickListener(
                        v -> markAllAsRead(false)
                );
    }


    // =========================================================
    // LOAD NOTIFICATIONS
    // =========================================================

    private void loadNotifications(
            boolean alreadyRetried
    ) {

        reloadAccessToken();


        if (userId.isEmpty()
                || accessToken.isEmpty()) {

            showEmptyState();

            Toast.makeText(
                    this,
                    "Unable to load notifications.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        showLoading();


        String bearerToken =
                "Bearer " + accessToken;


        notificationAPI
                .getNotifications(
                        bearerToken,
                        "eq." + userId,
                        "*",
                        "created_at.desc"
                )
                .enqueue(
                        new Callback<List<ReportNotification>>() {

                            @Override
                            public void onResponse(
                                    Call<List<ReportNotification>> call,
                                    Response<List<ReportNotification>> response
                            ) {

                                // ---------------------------------
                                // EXPIRED ACCESS TOKEN
                                // ---------------------------------

                                if (response.code() == 401
                                        && !alreadyRetried) {

                                    Log.d(
                                            TAG,
                                            "LOAD RECEIVED 401. REFRESHING SESSION."
                                    );


                                    requestSessionRefresh(
                                            RefreshAction.LOAD,
                                            null
                                    );

                                    return;
                                }


                                hideLoading();


                                if (!response.isSuccessful()) {

                                    showEmptyState();


                                    Log.e(
                                            TAG,
                                            "LOAD HTTP ERROR: "
                                                    + response.code()
                                    );


                                    Toast.makeText(
                                            NotificationsActivity.this,
                                            "Unable to load notifications.",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    return;
                                }


                                List<ReportNotification>
                                        notifications =
                                        response.body();


                                if (notifications == null
                                        || notifications.isEmpty()) {

                                    notificationAdapter
                                            .setNotifications(
                                                    null
                                            );

                                    showEmptyState();

                                    return;
                                }


                                hideEmptyState();


                                notificationAdapter
                                        .setNotifications(
                                                notifications
                                        );
                            }


                            @Override
                            public void onFailure(
                                    Call<List<ReportNotification>> call,
                                    Throwable t
                            ) {

                                hideLoading();

                                showEmptyState();


                                Log.e(
                                        TAG,
                                        "LOAD NETWORK FAILURE",
                                        t
                                );


                                Toast.makeText(
                                        NotificationsActivity.this,
                                        "Unable to connect to the server.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                );
    }


    // =========================================================
    // NOTIFICATION CLICK
    // =========================================================

    private void handleNotificationClick(
            ReportNotification notification
    ) {

        if (notification == null) {
            return;
        }


        /*
         * If already read, open details immediately.
         *
         * If unread, mark it as read first and then
         * open Notification Details after the PATCH
         * succeeds.
         */

        if (notification.isRead()) {

            openNotificationDetails(
                    notification
            );

            return;
        }


        markNotificationAsRead(
                notification,
                false
        );
    }


    // =========================================================
    // MARK ONE NOTIFICATION AS READ
    // =========================================================

    private void markNotificationAsRead(
            ReportNotification notification,
            boolean alreadyRetried
    ) {

        if (notification == null) {
            return;
        }


        if (notification.getId() == null
                || notification
                .getId()
                .trim()
                .isEmpty()) {

            openNotificationDetails(
                    notification
            );

            return;
        }


        reloadAccessToken();


        if (accessToken.isEmpty()) {

            Toast.makeText(
                    this,
                    "Unable to update notification.",
                    Toast.LENGTH_SHORT
            ).show();

            openNotificationDetails(
                    notification
            );

            return;
        }


        Map<String, Object> updateData =
                new HashMap<>();


        /*
         * Android changes ONLY is_read.
         */

        updateData.put(
                "is_read",
                true
        );


        notificationAPI
                .markNotificationAsRead(
                        "Bearer " + accessToken,
                        "eq." + notification.getId(),
                        "eq." + userId,
                        updateData
                )
                .enqueue(
                        new Callback<Void>() {

                            @Override
                            public void onResponse(
                                    Call<Void> call,
                                    Response<Void> response
                            ) {

                                // ---------------------------------
                                // EXPIRED ACCESS TOKEN
                                // ---------------------------------

                                if (response.code() == 401
                                        && !alreadyRetried) {

                                    Log.d(
                                            TAG,
                                            "MARK ONE RECEIVED 401. REFRESHING SESSION."
                                    );


                                    requestSessionRefresh(
                                            RefreshAction.MARK_ONE,
                                            notification
                                    );

                                    return;
                                }


                                if (response.isSuccessful()) {

                                    notification.setRead(
                                            true
                                    );


                                    notificationAdapter
                                            .notifyDataSetChanged();


                                    openNotificationDetails(
                                            notification
                                    );

                                    return;
                                }


                                Log.e(
                                        TAG,
                                        "MARK ONE HTTP ERROR: "
                                                + response.code()
                                );


                                Toast.makeText(
                                        NotificationsActivity.this,
                                        "Could not update read status.",
                                        Toast.LENGTH_SHORT
                                ).show();


                                /*
                                 * Even if the read-status update fails,
                                 * still allow the user to view the
                                 * notification.
                                 */

                                openNotificationDetails(
                                        notification
                                );
                            }


                            @Override
                            public void onFailure(
                                    Call<Void> call,
                                    Throwable t
                            ) {

                                Log.e(
                                        TAG,
                                        "MARK ONE NETWORK FAILURE",
                                        t
                                );


                                Toast.makeText(
                                        NotificationsActivity.this,
                                        "Could not update read status.",
                                        Toast.LENGTH_SHORT
                                ).show();


                                openNotificationDetails(
                                        notification
                                );
                            }
                        }
                );
    }


    // =========================================================
    // MARK ALL AS READ
    // =========================================================

    private void markAllAsRead(
            boolean alreadyRetried
    ) {

        reloadAccessToken();


        if (userId.isEmpty()
                || accessToken.isEmpty()) {

            return;
        }


        txtMarkAllAsRead
                .setEnabled(
                        false
                );


        Map<String, Object> updateData =
                new HashMap<>();


        /*
         * Android changes ONLY is_read.
         */

        updateData.put(
                "is_read",
                true
        );


        notificationAPI
                .markAllNotificationsAsRead(
                        "Bearer " + accessToken,
                        "eq." + userId,
                        "eq.false",
                        updateData
                )
                .enqueue(
                        new Callback<Void>() {

                            @Override
                            public void onResponse(
                                    Call<Void> call,
                                    Response<Void> response
                            ) {

                                // ---------------------------------
                                // EXPIRED ACCESS TOKEN
                                // ---------------------------------

                                if (response.code() == 401
                                        && !alreadyRetried) {

                                    Log.d(
                                            TAG,
                                            "MARK ALL RECEIVED 401. REFRESHING SESSION."
                                    );


                                    requestSessionRefresh(
                                            RefreshAction.MARK_ALL,
                                            null
                                    );

                                    return;
                                }


                                txtMarkAllAsRead
                                        .setEnabled(
                                                true
                                        );


                                if (response.isSuccessful()) {

                                    Toast.makeText(
                                            NotificationsActivity.this,
                                            "All notifications marked as read.",
                                            Toast.LENGTH_SHORT
                                    ).show();


                                    loadNotifications(
                                            false
                                    );

                                    return;
                                }


                                Log.e(
                                        TAG,
                                        "MARK ALL HTTP ERROR: "
                                                + response.code()
                                );


                                Toast.makeText(
                                        NotificationsActivity.this,
                                        "Unable to mark notifications as read.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }


                            @Override
                            public void onFailure(
                                    Call<Void> call,
                                    Throwable t
                            ) {

                                txtMarkAllAsRead
                                        .setEnabled(
                                                true
                                        );


                                Log.e(
                                        TAG,
                                        "MARK ALL NETWORK FAILURE",
                                        t
                                );


                                Toast.makeText(
                                        NotificationsActivity.this,
                                        "Unable to connect to the server.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                );
    }


    // =========================================================
    // REFRESH ACTION
    // =========================================================

    private enum RefreshAction {

        LOAD,

        MARK_ONE,

        MARK_ALL
    }


    // =========================================================
    // REFRESH EXPIRED SUPABASE SESSION
    // =========================================================

    private void requestSessionRefresh(
            RefreshAction action,
            ReportNotification notification
    ) {

        // -----------------------------------------------------
        // REMEMBER WHAT MUST BE RETRIED
        // -----------------------------------------------------

        if (action == RefreshAction.LOAD) {

            retryLoadAfterRefresh =
                    true;
        }


        if (action == RefreshAction.MARK_ONE) {

            retryNotificationAfterRefresh =
                    notification;
        }


        if (action == RefreshAction.MARK_ALL) {

            retryMarkAllAfterRefresh =
                    true;
        }


        // -----------------------------------------------------
        // PREVENT MULTIPLE REFRESH REQUESTS
        // -----------------------------------------------------

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


            clearRefreshRetryState();

            hideLoading();

            txtMarkAllAsRead
                    .setEnabled(
                            true
                    );


            Toast.makeText(
                    this,
                    "Your session has expired. Please log in again.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        isRefreshingToken =
                true;


        authAPI
                .refreshToken(
                        "refresh_token",
                        new RefreshTokenRequest(
                                refreshToken.trim()
                        )
                )
                .enqueue(
                        new Callback<AuthResponse>() {

                            @Override
                            public void onResponse(
                                    Call<AuthResponse> call,
                                    Response<AuthResponse> response
                            ) {

                                isRefreshingToken =
                                        false;


                                if (!response.isSuccessful()
                                        || response.body() == null) {

                                    Log.e(
                                            TAG,
                                            "SESSION REFRESH HTTP ERROR: "
                                                    + response.code()
                                    );


                                    clearRefreshRetryState();

                                    hideLoading();

                                    txtMarkAllAsRead
                                            .setEnabled(
                                                    true
                                            );


                                    Toast.makeText(
                                            NotificationsActivity.this,
                                            "Your session has expired. Please log in again.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }


                                AuthResponse authResponse =
                                        response.body();


                                String newAccessToken =
                                        authResponse
                                                .getAccess_token();


                                String newRefreshToken =
                                        authResponse
                                                .getRefresh_token();


                                if (newAccessToken == null
                                        || newAccessToken
                                        .trim()
                                        .isEmpty()) {

                                    Log.e(
                                            TAG,
                                            "SESSION REFRESH RETURNED EMPTY ACCESS TOKEN"
                                    );


                                    clearRefreshRetryState();

                                    hideLoading();

                                    txtMarkAllAsRead
                                            .setEnabled(
                                                    true
                                            );


                                    Toast.makeText(
                                            NotificationsActivity.this,
                                            "Your session has expired. Please log in again.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }


                                // ---------------------------------
                                // SAVE NEW SESSION
                                // ---------------------------------

                                SharedPreferences.Editor editor =
                                        preferences.edit();


                                editor.putString(
                                        "access_token",
                                        newAccessToken
                                );


                                /*
                                 * Supabase may rotate the refresh
                                 * token. Save the new one when
                                 * returned.
                                 */

                                if (newRefreshToken != null
                                        && !newRefreshToken
                                        .trim()
                                        .isEmpty()) {

                                    editor.putString(
                                            "refresh_token",
                                            newRefreshToken
                                    );
                                }


                                editor.apply();


                                accessToken =
                                        newAccessToken.trim();


                                Log.d(
                                        TAG,
                                        "SESSION REFRESH SUCCESSFUL"
                                );


                                // ---------------------------------
                                // CAPTURE RETRIES
                                // ---------------------------------

                                boolean shouldRetryLoad =
                                        retryLoadAfterRefresh;


                                boolean shouldRetryMarkAll =
                                        retryMarkAllAfterRefresh;


                                ReportNotification
                                        notificationToRetry =
                                        retryNotificationAfterRefresh;


                                // ---------------------------------
                                // CLEAR FLAGS BEFORE RETRY
                                // ---------------------------------

                                clearRefreshRetryState();


                                // ---------------------------------
                                // RETRY FAILED REQUEST ONCE
                                // ---------------------------------

                                if (notificationToRetry != null) {

                                    markNotificationAsRead(
                                            notificationToRetry,
                                            true
                                    );
                                }


                                if (shouldRetryMarkAll) {

                                    markAllAsRead(
                                            true
                                    );
                                }


                                if (shouldRetryLoad) {

                                    loadNotifications(
                                            true
                                    );
                                }
                            }


                            @Override
                            public void onFailure(
                                    Call<AuthResponse> call,
                                    Throwable t
                            ) {

                                isRefreshingToken =
                                        false;


                                Log.e(
                                        TAG,
                                        "SESSION REFRESH NETWORK FAILURE",
                                        t
                                );


                                clearRefreshRetryState();

                                hideLoading();

                                txtMarkAllAsRead
                                        .setEnabled(
                                                true
                                        );


                                Toast.makeText(
                                        NotificationsActivity.this,
                                        "Unable to refresh your session.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                );
    }


    // =========================================================
    // CLEAR REFRESH RETRY STATE
    // =========================================================

    private void clearRefreshRetryState() {

        retryLoadAfterRefresh =
                false;

        retryMarkAllAfterRefresh =
                false;

        retryNotificationAfterRefresh =
                null;
    }


    // =========================================================
    // OPEN NOTIFICATION DETAILS
    // =========================================================

    private void openNotificationDetails(
            ReportNotification notification
    ) {

        if (notification == null) {
            return;
        }


        Intent intent =
                new Intent(
                        NotificationsActivity.this,
                        NotificationDetailsActivity.class
                );


        // -----------------------------------------------------
        // TITLE
        // -----------------------------------------------------

        intent.putExtra(
                "notification_title",
                notification.getTitle()
        );


        // -----------------------------------------------------
        // MESSAGE
        // -----------------------------------------------------

        intent.putExtra(
                "notification_message",
                notification.getMessage()
        );


        // -----------------------------------------------------
        // NOTIFICATION TYPE
        // submitted / under_review / validated /
        // referred / resolved / closed
        // -----------------------------------------------------

        intent.putExtra(
                "notification_type",
                notification.getNotificationType()
        );


        // -----------------------------------------------------
        // CASE NUMBER
        // -----------------------------------------------------

        intent.putExtra(
                "case_number",
                notification.getCaseNumber()
        );


        // -----------------------------------------------------
        // CREATED DATE / TIME
        // -----------------------------------------------------

        intent.putExtra(
                "created_at",
                notification.getCreatedAt()
        );


        // -----------------------------------------------------
        // REPORT ID
        //
        // We are passing this now so it is available later
        // if Notification Details needs to connect to the
        // corresponding Report Details page.
        // -----------------------------------------------------

        intent.putExtra(
                "report_id",
                notification.getReportId()
        );


        startActivity(
                intent
        );
    }


    // =========================================================
    // LOADING
    // =========================================================

    private void showLoading() {

        progressNotifications
                .setVisibility(
                        View.VISIBLE
                );


        recyclerNotifications
                .setVisibility(
                        View.GONE
                );


        layoutNotificationsEmpty
                .setVisibility(
                        View.GONE
                );
    }


    private void hideLoading() {

        progressNotifications
                .setVisibility(
                        View.GONE
                );
    }


    // =========================================================
    // EMPTY STATE
    // =========================================================

    private void showEmptyState() {

        recyclerNotifications
                .setVisibility(
                        View.GONE
                );


        layoutNotificationsEmpty
                .setVisibility(
                        View.VISIBLE
                );
    }


    private void hideEmptyState() {

        layoutNotificationsEmpty
                .setVisibility(
                        View.GONE
                );


        recyclerNotifications
                .setVisibility(
                        View.VISIBLE
                );
    }
}