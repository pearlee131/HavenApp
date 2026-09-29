package com.example.havenapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.havenapp.adapter.AnnouncementAdapter;
import com.example.havenapp.adapter.TinigEpisodeAdapter;

import com.example.havenapp.api.AnnouncementAPI;
import com.example.havenapp.api.ReportNotificationAPI;
import com.example.havenapp.api.SupabaseClient;
import com.example.havenapp.api.TinigEpisodeAPI;

import com.example.havenapp.model.Announcement;
import com.example.havenapp.model.ReportNotification;
import com.example.havenapp.model.TinigEpisode;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class ExploreActivity extends AppCompatActivity {

    private static final String TAG =
            "ExploreActivity";


    // =====================================================
    // REPORT NOTIFICATION BADGE
    // =====================================================

    private static final long NOTIFICATION_BADGE_REFRESH_INTERVAL_MS = 5000L;

    private TextView txtExploreNotificationBadge;
    private ReportNotificationAPI notificationAPI;

    private final Handler notificationBadgeHandler =
            new Handler(Looper.getMainLooper());

    private final Runnable notificationBadgeRefreshRunnable =
            new Runnable() {

                @Override
                public void run() {

                    loadUnreadNotificationCount();

                    notificationBadgeHandler.postDelayed(
                            this,
                            NOTIFICATION_BADGE_REFRESH_INTERVAL_MS
                    );
                }
            };


    // =====================================================
    // INFORMATION HUB PRESS ANIMATION
    // =====================================================

    private static final float HUB_PRESSED_SCALE = 1.06f;
    private static final float HUB_NORMAL_SCALE = 1.0f;

    private static final long HUB_PRESS_DURATION = 100L;
    private static final long HUB_RELEASE_DURATION = 120L;


    // =====================================================
    // LATEST ANNOUNCEMENTS
    // =====================================================

    private RecyclerView recyclerAnnouncements;

    private AnnouncementAdapter announcementAdapter;

    private final List<Announcement> announcements =
            new ArrayList<>();

    private AnnouncementAPI announcementAPI;


    // =====================================================
    // TNK EPISODES
    // =====================================================

    private RecyclerView recyclerTinigEpisodes;

    private TinigEpisodeAdapter tinigEpisodeAdapter;

    private final List<TinigEpisode> tinigEpisodes =
            new ArrayList<>();

    private TinigEpisodeAPI tinigEpisodeAPI;


    // =====================================================
    // ACTIVITY STATE
    // =====================================================

    /*
     * onResume() is also called immediately after onCreate().
     *
     * We use this flag so the first load happens only once.
     * After that, every time the user returns to Explore,
     * announcements and TNK are refreshed.
     */
    private boolean firstResume = true;


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
                R.layout.explore_activity
        );


        // -------------------------------------------------
        // REPORT NOTIFICATION BADGE
        // -------------------------------------------------

        txtExploreNotificationBadge =
                findViewById(
                        R.id.txtExploreNotificationBadge
                );

        notificationAPI =
                SupabaseClient
                        .getClient()
                        .create(
                                ReportNotificationAPI.class
                        );

        findViewById(
                R.id.notificationBell
        ).setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            ExploreActivity.this,
                            NotificationsActivity.class
                    );

            startActivity(
                    intent
            );
        });


        // -------------------------------------------------
        // SETUP STATIC PAGE ACTIONS
        // -------------------------------------------------

        setupExploreActions();


        // -------------------------------------------------
        // INFORMATION HUB PRESS EFFECT
        // -------------------------------------------------

        setupInformationHubPressAnimations();


        // -------------------------------------------------
        // SETUP RECYCLER VIEWS / APIs
        // -------------------------------------------------

        setupAnnouncements();

        setupTinigEpisodes();


// -------------------------------------------------
// FLOATING HOPE ASSISTANT
// -------------------------------------------------

        setupHopeAssistant();


// -------------------------------------------------
// BOTTOM NAVIGATION
// -------------------------------------------------

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
    // ON RESUME
    // =====================================================

    @Override
    protected void onResume() {

        super.onResume();


        // -------------------------------------------------
        // START REPORT NOTIFICATION BADGE AUTO-REFRESH
        // -------------------------------------------------

        notificationBadgeHandler.removeCallbacks(
                notificationBadgeRefreshRunnable
        );

        notificationBadgeHandler.post(
                notificationBadgeRefreshRunnable
        );


        /*
         * setupAnnouncements() and setupTinigEpisodes()
         * already perform the initial load during onCreate().
         *
         * Therefore we skip the first dynamic-content refresh
         * to avoid duplicate announcement/TNK API requests.
         *
         * The notification badge still starts immediately above.
         */
        if (firstResume) {

            firstResume = false;

            return;
        }


        Log.d(
                TAG,
                "Explore resumed - refreshing dynamic content"
        );


        // -------------------------------------------------
        // REFRESH LATEST ANNOUNCEMENTS
        // -------------------------------------------------

        loadLatestAnnouncements();


        // -------------------------------------------------
        // REFRESH TNK EPISODES
        // -------------------------------------------------

        loadLatestTinigEpisodes();
    }


    // =====================================================
    // ON PAUSE
    // =====================================================

    @Override
    protected void onPause() {

        super.onPause();

        // Stop badge polling while Explore is not visible.
        notificationBadgeHandler.removeCallbacks(
                notificationBadgeRefreshRunnable
        );
    }


    // =====================================================
    // EXPLORE PAGE ACTIONS
    // =====================================================

    private void setupExploreActions() {


        // -------------------------------------------------
        // LEGAL RIGHTS
        // -------------------------------------------------

        findViewById(
                R.id.cardLegalRights
        ).setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            ExploreActivity.this,
                            LegalRightsActivity.class
                    );


            startActivity(
                    intent
            );
        });


        // -------------------------------------------------
        // EDUCATIONAL ARTICLES
        // -------------------------------------------------

        findViewById(
                R.id.cardEducationalArticles
        ).setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            ExploreActivity.this,
                            EducationalArticlesActivity.class
                    );


            startActivity(
                    intent
            );
        });


        // -------------------------------------------------
        // EMERGENCY HOTLINES
        // -------------------------------------------------

        findViewById(
                R.id.cardEmergencyHotlines
        ).setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            ExploreActivity.this,
                            EmergencyHotlinesActivity.class
                    );


            startActivity(
                    intent
            );
        });
    }


    // =====================================================
    // INFORMATION HUB PRESS ANIMATIONS
    // =====================================================

    private void setupInformationHubPressAnimations() {

        View legalRights =
                findViewById(
                        R.id.cardLegalRights
                );

        View educationalArticles =
                findViewById(
                        R.id.cardEducationalArticles
                );

        View emergencyHotlines =
                findViewById(
                        R.id.cardEmergencyHotlines
                );


        applyPressScaleAnimation(
                legalRights
        );

        applyPressScaleAnimation(
                educationalArticles
        );

        applyPressScaleAnimation(
                emergencyHotlines
        );
    }


    // =====================================================
    // APPLY PRESS SCALE EFFECT
    // =====================================================

    private void applyPressScaleAnimation(
            View view
    ) {

        if (view == null) {

            return;
        }


        view.setOnTouchListener(
                (v, event) -> {

                    switch (
                            event.getActionMasked()
                    ) {


                        // -----------------------------------------
                        // USER STARTS PRESSING
                        // -----------------------------------------

                        case MotionEvent.ACTION_DOWN:

                            v.animate()
                                    .cancel();


                            v.animate()
                                    .scaleX(
                                            HUB_PRESSED_SCALE
                                    )
                                    .scaleY(
                                            HUB_PRESSED_SCALE
                                    )
                                    .setDuration(
                                            HUB_PRESS_DURATION
                                    )
                                    .start();

                            break;


                        // -----------------------------------------
                        // USER RELEASES
                        // -----------------------------------------

                        case MotionEvent.ACTION_UP:

                            v.animate()
                                    .cancel();


                            v.animate()
                                    .scaleX(
                                            HUB_NORMAL_SCALE
                                    )
                                    .scaleY(
                                            HUB_NORMAL_SCALE
                                    )
                                    .setDuration(
                                            HUB_RELEASE_DURATION
                                    )
                                    .start();

                            break;


                        // -----------------------------------------
                        // TOUCH WAS CANCELLED
                        // -----------------------------------------

                        case MotionEvent.ACTION_CANCEL:

                            v.animate()
                                    .cancel();


                            v.animate()
                                    .scaleX(
                                            HUB_NORMAL_SCALE
                                    )
                                    .scaleY(
                                            HUB_NORMAL_SCALE
                                    )
                                    .setDuration(
                                            HUB_RELEASE_DURATION
                                    )
                                    .start();

                            break;


                        // -----------------------------------------
                        // FINGER MOVED OUTSIDE VIEW
                        // -----------------------------------------

                        case MotionEvent.ACTION_MOVE:

                            float x =
                                    event.getX();

                            float y =
                                    event.getY();


                            boolean outsideView =
                                    x < 0
                                            ||
                                            x > v.getWidth()
                                            ||
                                            y < 0
                                            ||
                                            y > v.getHeight();


                            if (outsideView) {

                                v.animate()
                                        .cancel();


                                v.animate()
                                        .scaleX(
                                                HUB_NORMAL_SCALE
                                        )
                                        .scaleY(
                                                HUB_NORMAL_SCALE
                                        )
                                        .setDuration(
                                                HUB_RELEASE_DURATION
                                        )
                                        .start();
                            }

                            break;
                    }


                    /*
                     * IMPORTANT:
                     *
                     * Return false so the normal click listener
                     * still receives the tap.
                     *
                     * Legal Rights,
                     * Educational Articles,
                     * and Emergency Hotlines
                     * therefore continue opening normally.
                     */
                    return false;
                }
        );
    }


    // =====================================================
    // LATEST ANNOUNCEMENTS
    // =====================================================

    private void setupAnnouncements() {

        recyclerAnnouncements =
                findViewById(
                        R.id.recyclerAnnouncements
                );


        LinearLayoutManager layoutManager =
                new LinearLayoutManager(
                        this,
                        LinearLayoutManager.HORIZONTAL,
                        false
                );


        recyclerAnnouncements.setNestedScrollingEnabled(
                false
        );


        recyclerAnnouncements.setLayoutManager(
                layoutManager
        );


        announcementAdapter =
                new AnnouncementAdapter(
                        this,
                        announcements
                );


        recyclerAnnouncements.setAdapter(
                announcementAdapter
        );


        announcementAPI =
                SupabaseClient
                        .getClient()
                        .create(
                                AnnouncementAPI.class
                        );


        // Initial load
        loadLatestAnnouncements();
    }


    // =====================================================
    // LOAD LATEST 10 ANNOUNCEMENTS
    // =====================================================

    private void loadLatestAnnouncements() {

        if (announcementAPI == null) {

            Log.e(
                    TAG,
                    "ANNOUNCEMENTS: API is null"
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
                    "ANNOUNCEMENTS: access token is missing"
            );


            /*
             * Do not crash Explore.
             *
             * The token may still be getting stored during
             * the login transition.
             */
            return;
        }


        Log.d(
                TAG,
                "Loading latest announcements..."
        );


        // -------------------------------------------------
        // CALL SUPABASE
        // -------------------------------------------------

        announcementAPI
                .getLatestAnnouncements(
                        "Bearer "
                                + accessToken
                )
                .enqueue(

                        new Callback<List<Announcement>>() {

                            @Override
                            public void onResponse(
                                    Call<List<Announcement>> call,
                                    Response<List<Announcement>> response
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
                                            "ANNOUNCEMENTS HTTP ERROR: "
                                                    + response.code()
                                    );

                                    return;
                                }


                                // ---------------------------------
                                // BODY
                                // ---------------------------------

                                List<Announcement> result =
                                        response.body();


                                if (
                                        result == null
                                ) {

                                    Log.w(
                                            TAG,
                                            "ANNOUNCEMENTS response body is null"
                                    );

                                    return;
                                }


                                // ---------------------------------
                                // UPDATE RECYCLER VIEW
                                // ---------------------------------

                                announcements.clear();


                                announcements.addAll(
                                        result
                                );


                                if (
                                        announcementAdapter
                                                != null
                                ) {

                                    announcementAdapter
                                            .notifyDataSetChanged();
                                }


                                Log.d(
                                        TAG,
                                        "Announcements loaded: "
                                                + announcements.size()
                                );
                            }


                            @Override
                            public void onFailure(
                                    Call<List<Announcement>> call,
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
                                        "ANNOUNCEMENTS NETWORK FAILURE",
                                        t
                                );


                                /*
                                 * No Toast.
                                 *
                                 * If Supabase/network temporarily
                                 * fails, Explore remains usable and
                                 * existing announcements stay visible.
                                 */
                            }
                        }
                );
    }


    // =====================================================
    // TNK EPISODES
    // =====================================================

    private void setupTinigEpisodes() {

        recyclerTinigEpisodes =
                findViewById(
                        R.id.recyclerTinigEpisodes
                );


        LinearLayoutManager layoutManager =
                new LinearLayoutManager(
                        this,
                        LinearLayoutManager.HORIZONTAL,
                        false
                );


        recyclerTinigEpisodes.setNestedScrollingEnabled(
                false
        );


        recyclerTinigEpisodes.setLayoutManager(
                layoutManager
        );


        tinigEpisodeAdapter =
                new TinigEpisodeAdapter(
                        this,
                        tinigEpisodes
                );


        recyclerTinigEpisodes.setAdapter(
                tinigEpisodeAdapter
        );


        tinigEpisodeAPI =
                SupabaseClient
                        .getClient()
                        .create(
                                TinigEpisodeAPI.class
                        );


        findViewById(
                R.id.txtSeeAllEpisodes
        ).setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            ExploreActivity.this,
                            TinigEpisodesActivity.class
                    );


            startActivity(
                    intent
            );
        });


        // Initial load
        loadLatestTinigEpisodes();
    }


    // =====================================================
    // LOAD LATEST 10 TNK EPISODES
    // =====================================================

    private void loadLatestTinigEpisodes() {

        if (tinigEpisodeAPI == null) {

            Log.e(
                    TAG,
                    "TNK: API is null"
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
                        null
                );


        if (
                accessToken == null
                        ||
                        accessToken
                                .trim()
                                .isEmpty()
        ) {

            Log.e(
                    TAG,
                    "TNK: access token is missing"
            );

            return;
        }


        Log.d(
                TAG,
                "Loading latest TNK episodes..."
        );


        tinigEpisodeAPI
                .getLatestEpisodes(
                        "Bearer "
                                + accessToken
                )
                .enqueue(

                        new Callback<List<TinigEpisode>>() {

                            @Override
                            public void onResponse(
                                    Call<List<TinigEpisode>> call,
                                    Response<List<TinigEpisode>> response
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
                                            "TNK HTTP ERROR: "
                                                    + response.code()
                                    );

                                    return;
                                }


                                // ---------------------------------
                                // BODY
                                // ---------------------------------

                                List<TinigEpisode> result =
                                        response.body();


                                if (
                                        result == null
                                ) {

                                    Log.w(
                                            TAG,
                                            "TNK response body is null"
                                    );

                                    return;
                                }


                                // ---------------------------------
                                // UPDATE RECYCLER VIEW
                                // ---------------------------------

                                tinigEpisodes.clear();


                                tinigEpisodes.addAll(
                                        result
                                );


                                if (
                                        tinigEpisodeAdapter
                                                != null
                                ) {

                                    tinigEpisodeAdapter
                                            .notifyDataSetChanged();
                                }


                                Log.d(
                                        TAG,
                                        "TNK episodes loaded: "
                                                + tinigEpisodes.size()
                                );
                            }


                            @Override
                            public void onFailure(
                                    Call<List<TinigEpisode>> call,
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
                                        "TNK NETWORK FAILURE",
                                        t
                                );


                                /*
                                 * Keep Explore usable if there is
                                 * a temporary connection failure.
                                 */
                            }
                        }
                );
    }


    // =====================================================
    // LOAD UNREAD REPORT NOTIFICATION COUNT
    // =====================================================

    private void loadUnreadNotificationCount() {

        if (txtExploreNotificationBadge == null
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


        // Fallback to the stored public.users.id.
        // Never use auth_id for report_notifications.user_id.
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

            txtExploreNotificationBadge.setVisibility(
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

                                if (isFinishing()
                                        || isDestroyed()) {

                                    return;
                                }


                                if (!response.isSuccessful()) {

                                    Log.e(
                                            TAG,
                                            "UNREAD NOTIFICATION COUNT HTTP ERROR: "
                                                    + response.code()
                                    );

                                    // Preserve the last visible badge on
                                    // temporary HTTP/session failures.
                                    return;
                                }


                                List<ReportNotification> result =
                                        response.body();


                                int unreadCount =
                                        result == null
                                                ? 0
                                                : result.size();


                                updateNotificationBadge(
                                        unreadCount
                                );
                            }


                            @Override
                            public void onFailure(
                                    Call<List<ReportNotification>> call,
                                    Throwable t
                            ) {

                                if (isFinishing()
                                        || isDestroyed()) {

                                    return;
                                }


                                Log.e(
                                        TAG,
                                        "UNREAD NOTIFICATION COUNT NETWORK FAILURE",
                                        t
                                );

                                // Preserve the last visible badge on
                                // temporary network failures.
                            }
                        }
                );
    }


    // =====================================================
    // UPDATE REPORT NOTIFICATION BADGE
    // =====================================================

    private void updateNotificationBadge(
            int unreadCount
    ) {

        if (txtExploreNotificationBadge == null) {

            return;
        }


        if (unreadCount <= 0) {

            txtExploreNotificationBadge.setVisibility(
                    View.GONE
            );

            return;
        }


        txtExploreNotificationBadge.setText(
                unreadCount > 99
                        ? "99+"
                        : String.valueOf(unreadCount)
        );

        txtExploreNotificationBadge.setVisibility(
                View.VISIBLE
        );
    }


    // =====================================================
    // BOTTOM NAVIGATION
    // =====================================================

    private void setupNavigation() {


        // -------------------------------------------------
        // HOME
        // -------------------------------------------------

        findViewById(
                R.id.navHome
        ).setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            ExploreActivity.this,
                            HomeActivity.class
                    );


            startActivity(
                    intent
            );


            finish();
        });


        // -------------------------------------------------
        // SUPPORT
        // -------------------------------------------------

        findViewById(
                R.id.navSupport
        ).setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            ExploreActivity.this,
                            SupportActivity.class
                    );


            startActivity(
                    intent
            );


            finish();
        });


        // -------------------------------------------------
        // EXPLORE
        // -------------------------------------------------

        findViewById(
                R.id.navExplore
        ).setOnClickListener(v -> {

            /*
             * Already on ExploreActivity.
             *
             * Refresh the dynamic content when the
             * currently selected Explore button is tapped.
             */

            loadLatestAnnouncements();

            loadLatestTinigEpisodes();
        });


        // -------------------------------------------------
        // PROFILE
        // -------------------------------------------------

        findViewById(
                R.id.navProfile
        ).setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            ExploreActivity.this,
                            ProfileActivity.class
                    );


            startActivity(
                    intent
            );


            finish();
        });
    }
}
