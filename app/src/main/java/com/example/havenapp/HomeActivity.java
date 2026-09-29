package com.example.havenapp;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.havenapp.api.RefreshTokenRequest;
import com.example.havenapp.api.ReportNotificationAPI;
import com.example.havenapp.api.SupabaseAuthAPI;
import com.example.havenapp.api.SupabaseCallLogAPI;
import com.example.havenapp.api.SupabaseClient;
import com.example.havenapp.api.SupabaseConfig;
import com.example.havenapp.api.SupabaseLocationAPI;
import com.example.havenapp.api.SupabaseUserAPI;
import com.example.havenapp.model.AuthResponse;
import com.example.havenapp.model.ReportNotification;
import com.example.havenapp.model.Profile;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "SUPABASE_LOCATION";

    // =========================================================
    // PERMISSIONS
    // =========================================================

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int CALL_PERMISSION_REQUEST_CODE = 1002;
    private static final int PHONE_STATE_PERMISSION_REQUEST_CODE = 1003;

    // =========================================================
    // EMERGENCY NUMBERS
    // =========================================================

    private static final String EMERGENCY_911_NUMBER = "911";
    private static final String MSWDO_NUMBER = "09560439129";

    // =========================================================
    // PANIC BUTTON
    // =========================================================

    private static final long PANIC_HOLD_DURATION = 3000;

    private ImageButton panicButton;

    private boolean panicTriggered = false;

    private ObjectAnimator panicRotationAnimator;

    private final Handler panicHandler = new Handler();

    private boolean pendingPanicCall = false;

    // =========================================================
    // EMERGENCY CALL STATE
    // =========================================================

    private boolean emergencyCallStarted = false;

    private String activeCallLogId = null;

    private String pendingMSWDOCalledUserId = null;

    private TelephonyManager telephonyManager;


    private PhoneStateListener phoneStateListener;

    private boolean emergencyCallWasConnected = false;
    private boolean emergencyStillActive = false;

    // =========================================================
    // PANIC CALL RUNNABLE
    // =========================================================

    private final Runnable panicCallRunnable = new Runnable() {

        @Override
        public void run() {

            if (panicButton == null) {
                return;
            }

            panicTriggered = true;

            stopVibration();
            stopPanicRotation();

            if (!isLocationShared) {

                pendingPanicCall = true;

                showPanicLocationShareDialog();

            } else {

                pendingPanicCall = false;

                startEmergencyLiveLocationTracking();

                callMSWDO();
            }
        }
    };

    // =========================================================
    // HEADER
    // =========================================================

    private ImageView profileImage;
    private ImageView notificationButton;
    private TextView txtNotificationBadge;
    private TextView welcomeText;

    // =========================================================
    // LOCATION
    // =========================================================

    private View locationCard;
    private TextView locationCardText;

    private FusedLocationProviderClient fusedLocationClient;

    private boolean isLocationShared = false;

    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    private boolean waitingForLocationSettings = false;

    private SupabaseLocationAPI locationAPI;
    private ReportNotificationAPI notificationAPI;

    private final Handler notificationBadgeHandler =
            new Handler(Looper.getMainLooper());

    private static final long NOTIFICATION_BADGE_REFRESH_INTERVAL = 5000L;

    // =========================================================
// NOTIFICATION BADGE AUTO REFRESH
// =========================================================

    private final Runnable notificationBadgeRefreshRunnable =
            new Runnable() {

                @Override
                public void run() {

                    // Get the latest unread notification count
                    loadUnreadNotificationCount();

                    // Schedule the next check after 5 seconds
                    notificationBadgeHandler.postDelayed(
                            this,
                            NOTIFICATION_BADGE_REFRESH_INTERVAL
                    );
                }
            };

    // =========================================================
    // EMERGENCY LIVE LOCATION TRACKING
    // =========================================================

    private LocationCallback emergencyLocationCallback;

    private LocationRequest emergencyLocationRequest;

    private boolean emergencyLocationTracking = false;

    private static final long EMERGENCY_LOCATION_INTERVAL = 3000;

    // =========================================================
    // USER / AUTH / CALL LOG APIs
    // =========================================================

    private SupabaseUserAPI userAPI;

    private SupabaseAuthAPI authAPI;

    private SupabaseCallLogAPI callLogAPI;

    // =========================================================
    // CALL CARDS
    // =========================================================

    private View card911;
    private View cardMSWDO;

    // =========================================================
    // VIBRATOR
    // =========================================================

    private Vibrator vibrator;

    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.home_activity);

        initializeViews();

        initializeLocation();

        initializeSupabase();

        loadUsername();

        initializePhoneCallStateDetector();

        loadProfileImage();

        loadLocationState();

        setupNotification();

        setupLocationSharing();

        setupPanicButton();

        setupCallButtons();

        setupHopeAssistant();

        setupNavigation();
    }

    @Override
    protected void onResume() {

        super.onResume();

        // =====================================================
        // REFRESH HOME UI
        // =====================================================

        loadUsername();

        loadProfileImage();

        loadLocationState();

        // Start notification badge auto-refresh
        notificationBadgeHandler.removeCallbacks(
                notificationBadgeRefreshRunnable
        );

        notificationBadgeHandler.post(
                notificationBadgeRefreshRunnable
        );


        // =====================================================
        // RETURNING FROM PHONE LOCATION SETTINGS
        // =====================================================

        if (waitingForLocationSettings) {

            waitingForLocationSettings = false;

            LocationManager locationManager =
                    (LocationManager)
                            getSystemService(
                                    Context.LOCATION_SERVICE
                            );

            boolean locationEnabled = false;

            try {

                if (android.os.Build.VERSION.SDK_INT >= 28) {

                    locationEnabled =
                            locationManager.isLocationEnabled();

                } else {

                    locationEnabled =
                            locationManager.isProviderEnabled(
                                    LocationManager.GPS_PROVIDER
                            )
                                    ||
                                    locationManager.isProviderEnabled(
                                            LocationManager.NETWORK_PROVIDER
                                    );
                }

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "ERROR CHECKING LOCATION STATUS",
                        e
                );
            }

            if (locationEnabled) {

                Log.d(
                        TAG,
                        "LOCATION ENABLED AFTER RETURNING FROM SETTINGS"
                );

                detectCurrentLocation();

            } else {

                Log.d(
                        TAG,
                        "LOCATION STILL DISABLED"
                );
            }
        }

        /*
         * Do NOT stop emergency tracking here.
         */
    }

    @Override
    protected void onPause() {

        super.onPause();

        // Stop notification badge auto-refresh
        notificationBadgeHandler.removeCallbacks(
                notificationBadgeRefreshRunnable
        );
    }

    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initializeViews() {

        profileImage = findViewById(R.id.profileImage);

        notificationButton =
                findViewById(R.id.notificationButton);

        txtNotificationBadge =
                findViewById(R.id.txtNotificationBadge);

        welcomeText =
                findViewById(R.id.welcomeText);

        panicButton =
                findViewById(R.id.panicButton);

        locationCard =
                findViewById(R.id.locationCard);

        locationCardText =
                findLocationCardText();

        card911 =
                findViewById(R.id.card911);

        cardMSWDO =
                findViewById(R.id.cardMSWDO);

        vibrator =
                (Vibrator) getSystemService(
                        Context.VIBRATOR_SERVICE
                );
    }

    // =========================================================
    // INITIALIZE LOCATION
    // =========================================================

    private void initializeLocation() {

        fusedLocationClient =
                LocationServices
                        .getFusedLocationProviderClient(this);
    }

    // =========================================================
    // INITIALIZE SUPABASE
    // =========================================================

    private void initializeSupabase() {

        locationAPI =
                SupabaseClient
                        .getClient()
                        .create(SupabaseLocationAPI.class);

        userAPI =
                SupabaseClient
                        .getClient()
                        .create(SupabaseUserAPI.class);

        authAPI =
                SupabaseClient
                        .getClient()
                        .create(SupabaseAuthAPI.class);

        callLogAPI =
                SupabaseClient
                        .getClient()
                        .create(SupabaseCallLogAPI.class);

        notificationAPI =
                SupabaseClient
                        .getClient()
                        .create(ReportNotificationAPI.class);
    }

    // =========================================================
    // INITIALIZE PHONE CALL STATE DETECTOR
    // =========================================================

    private void initializePhoneCallStateDetector() {

        telephonyManager =
                (TelephonyManager) getSystemService(
                        Context.TELEPHONY_SERVICE
                );

        if (telephonyManager == null) {

            Log.e(
                    TAG,
                    "TELEPHONY MANAGER IS NULL"
            );

            return;
        }

        phoneStateListener =
                new PhoneStateListener() {

                    @Override
                    public void onCallStateChanged(
                            int state,
                            String phoneNumber
                    ) {

                        super.onCallStateChanged(
                                state,
                                phoneNumber
                        );

                        if (!emergencyCallStarted) {
                            return;
                        }

                        if (state ==
                                TelephonyManager.CALL_STATE_OFFHOOK) {

                            Log.d(
                                    TAG,
                                    "================================"
                            );

                            Log.d(
                                    TAG,
                                    "EMERGENCY CALL CONNECTED"
                            );

                            Log.d(
                                    TAG,
                                    "================================"
                            );

                            emergencyCallWasConnected = true;

                            updateCallLogAsActive();

                        } else if (state ==
                                TelephonyManager.CALL_STATE_IDLE) {

                            /*
                             * If an emergency call was started, IDLE means
                             * the call attempt has finished.
                             *
                             * This also handles unanswered / cancelled 911 calls.
                             */
                            if (emergencyCallStarted) {

                                Log.d(
                                        TAG,
                                        "================================"
                                );

                                if (emergencyCallWasConnected) {

                                    Log.d(
                                            TAG,
                                            "EMERGENCY CALL ENDED"
                                    );

                                } else {

                                    Log.d(
                                            TAG,
                                            "EMERGENCY CALL ATTEMPT ENDED WITHOUT CONNECTION"
                                    );
                                }

                                Log.d(
                                        TAG,
                                        "================================"
                                );

                                updateCallLogAsEnded();

                                emergencyCallWasConnected = false;
                                emergencyCallStarted = false;
                            }
                        }
                    }
                };

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.READ_PHONE_STATE
                    },
                    PHONE_STATE_PERMISSION_REQUEST_CODE
            );

            return;
        }

        telephonyManager.listen(
                phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE
        );

        Log.d(
                TAG,
                "PHONE CALL STATE DETECTOR INITIALIZED"
        );
    }

    // =========================================================
    // FIND LOCATION CARD TEXT
    // =========================================================

    private TextView findLocationCardText() {

        if (!(locationCard instanceof android.view.ViewGroup)) {
            return null;
        }

        android.view.ViewGroup card =
                (android.view.ViewGroup) locationCard;

        return findTextViewInside(card);
    }

    private TextView findTextViewInside(
            android.view.ViewGroup parent
    ) {

        for (int i = 0; i < parent.getChildCount(); i++) {

            View child =
                    parent.getChildAt(i);

            if (child instanceof TextView) {

                TextView textView =
                        (TextView) child;

                String text =
                        textView.getText().toString();

                if (text.contains("Share Current Location")
                        || text.contains("Location Shared")) {

                    return textView;
                }
            }

            if (child instanceof android.view.ViewGroup) {

                TextView result =
                        findTextViewInside(
                                (android.view.ViewGroup) child
                        );

                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    // =========================================================
    // LOAD LOCATION STATE
    // =========================================================

    private void loadLocationState() {

        SharedPreferences preferences =
                getSharedPreferences(
                        "HavenLocation",
                        MODE_PRIVATE
                );

        isLocationShared =
                preferences.getBoolean(
                        "location_shared",
                        false
                );

        currentLatitude =
                Double.longBitsToDouble(
                        preferences.getLong(
                                "latitude",
                                Double.doubleToLongBits(0.0)
                        )
                );

        currentLongitude =
                Double.longBitsToDouble(
                        preferences.getLong(
                                "longitude",
                                Double.doubleToLongBits(0.0)
                        )
                );

        updateLocationCard();
    }

    // =========================================================
    // UPDATE LOCATION CARD
    // =========================================================

    private void updateLocationCard() {

        if (locationCardText == null) {
            return;
        }

        if (isLocationShared) {

            locationCardText.setText(
                    "Location Shared"
            );

        } else {

            locationCardText.setText(
                    "Share Current Location"
            );
        }
    }

    // =========================================================
    // SAVE LOCATION STATE
    // =========================================================

    private void saveLocationState() {

        SharedPreferences preferences =
                getSharedPreferences(
                        "HavenLocation",
                        MODE_PRIVATE
                );

        preferences.edit()
                .putBoolean(
                        "location_shared",
                        isLocationShared
                )
                .putLong(
                        "latitude",
                        Double.doubleToLongBits(
                                currentLatitude
                        )
                )
                .putLong(
                        "longitude",
                        Double.doubleToLongBits(
                                currentLongitude
                        )
                )
                .apply();
    }

    // =========================================================
    // LOAD USERNAME
    // =========================================================

    private void loadUsername() {

        SharedPreferences preferences =
                getSharedPreferences(
                        "HavenUser",
                        MODE_PRIVATE
                );

        String username =
                preferences.getString(
                        "username",
                        "User"
                );

        if (welcomeText != null) {

            welcomeText.setText(
                    "Welcome " + username + "!"
            );
        }
    }

    // =========================================================
    // LOAD PROFILE IMAGE
    // =========================================================

    private void loadProfileImage() {

        if (profileImage == null) {
            return;
        }

        SharedPreferences preferences =
                getSharedPreferences(
                        "HavenUser",
                        MODE_PRIVATE
                );

        String imageUrl =
                preferences.getString(
                        "profile_image",
                        ""
                );

        if (!imageUrl.isEmpty()) {

            Glide.with(this)
                    .load(imageUrl)
                    .circleCrop()
                    .into(profileImage);

        } else {

            Glide.with(this)
                    .load(R.drawable.dp)
                    .circleCrop()
                    .into(profileImage);
        }
    }

    // =========================================================
// NOTIFICATION
// =========================================================

    private void setupNotification() {

        if (notificationButton == null) {
            return;
        }

        notificationButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            HomeActivity.this,
                            NotificationsActivity.class
                    );

            startActivity(intent);
        });
    }

    // =========================================================
// LOAD UNREAD NOTIFICATION COUNT
// =========================================================

    private void loadUnreadNotificationCount() {

        // -----------------------------------------------------
        // SAFETY CHECK
        // -----------------------------------------------------

        if (txtNotificationBadge == null
                || notificationAPI == null) {

            return;
        }


        // Hide badge while checking the latest count.

        txtNotificationBadge.setVisibility(
                View.GONE
        );


        // -----------------------------------------------------
        // GET CURRENT HAVENAPP SESSION
        // -----------------------------------------------------

        SharedPreferences preferences =
                getSharedPreferences(
                        "HavenUser",
                        MODE_PRIVATE
                );


        /*
         * IMPORTANT:
         *
         * report_notifications.user_id stores public.users.id,
         * NOT Supabase auth.uid().
         */

        String notificationUserId =
                preferences.getString(
                        "user_id",
                        ""
                );


        if (notificationUserId == null
                || notificationUserId.trim().isEmpty()) {

            notificationUserId =
                    preferences.getString(
                            "id",
                            ""
                    );
        }


        String currentAccessToken =
                preferences.getString(
                        "access_token",
                        ""
                );


        if (notificationUserId == null) {
            notificationUserId = "";
        }

        if (currentAccessToken == null) {
            currentAccessToken = "";
        }


        notificationUserId =
                notificationUserId.trim();

        currentAccessToken =
                currentAccessToken.trim();


        // -----------------------------------------------------
        // INVALID / MISSING SESSION
        // -----------------------------------------------------

        if (notificationUserId.isEmpty()
                || currentAccessToken.isEmpty()) {

            txtNotificationBadge.setVisibility(
                    View.GONE
            );

            return;
        }


        // -----------------------------------------------------
        // QUERY ONLY UNREAD NOTIFICATIONS
        // -----------------------------------------------------

        notificationAPI
                .getUnreadNotifications(
                        "Bearer " + currentAccessToken,
                        "eq." + notificationUserId,
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

                                    /*
                                     * For now simply hide the badge
                                     * if Supabase rejects the request.
                                     *
                                     * We will add the 401 refresh/retry
                                     * handling in the next step.
                                     */

                                    txtNotificationBadge.setVisibility(
                                            View.GONE
                                    );


                                    Log.e(
                                            TAG,
                                            "UNREAD NOTIFICATION COUNT HTTP ERROR: "
                                                    + response.code()
                                    );

                                    return;
                                }


                                List<ReportNotification> unreadNotifications =
                                        response.body();


                                int unreadCount =
                                        unreadNotifications == null
                                                ? 0
                                                : unreadNotifications.size();


                                updateNotificationBadge(
                                        unreadCount
                                );
                            }


                            @Override
                            public void onFailure(
                                    Call<List<ReportNotification>> call,
                                    Throwable t
                            ) {

                                txtNotificationBadge.setVisibility(
                                        View.GONE
                                );


                                Log.e(
                                        TAG,
                                        "UNREAD NOTIFICATION COUNT NETWORK FAILURE",
                                        t
                                );
                            }
                        }
                );
    }

    // =========================================================
// UPDATE NOTIFICATION BADGE
// =========================================================

    private void updateNotificationBadge(
            int unreadCount
    ) {

        if (txtNotificationBadge == null) {
            return;
        }


        // -----------------------------------------------------
        // NO UNREAD NOTIFICATIONS
        // -----------------------------------------------------

        if (unreadCount <= 0) {

            txtNotificationBadge.setVisibility(
                    View.GONE
            );

            return;
        }


        // -----------------------------------------------------
        // DISPLAY COUNT
        // -----------------------------------------------------

        if (unreadCount > 99) {

            txtNotificationBadge.setText(
                    "99+"
            );

        } else {

            txtNotificationBadge.setText(
                    String.valueOf(unreadCount)
            );
        }


        txtNotificationBadge.setVisibility(
                View.VISIBLE
        );
    }
    // =========================================================
    // LOCATION SHARING
    // =========================================================

    private void setupLocationSharing() {

        if (locationCard == null) {
            return;
        }

        locationCard.setClickable(true);
        locationCard.setFocusable(true);

        locationCard.setOnClickListener(v -> {

            if (isLocationShared) {

                showLocationSharedDialog();

            } else {

                showLocationShareDialog();
            }
        });
    }

    // =========================================================
    // NORMAL LOCATION SHARE DIALOG
    // =========================================================

    private void showLocationShareDialog() {

        View dialogView =
                LayoutInflater.from(this)
                        .inflate(
                                R.layout.location_share_dialog,
                                null
                        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .create();

        MaterialButton shareLocationButton =
                dialogView.findViewById(
                        R.id.shareLocationButton
                );

        TextView maybeLaterButton =
                dialogView.findViewById(
                        R.id.maybeLaterButton
                );

        shareLocationButton.setOnClickListener(v -> {

            dialog.dismiss();

            requestLocationPermission();
        });

        maybeLaterButton.setOnClickListener(v -> {

            dialog.dismiss();
        });

        dialog.show();

        setDialogSize(dialog);
    }

    // =========================================================
    // PANIC LOCATION SHARE DIALOG
    // =========================================================

    private void showPanicLocationShareDialog() {

        View dialogView =
                LayoutInflater.from(this)
                        .inflate(
                                R.layout.location_share_dialog,
                                null
                        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .create();

        ImageView locationDialogImage =
                dialogView.findViewById(
                        R.id.locationDialogImage
                );

        TextView locationDialogTitle =
                dialogView.findViewById(
                        R.id.locationDialogTitle
                );

        TextView locationDialogDescription =
                dialogView.findViewById(
                        R.id.locationDialogDescription
                );

        MaterialButton shareLocationButton =
                dialogView.findViewById(
                        R.id.shareLocationButton
                );

        TextView maybeLaterButton =
                dialogView.findViewById(
                        R.id.maybeLaterButton
                );

        locationDialogTitle.setText(
                "Share Your Location"
        );

        locationDialogDescription.setText(
                "Your location must be shared so the " +
                        "Barangay Admin can see your live location " +
                        "during this emergency call."
        );

        locationDialogImage.setImageResource(
                R.drawable.location_button
        );

        shareLocationButton.setText(
                "SHARE LOCATION"
        );

        maybeLaterButton.setText(
                "CANCEL"
        );

        shareLocationButton.setOnClickListener(v -> {

            dialog.dismiss();

            requestLocationPermission();
        });

        maybeLaterButton.setOnClickListener(v -> {

            pendingPanicCall = false;

            dialog.dismiss();

            Toast.makeText(
                    HomeActivity.this,
                    "Emergency call cancelled because location sharing was not enabled.",
                    Toast.LENGTH_LONG
            ).show();
        });

        dialog.show();

        setDialogSize(dialog);
    }

    // =========================================================
    // LOCATION SHARED DIALOG
    // =========================================================

    private void showLocationSharedDialog() {

        View dialogView =
                LayoutInflater.from(this)
                        .inflate(
                                R.layout.location_share_dialog,
                                null
                        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .create();

        ImageView locationDialogImage =
                dialogView.findViewById(
                        R.id.locationDialogImage
                );

        TextView locationDialogTitle =
                dialogView.findViewById(
                        R.id.locationDialogTitle
                );

        TextView locationDialogDescription =
                dialogView.findViewById(
                        R.id.locationDialogDescription
                );

        MaterialButton keepSharingButton =
                dialogView.findViewById(
                        R.id.shareLocationButton
                );

        TextView unshareLocationButton =
                dialogView.findViewById(
                        R.id.maybeLaterButton
                );

        locationDialogTitle.setText(
                "Location Sharing Active"
        );

        locationDialogDescription.setText(
                "Your current location is currently shared " +
                        "with HavenApp. You can stop sharing " +
                        "your location at any time."
        );

        locationDialogImage.setImageResource(
                R.drawable.location_button
        );

        keepSharingButton.setText(
                "KEEP SHARING"
        );

        unshareLocationButton.setText(
                "UNSHARE LOCATION"
        );

        keepSharingButton.setOnClickListener(v -> {

            dialog.dismiss();

            Toast.makeText(
                    HomeActivity.this,
                    "Your location is still being shared.",
                    Toast.LENGTH_SHORT
            ).show();
        });

        unshareLocationButton.setOnClickListener(v -> {

            dialog.dismiss();

            unshareLocation();
        });

        dialog.show();

        setDialogSize(dialog);
    }

    // =========================================================
    // DIALOG SIZE
    // =========================================================

    private void setDialogSize(AlertDialog dialog) {

        Window window =
                dialog.getWindow();

        if (window != null) {

            window.setBackgroundDrawableResource(
                    android.R.color.transparent
            );

            WindowManager.LayoutParams params =
                    window.getAttributes();

            params.width =
                    (int) (
                            getResources()
                                    .getDisplayMetrics()
                                    .widthPixels * 0.86
                    );

            params.height =
                    WindowManager.LayoutParams.WRAP_CONTENT;

            params.dimAmount = 0.35f;

            window.setAttributes(params);

            window.addFlags(
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND
            );
        }
    }

    // =========================================================
    // LOCATION PERMISSION
    // =========================================================

    private void requestLocationPermission() {

        boolean fineGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        if (fineGranted || coarseGranted) {

            checkLocationEnabled();

            return;
        }

        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE
        );
    }

    // =========================================================
    // PERMISSION RESULT
    // =========================================================

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        // =====================================================
        // LOCATION
        // =====================================================

        if (requestCode ==
                LOCATION_PERMISSION_REQUEST_CODE) {

            boolean locationGranted = false;

            for (int result : grantResults) {

                if (result ==
                        PackageManager.PERMISSION_GRANTED) {

                    locationGranted = true;

                    break;
                }
            }

            if (locationGranted) {

                checkLocationEnabled();

            } else {

                pendingPanicCall = false;

                Toast.makeText(
                        this,
                        "Location access was denied. " +
                                "Location sharing cannot be activated.",
                        Toast.LENGTH_LONG
                ).show();
            }

            return;
        }

        // =====================================================
// LOCATION
// =====================================================

        if (requestCode ==
                LOCATION_PERMISSION_REQUEST_CODE) {

            // your existing location code


            return;
        }


// =====================================================
// CALL
// =====================================================

// 👈 REPLACE THIS WHOLE SECTION
// with the new CALL section


// =====================================================
// PHONE STATE
// =====================================================

        if (requestCode ==
                PHONE_STATE_PERMISSION_REQUEST_CODE) {

            // your existing phone-state code


        }

        // =====================================================
        // PHONE STATE
        // =====================================================

        if (requestCode ==
                PHONE_STATE_PERMISSION_REQUEST_CODE) {

            if (grantResults.length > 0
                    && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED) {

                Log.d(
                        TAG,
                        "READ_PHONE_STATE PERMISSION GRANTED"
                );

                if (telephonyManager != null
                        && phoneStateListener != null) {

                    telephonyManager.listen(
                            phoneStateListener,
                            PhoneStateListener.LISTEN_CALL_STATE
                    );

                    Log.d(
                            TAG,
                            "PHONE CALL STATE DETECTOR INITIALIZED AFTER PERMISSION"
                    );
                }

            } else {

                Log.w(
                        TAG,
                        "READ_PHONE_STATE PERMISSION DENIED"
                );

                Toast.makeText(
                        this,
                        "Call status tracking permission was denied. The emergency call can still be made, but HavenApp may not detect when the call connects or ends.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    // =========================================================
    // CHECK LOCATION ENABLED
    // =========================================================

    private void checkLocationEnabled() {

        LocationManager locationManager =
                (LocationManager)
                        getSystemService(
                                Context.LOCATION_SERVICE
                        );

        boolean locationEnabled = false;

        try {

            if (android.os.Build.VERSION.SDK_INT >= 28) {

                locationEnabled =
                        locationManager.isLocationEnabled();

            } else {

                locationEnabled =
                        locationManager.isProviderEnabled(
                                LocationManager.GPS_PROVIDER
                        )
                                ||
                                locationManager.isProviderEnabled(
                                        LocationManager.NETWORK_PROVIDER
                                );
            }

        } catch (Exception ignored) {
        }

        if (!locationEnabled) {

            new AlertDialog.Builder(this)

                    .setTitle(
                            "Location is Turned Off"
                    )

                    .setMessage(
                            "Please turn on your phone's " +
                                    "location service so HavenApp " +
                                    "can detect your current location."
                    )

                    .setPositiveButton(
                            "TURN ON LOCATION",
                            (dialog, which) -> {

                                try {

                                    waitingForLocationSettings = true;

                                    Intent intent =
                                            new Intent(
                                                    Settings.ACTION_LOCATION_SOURCE_SETTINGS
                                            );

                                    startActivity(intent);

                                } catch (Exception e) {

                                    Toast.makeText(
                                            this,
                                            "Please enable Location in your phone settings.",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }
                    )

                    .setNegativeButton(
                            "CANCEL",
                            null
                    )

                    .show();

            return;
        }

        detectCurrentLocation();
    }

    // =========================================================
    // DETECT CURRENT LOCATION
    // =========================================================

    private void detectCurrentLocation() {

        boolean fineGranted =
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseGranted =
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        if (!fineGranted && !coarseGranted) {
            return;
        }

        Toast.makeText(
                this,
                "Detecting your current location...",
                Toast.LENGTH_SHORT
        ).show();

        CancellationTokenSource cancellationTokenSource =
                new CancellationTokenSource();

        fusedLocationClient
                .getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.getToken()
                )
                .addOnSuccessListener(location -> {

                    if (location != null) {

                        currentLatitude =
                                location.getLatitude();

                        currentLongitude =
                                location.getLongitude();

                        Log.d(
                                TAG,
                                "LOCATION DETECTED"
                        );

                        Log.d(
                                TAG,
                                "Latitude: " +
                                        currentLatitude
                        );

                        Log.d(
                                TAG,
                                "Longitude: " +
                                        currentLongitude
                        );

                        uploadLocationToSupabase();

                    } else {

                        Toast.makeText(
                                HomeActivity.this,
                                "Unable to detect your location. " +
                                        "Please make sure your GPS/location is turned on.",
                                Toast.LENGTH_LONG
                        ).show();
                    }

                })
                .addOnFailureListener(e -> {

                    Log.e(
                            TAG,
                            "LOCATION DETECTION FAILED",
                            e
                    );

                    Toast.makeText(
                            HomeActivity.this,
                            "Unable to detect your current location.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =========================================================
    // GET LOGGED IN USER ID
    // =========================================================

    private String getLoggedInUserId() {

        SharedPreferences preferences =
                getSharedPreferences(
                        "HavenUser",
                        MODE_PRIVATE
                );

        String userId =
                preferences.getString(
                        "auth_id",
                        ""
                );

        if (userId == null || userId.trim().isEmpty()) {

            userId =
                    preferences.getString(
                            "user_id",
                            ""
                    );
        }

        if (userId == null || userId.trim().isEmpty()) {

            userId =
                    preferences.getString(
                            "id",
                            ""
                    );
        }

        return userId == null
                ? ""
                : userId.trim();
    }

    // =========================================================
    // UPLOAD LOCATION
    // =========================================================

    private void uploadLocationToSupabase() {

        String userId =
                getLoggedInUserId();

        if (userId.isEmpty()) {

            Log.e(
                    TAG,
                    "USER ID IS EMPTY"
            );

            Toast.makeText(
                    this,
                    "Location detected, but your user ID could not be found.",
                    Toast.LENGTH_LONG
            ).show();

            pendingPanicCall = false;

            return;
        }

        Map<String, Object> locationData =
                new HashMap<>();

        locationData.put(
                "latitude",
                currentLatitude
        );

        locationData.put(
                "longitude",
                currentLongitude
        );

        locationData.put(
                "is_shared",
                true
        );

        locationAPI
                .updateLocation(
                        "eq." + userId,
                        locationData
                )
                .enqueue(
                        new Callback<List<Map<String, Object>>>() {

                            @Override
                            public void onResponse(
                                    Call<List<Map<String, Object>>> call,
                                    Response<List<Map<String, Object>>> response
                            ) {

                                Log.d(
                                        TAG,
                                        "UPDATE HTTP CODE: " +
                                                response.code()
                                );

                                if (!response.isSuccessful()) {

                                    handleLocationError(
                                            response,
                                            "LOCATION UPDATE FAILED"
                                    );

                                    return;
                                }

                                List<Map<String, Object>> updatedRows =
                                        response.body();

                                if (updatedRows == null
                                        || updatedRows.isEmpty()) {

                                    createInitialLocation(
                                            userId
                                    );

                                    return;
                                }

                                locationShareSuccessful();
                            }

                            @Override
                            public void onFailure(
                                    Call<List<Map<String, Object>>> call,
                                    Throwable t
                            ) {

                                Log.e(
                                        TAG,
                                        "NETWORK FAILURE WHILE UPDATING LOCATION",
                                        t
                                );

                                isLocationShared = false;

                                saveLocationState();

                                updateLocationCard();

                                pendingPanicCall = false;

                                Toast.makeText(
                                        HomeActivity.this,
                                        "Location detected, but connection to the server failed.",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }

    // =========================================================
    // LOCATION SHARE SUCCESS
    // =========================================================

    private void locationShareSuccessful() {

        isLocationShared = true;

        saveLocationState();

        updateLocationCard();

        if (pendingPanicCall) {

            pendingPanicCall = false;

            Toast.makeText(
                    HomeActivity.this,
                    "Location shared. Starting emergency live tracking...",
                    Toast.LENGTH_LONG
            ).show();

            startEmergencyLiveLocationTracking();

            callMSWDO();

        } else {

            Toast.makeText(
                    HomeActivity.this,
                    "Your location has been shared with HavenApp.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // CREATE LOCATION
    // =========================================================

    private void createInitialLocation(
            String userId
    ) {

        Map<String, Object> locationData =
                new HashMap<>();

        locationData.put(
                "user_id",
                userId
        );

        locationData.put(
                "latitude",
                currentLatitude
        );

        locationData.put(
                "longitude",
                currentLongitude
        );

        locationData.put(
                "is_shared",
                true
        );

        locationAPI
                .createLocation(locationData)
                .enqueue(
                        new Callback<List<Map<String, Object>>>() {

                            @Override
                            public void onResponse(
                                    Call<List<Map<String, Object>>> call,
                                    Response<List<Map<String, Object>>> response
                            ) {

                                if (response.isSuccessful()) {

                                    locationShareSuccessful();

                                } else {

                                    handleLocationError(
                                            response,
                                            "LOCATION CREATE FAILED"
                                    );
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<List<Map<String, Object>>> call,
                                    Throwable t
                            ) {

                                Log.e(
                                        TAG,
                                        "NETWORK FAILURE WHILE CREATING LOCATION",
                                        t
                                );

                                isLocationShared = false;

                                saveLocationState();

                                updateLocationCard();

                                pendingPanicCall = false;

                                Toast.makeText(
                                        HomeActivity.this,
                                        "Location detected, but connection to the server failed.",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }

    // =========================================================
    // LOCATION ERROR
    // =========================================================

    private void handleLocationError(
            Response<?> response,
            String logMessage
    ) {

        String errorBody = "";

        try {

            if (response.errorBody() != null) {

                errorBody =
                        response.errorBody().string();
            }

        } catch (Exception e) {

            errorBody = e.getMessage();
        }

        Log.e(
                TAG,
                logMessage
        );

        Log.e(
                TAG,
                "HTTP CODE: " +
                        response.code()
        );

        Log.e(
                TAG,
                "ERROR BODY: " +
                        errorBody
        );

        isLocationShared = false;

        saveLocationState();

        updateLocationCard();

        pendingPanicCall = false;

        Toast.makeText(
                HomeActivity.this,
                "Location update failed: " +
                        response.code(),
                Toast.LENGTH_LONG
        ).show();
    }

    // =========================================================
// UNSHARE LOCATION
// =========================================================

    private void unshareLocation() {

        String userId =
                getLoggedInUserId();

        /*
         * User stopped sharing.
         *
         * IMPORTANT:
         * Keep the last latitude and longitude.
         * Do NOT change them to 0.0.
         */
        isLocationShared = false;

        saveLocationState();
        updateLocationCard();

        if (userId.isEmpty()) {

            Toast.makeText(
                    this,
                    "Location sharing has been stopped.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Map<String, Object> locationData =
                new HashMap<>();

        /*
         * Only change the sharing status.
         *
         * The existing latitude and longitude
         * in Supabase will remain untouched.
         */
        locationData.put(
                "is_shared",
                false
        );

        locationAPI
                .updateLocation(
                        "eq." + userId,
                        locationData
                )
                .enqueue(
                        new Callback<List<Map<String, Object>>>() {

                            @Override
                            public void onResponse(
                                    Call<List<Map<String, Object>>> call,
                                    Response<List<Map<String, Object>>> response
                            ) {

                                if (response.isSuccessful()) {

                                    Log.d(
                                            TAG,
                                            "LOCATION SHARING STOPPED"
                                    );

                                    Log.d(
                                            TAG,
                                            "LAST LOCATION RETAINED: " +
                                                    currentLatitude +
                                                    ", " +
                                                    currentLongitude
                                    );

                                    Toast.makeText(
                                            HomeActivity.this,
                                            "Location sharing has been stopped.",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                } else {

                                    Toast.makeText(
                                            HomeActivity.this,
                                            "Location sharing was stopped on this device, but the server could not be updated.",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<List<Map<String, Object>>> call,
                                    Throwable t
                            ) {

                                Log.e(
                                        TAG,
                                        "UNSHARE NETWORK FAILURE",
                                        t
                                );

                                Toast.makeText(
                                        HomeActivity.this,
                                        "Location sharing was stopped on this device, but the server could not be reached.",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }

    // =========================================================
    // EMERGENCY LIVE LOCATION TRACKING
    // =========================================================

    private void startEmergencyLiveLocationTracking() {

        if (emergencyLocationTracking) {

            Log.d(
                    TAG,
                    "EMERGENCY LIVE TRACKING ALREADY RUNNING"
            );

            return;
        }

        Intent serviceIntent =
                new Intent(
                        this,
                        EmergencyLocationService.class
                );

        ContextCompat.startForegroundService(
                this,
                serviceIntent
        );

        boolean fineGranted =
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseGranted =
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        if (!fineGranted && !coarseGranted) {

            Log.e(
                    TAG,
                    "LOCATION PERMISSION NOT GRANTED FOR EMERGENCY TRACKING"
            );

            Toast.makeText(
                    this,
                    "Emergency tracking cannot start because location permission is missing.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        Log.d(
                TAG,
                "================================"
        );

        Log.d(
                TAG,
                "STARTING EMERGENCY LIVE LOCATION TRACKING"
        );

        Log.d(
                TAG,
                "UPDATE INTERVAL: 3 SECONDS"
        );

        Log.d(
                TAG,
                "================================"
        );

        emergencyLocationRequest =
                new LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        EMERGENCY_LOCATION_INTERVAL
                )
                        .setMinUpdateIntervalMillis(
                                2000
                        )
                        .setMaxUpdateDelayMillis(
                                3000
                        )
                        .build();

        emergencyLocationCallback =
                new LocationCallback() {

                    @Override
                    public void onLocationResult(
                            @NonNull LocationResult locationResult
                    ) {

                        if (locationResult.getLastLocation() == null) {
                            return;
                        }

                        android.location.Location location =
                                locationResult.getLastLocation();

                        currentLatitude =
                                location.getLatitude();

                        currentLongitude =
                                location.getLongitude();

                        saveLocationState();

                        Log.d(
                                TAG,
                                "================================"
                        );

                        Log.d(
                                TAG,
                                "EMERGENCY LIVE LOCATION UPDATE"
                        );

                        Log.d(
                                TAG,
                                "Latitude: " +
                                        currentLatitude
                        );

                        Log.d(
                                TAG,
                                "Longitude: " +
                                        currentLongitude
                        );

                        Log.d(
                                TAG,
                                "PROCESSING EMERGENCY LOCATION UPDATE..."
                        );

                        Log.d(
                                TAG,
                                "================================"
                        );

                        updateEmergencyLocation();
                    }
                };

        fusedLocationClient.requestLocationUpdates(
                emergencyLocationRequest,
                emergencyLocationCallback,
                getMainLooper()
        );

        emergencyLocationTracking = true;

        if (currentLatitude != 0.0
                && currentLongitude != 0.0) {

            Log.d(
                    TAG,
                    "SENDING LAST KNOWN LOCATION IMMEDIATELY"
            );

            updateEmergencyLocation();
        }

        Toast.makeText(
                this,
                "Emergency live location tracking started.",
                Toast.LENGTH_SHORT
        ).show();
    }
// =========================================================
// UPDATE EMERGENCY LOCATION
// =========================================================

    private void updateEmergencyLocation() {

        String userId =
                getLoggedInUserId();

        if (userId.isEmpty()) {

            Log.e(
                    TAG,
                    "CANNOT UPDATE EMERGENCY LOCATION: USER ID EMPTY"
            );

            return;
        }

        if (currentLatitude == 0.0
                && currentLongitude == 0.0) {

            Log.e(
                    TAG,
                    "CANNOT UPDATE EMERGENCY LOCATION: INVALID COORDINATES"
            );

            return;
        }

        /*
         * If the user manually stopped sharing location,
         * do not send new coordinates to Supabase.
         *
         * The emergency can remain active,
         * but Supabase keeps the last known location.
         */
        if (!isLocationShared) {

            Log.d(
                    TAG,
                    "EMERGENCY ACTIVE BUT LOCATION SHARING IS OFF"
            );

            Log.d(
                    TAG,
                    "KEEPING LAST KNOWN LOCATION IN SUPABASE"
            );

            return;
        }

        Map<String, Object> locationData =
                new HashMap<>();

        locationData.put(
                "latitude",
                currentLatitude
        );

        locationData.put(
                "longitude",
                currentLongitude
        );

        Log.d(
                TAG,
                "SENDING EMERGENCY LOCATION TO SUPABASE..."
        );

        locationAPI
                .updateLocation(
                        "eq." + userId,
                        locationData
                )
                .enqueue(
                        new Callback<List<Map<String, Object>>>() {

                            @Override
                            public void onResponse(
                                    Call<List<Map<String, Object>>> call,
                                    Response<List<Map<String, Object>>> response
                            ) {

                                if (response.isSuccessful()) {

                                    List<Map<String, Object>> updatedRows =
                                            response.body();

                                    if (updatedRows == null
                                            || updatedRows.isEmpty()) {

                                        Log.d(
                                                TAG,
                                                "NO LOCATION ROW FOUND. CREATING INITIAL LOCATION..."
                                        );

                                        createInitialLocation(userId);

                                        return;
                                    }

                                    Log.d(
                                            TAG,
                                            "EMERGENCY LOCATION SENT TO SUPABASE SUCCESSFULLY"
                                    );

                                } else {

                                    String errorBody = "";

                                    try {

                                        if (response.errorBody() != null) {

                                            errorBody =
                                                    response.errorBody().string();
                                        }

                                    } catch (Exception e) {

                                        Log.e(
                                                TAG,
                                                "ERROR READING EMERGENCY LOCATION ERROR",
                                                e
                                        );
                                    }

                                    Log.e(
                                            TAG,
                                            "FAILED TO SEND EMERGENCY LOCATION: " +
                                                    response.code()
                                    );

                                    Log.e(
                                            TAG,
                                            "ERROR BODY: " +
                                                    errorBody
                                    );
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<List<Map<String, Object>>> call,
                                    Throwable t
                            ) {

                                Log.e(
                                        TAG,
                                        "EMERGENCY LOCATION NETWORK FAILURE",
                                        t
                                );
                            }
                        }
                );
    }
    // =========================================================
    // STOP EMERGENCY LIVE LOCATION TRACKING
    // =========================================================

    private void stopEmergencyLiveLocationTracking() {

        if (!emergencyLocationTracking) {
            return;
        }

        Log.d(
                TAG,
                "STOPPING EMERGENCY LIVE LOCATION TRACKING"
        );

        if (fusedLocationClient != null
                && emergencyLocationCallback != null) {

            fusedLocationClient.removeLocationUpdates(
                    emergencyLocationCallback
            );
        }

        emergencyLocationCallback = null;

        emergencyLocationRequest = null;

        emergencyLocationTracking = false;
    }

    // =========================================================
    // PANIC BUTTON
    // =========================================================

    private void setupPanicButton() {

        if (panicButton == null) {
            return;
        }

        panicButton.setOnTouchListener(
                (v, event) -> {

                    switch (event.getAction()) {

                        case MotionEvent.ACTION_DOWN:

                            panicTriggered = false;

                            startVibration();

                            startPanicRotation();

                            panicHandler.postDelayed(
                                    panicCallRunnable,
                                    PANIC_HOLD_DURATION
                            );

                            return true;

                        case MotionEvent.ACTION_UP:

                            panicHandler.removeCallbacks(
                                    panicCallRunnable
                            );

                            stopVibration();

                            stopPanicRotation();

                            if (!panicTriggered) {

                                Toast.makeText(
                                        HomeActivity.this,
                                        "Hold the panic button for 3 seconds to call the Barangay Admin.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }

                            return true;

                        case MotionEvent.ACTION_CANCEL:

                            panicHandler.removeCallbacks(
                                    panicCallRunnable
                            );

                            stopVibration();

                            stopPanicRotation();

                            return true;
                    }

                    return true;
                }
        );
    }

    // =========================================================
    // PANIC RING ROTATION
    // =========================================================

    private void startPanicRotation() {

        if (panicButton == null) {
            return;
        }

        stopPanicRotation();

        panicRotationAnimator =
                ObjectAnimator.ofFloat(
                        panicButton,
                        "rotation",
                        0f,
                        360f
                );

        panicRotationAnimator.setDuration(1000);

        panicRotationAnimator.setRepeatCount(
                ObjectAnimator.INFINITE
        );

        panicRotationAnimator.setInterpolator(
                new LinearInterpolator()
        );

        panicRotationAnimator.start();
    }

    // =========================================================
    // STOP PANIC ROTATION
    // =========================================================

    private void stopPanicRotation() {

        if (panicRotationAnimator != null) {

            panicRotationAnimator.cancel();

            panicRotationAnimator = null;
        }

        if (panicButton != null) {

            panicButton.setRotation(0f);
        }
    }

    // =========================================================
    // START VIBRATION
    // =========================================================

    private void startVibration() {

        if (vibrator == null) {
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= 26) {

            vibrator.vibrate(
                    VibrationEffect.createWaveform(
                            new long[]{
                                    0,
                                    500,
                                    500
                            },
                            0
                    )
            );

        } else {

            vibrator.vibrate(
                    new long[]{
                            0,
                            500,
                            500
                    },
                    0
            );
        }
    }

    // =========================================================
    // STOP VIBRATION
    // =========================================================

    private void stopVibration() {

        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    // =========================================================
// CALL BUTTONS
// =========================================================

    private void setupCallButtons() {

        // =====================================================
        // 911
        // =====================================================

        if (card911 != null) {

            card911.setOnClickListener(v -> {

                /*
                 * 911 is now treated as an emergency call.
                 *
                 * This allows the existing emergency location
                 * tracking and call-state system to work.
                 *
                 * We do NOT change the existing panic-button flow.
                 */

                makePhoneCall(
                        EMERGENCY_911_NUMBER,
                        true
                );
            });
        }

        // =====================================================
        // MSWDO
        // =====================================================

        if (cardMSWDO != null) {

            cardMSWDO.setOnClickListener(v -> {

                getActiveSuperAdminAndCallMSWDO();

            });
        }
    }

    // =========================================================
// GET ACTIVE SUPER ADMIN → CALL MSWDO
// =========================================================

    private void getActiveSuperAdminAndCallMSWDO() {

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

            Log.e(
                    TAG,
                    "CANNOT GET MSWDO NUMBER: ACCESS TOKEN IS EMPTY"
            );

            Toast.makeText(
                    this,
                    "Unable to get MSWDO contact number.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String bearerToken =
                "Bearer " + accessToken.trim();

        Log.d(
                TAG,
                "GETTING ACTIVE SUPER ADMIN FOR MSWDO CALL"
        );

        userAPI.getActiveSuperAdmin(
                bearerToken,
                "eq.super_admin",
                "eq.active",
                "id,first_name,last_name,phone_number"
        ).enqueue(
                new Callback<List<Map<String, Object>>>() {

                    @Override
                    public void onResponse(
                            Call<List<Map<String, Object>>> call,
                            Response<List<Map<String, Object>>> response
                    ) {

                        Log.d(
                                TAG,
                                "SUPER ADMIN HTTP CODE: " +
                                        response.code()
                        );

                        if (!response.isSuccessful()
                                || response.body() == null
                                || response.body().isEmpty()) {

                            Log.e(
                                    TAG,
                                    "NO ACTIVE SUPER ADMIN FOUND"
                            );

                            Toast.makeText(
                                    HomeActivity.this,
                                    "MSWDO contact number is currently unavailable.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        Map<String, Object> superAdmin =
                                response.body().get(0);

                        Object superAdminIdObject =
                                superAdmin.get("id");

                        if (superAdminIdObject != null
                                && !superAdminIdObject.toString().trim().isEmpty()) {

                            pendingMSWDOCalledUserId =
                                    superAdminIdObject.toString().trim();

                            Log.d(
                                    TAG,
                                    "MSWDO CALLED USER ID: " +
                                            pendingMSWDOCalledUserId
                            );
                        }



                        Object phoneObject =
                                superAdmin.get("phone_number");

                        if (phoneObject == null
                                || phoneObject.toString().trim().isEmpty()) {

                            Log.e(
                                    TAG,
                                    "SUPER ADMIN PHONE NUMBER IS EMPTY"
                            );

                            Toast.makeText(
                                    HomeActivity.this,
                                    "MSWDO contact number is currently unavailable.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        String phoneNumber =
                                phoneObject.toString().trim();

                        Log.d(
                                TAG,
                                "MSWDO PHONE NUMBER FROM USERS: " +
                                        phoneNumber
                        );

                        makePhoneCall(
                                phoneNumber,
                                true
                        );
                    }

                    @Override
                    public void onFailure(
                            Call<List<Map<String, Object>>> call,
                            Throwable t
                    ) {

                        Log.e(
                                TAG,
                                "FAILED TO GET ACTIVE SUPER ADMIN",
                                t
                        );

                        Toast.makeText(
                                HomeActivity.this,
                                "Unable to get MSWDO contact number.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }


// =========================================================
// MAKE PHONE CALL
// =========================================================

    private void makePhoneCall(
            String phoneNumber,
            boolean emergencyCall
    ) {

        if (phoneNumber == null
                || phoneNumber.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Emergency phone number is not available.",
                    Toast.LENGTH_LONG
            ).show();

            Log.e(
                    TAG,
                    "PHONE NUMBER IS EMPTY"
            );

            return;
        }

        String cleanedNumber =
                phoneNumber.trim()
                        .replace(" ", "")
                        .replace("-", "")
                        .replace("(", "")
                        .replace(")", "");

        /*
         * Convert Philippine mobile numbers into international format.
         *
         * 09XXXXXXXXX -> +639XXXXXXXXX
         * 9XXXXXXXXX  -> +639XXXXXXXXX
         */

        if (cleanedNumber.startsWith("0")
                && cleanedNumber.length() == 11) {

            cleanedNumber =
                    "0" +
                            cleanedNumber.substring(1);

        } else if (cleanedNumber.length() == 10
                && cleanedNumber.startsWith("9")) {

            cleanedNumber =
                    "0" +
                            cleanedNumber;
        }

        Log.d(
                TAG,
                "================================"
        );

        Log.d(
                TAG,
                "PREPARING PHONE CALL"
        );

        Log.d(
                TAG,
                "ORIGINAL NUMBER: " +
                        phoneNumber
        );

        Log.d(
                TAG,
                "CLEANED NUMBER: " +
                        cleanedNumber
        );

        Log.d(
                TAG,
                "EMERGENCY CALL: " +
                        emergencyCall
        );

        Log.d(
                TAG,
                "================================"
        );

        // =====================================================
        // CHECK CALL PERMISSION
        // =====================================================

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
        ) != PackageManager.PERMISSION_GRANTED) {

            SharedPreferences preferences =
                    getSharedPreferences(
                            "HavenCall",
                            MODE_PRIVATE
                    );

            /*
             * Save the call information so the existing
             * permission-result code can continue the call.
             */

            preferences.edit()
                    .putString(
                            "pending_number",
                            cleanedNumber
                    )
                    .putBoolean(
                            "pending_emergency_call",
                            emergencyCall
                    )
                    .apply();

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.CALL_PHONE
                    },
                    CALL_PERMISSION_REQUEST_CODE
            );

            return;
        }

        // =====================================================
// PERMISSION ALREADY GRANTED
// =====================================================

        if (emergencyCall) {

            String callType;

            if (EMERGENCY_911_NUMBER.equals(cleanedNumber)) {
                callType = "911";
            } else {
                callType = "mswdo";
            }

            createNormalCallLogAndCall(
                    cleanedNumber,
                    callType
            );

        } else {

            makeDirectPhoneCall(
                    cleanedNumber,
                    false

            );
        }
    }


// =========================================================
// DIRECT PHONE CALL
// =========================================================

    private void makeDirectPhoneCall(
            String phoneNumber,
            boolean emergencyCall
    ) {

        try {

            /*
             * Only emergency calls start the existing
             * emergency live-location tracking.
             *
             * Normal/non-emergency calls remain unaffected.
             */

            if (emergencyCall) {

                emergencyCallStarted = true;
                emergencyStillActive = true;

                startEmergencyLiveLocationTracking();
            }

            Intent callIntent =
                    new Intent(
                            Intent.ACTION_CALL
                    );

            callIntent.setData(
                    Uri.parse(
                            "tel:" + phoneNumber
                    )
            );

            startActivity(callIntent);

        } catch (SecurityException e) {

            /*
             * If Android rejects the call permission,
             * clean up emergency tracking.
             */

            emergencyCallStarted = false;

            stopEmergencyLiveLocationTracking();

            Log.e(
                    TAG,
                    "CALL PERMISSION ERROR",
                    e
            );

            Toast.makeText(
                    this,
                    "Unable to make the call. Please check phone permission.",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            /*
             * If the phone call cannot be started,
             * clean up emergency tracking.
             */

            emergencyCallStarted = false;

            stopEmergencyLiveLocationTracking();

            Log.e(
                    TAG,
                    "CALL FAILED",
                    e
            );

            Toast.makeText(
                    this,
                    "Unable to make the call.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
// =========================================================
// CREATE NORMAL EMERGENCY CALL LOG THEN MAKE CALL
// =========================================================

    private void createNormalCallLogAndCall(
            String phoneNumber,
            String callType
    ) {

        if (callLogAPI == null) {

            Log.e(
                    TAG,
                    "CALL LOG API IS NULL"
            );

            makeDirectPhoneCall(
                    phoneNumber,
                    true
            );

            return;
        }

        String callerUserId =
                getLoggedInUserId();

        if (callerUserId == null
                || callerUserId.trim().isEmpty()) {

            Log.e(
                    TAG,
                    "CALLER USER ID IS EMPTY"
            );

            makeDirectPhoneCall(
                    phoneNumber,
                    true
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

        if (accessToken == null
                || accessToken.trim().isEmpty()) {

            Log.e(
                    TAG,
                    "ACCESS TOKEN IS EMPTY"
            );

            makeDirectPhoneCall(
                    phoneNumber,
                    true
            );

            return;
        }

        String bearerToken =
                "Bearer " + accessToken.trim();

        // =====================================================
        // 911
        // =====================================================

        // =====================================================
// 911
// =====================================================

        if ("911".equalsIgnoreCase(callType)) {

            Log.d(
                    TAG,
                    "GETTING BARANGAY FOR 911 CALL"
            );

            userAPI.getProfile(
                    bearerToken,
                    "eq." + callerUserId
            ).enqueue(
                    new Callback<List<Profile>>() {

                        @Override
                        public void onResponse(
                                Call<List<Profile>> call,
                                Response<List<Profile>> response
                        ) {

                            if (!response.isSuccessful()
                                    || response.body() == null
                                    || response.body().isEmpty()) {

                                Log.e(
                                        TAG,
                                        "UNABLE TO GET USER PROFILE FOR 911 CALL"
                                );

                                // Still create the call log and make the emergency call
                                createMSWDOOr911CallLog(
                                        bearerToken,
                                        callerUserId,
                                        null,
                                        phoneNumber,
                                        callType
                                );

                                return;
                            }

                            Profile profile =
                                    response.body().get(0);

                            String barangayId =
                                    profile.getBarangayId();

                            Log.d(
                                    TAG,
                                    "911 BARANGAY ID: " +
                                            barangayId
                            );

                            createMSWDOOr911CallLog(
                                    bearerToken,
                                    callerUserId,
                                    barangayId,
                                    phoneNumber,
                                    callType
                            );
                        }

                        @Override
                        public void onFailure(
                                Call<List<Profile>> call,
                                Throwable t
                        ) {

                            Log.e(
                                    TAG,
                                    "FAILED TO GET BARANGAY FOR 911 CALL",
                                    t
                            );

                            // Emergency call must still continue
                            createMSWDOOr911CallLog(
                                    bearerToken,
                                    callerUserId,
                                    null,
                                    phoneNumber,
                                    callType
                            );
                        }
                    }
            );

            return;
        }

        // =====================================================
        // MSWDO
        // =====================================================

        if ("mswdo".equalsIgnoreCase(callType)) {

            Log.d(
                    TAG,
                    "================================"
            );

            Log.d(
                    TAG,
                    "GETTING BARANGAY FOR MSWDO CALL"
            );

            Log.d(
                    TAG,
                    "CALLER USER ID: " +
                            callerUserId
            );

            Log.d(
                    TAG,
                    "================================"
            );

            userAPI.getProfile(
                    bearerToken,
                    "eq." + callerUserId
            ).enqueue(
                    new Callback<List<Profile>>() {

                        @Override
                        public void onResponse(
                                Call<List<Profile>> call,
                                Response<List<Profile>> response
                        ) {

                            Log.d(
                                    TAG,
                                    "MSWDO PROFILE HTTP CODE: " +
                                            response.code()
                            );

                            if (!response.isSuccessful()
                                    || response.body() == null
                                    || response.body().isEmpty()) {

                                Log.e(
                                        TAG,
                                        "UNABLE TO GET USER PROFILE FOR MSWDO CALL"
                                );

                                /*
                                 * Still make the emergency call.
                                 */
                                createMSWDOOr911CallLog(
                                        bearerToken,
                                        callerUserId,
                                        null,
                                        phoneNumber,
                                        callType
                                );

                                return;
                            }

                            Profile profile =
                                    response.body().get(0);

                            String barangayId =
                                    profile.getBarangayId();

                            Log.d(
                                    TAG,
                                    "MSWDO BARANGAY ID: " +
                                            barangayId
                            );

                            /*
                             * Create the MSWDO call log using
                             * the user's actual barangay.
                             */
                            createMSWDOOr911CallLog(
                                    bearerToken,
                                    callerUserId,
                                    barangayId,
                                    phoneNumber,
                                    callType
                            );
                        }

                        @Override
                        public void onFailure(
                                Call<List<Profile>> call,
                                Throwable t
                        ) {

                            Log.e(
                                    TAG,
                                    "FAILED TO GET BARANGAY FOR MSWDO CALL",
                                    t
                            );

                            /*
                             * Do not block the emergency call.
                             */
                            createMSWDOOr911CallLog(
                                    bearerToken,
                                    callerUserId,
                                    null,
                                    phoneNumber,
                                    callType
                            );
                        }
                    }
            );

            return;
        }

        // =====================================================
        // FALLBACK
        // =====================================================

        Log.e(
                TAG,
                "UNKNOWN EMERGENCY CALL TYPE: " +
                        callType
        );

        makeDirectPhoneCall(
                phoneNumber,
                true
        );
    }


// =========================================================
// CREATE MSWDO / 911 CALL LOG
// =========================================================

    private void createMSWDOOr911CallLog(
            String bearerToken,
            String callerUserId,
            String barangayId,
            String phoneNumber,
            String callType
    ) {

        Log.e(
                TAG,
                "========== CALL TYPE TRACE =========="
        );

        Log.e(
                TAG,
                "RECEIVED callType = [" + callType + "]"
        );

        Log.e(
                TAG,
                "RECEIVED phoneNumber = [" + phoneNumber + "]"
        );

        Log.e(
                TAG,
                "====================================="
        );
        Map<String, Object> callData =
                new HashMap<>();

        // =====================================================
        // REQUIRED DATA
        // =====================================================

        callData.put(
                "caller_user_id",
                callerUserId
        );

        // =====================================================
// MSWDO CALLED USER ID
// =====================================================

        if ("mswdo".equalsIgnoreCase(callType)
                && pendingMSWDOCalledUserId != null
                && !pendingMSWDOCalledUserId.trim().isEmpty()) {

            callData.put(
                    "called_user_id",
                    pendingMSWDOCalledUserId
            );

            Log.d(
                    TAG,
                    "ADDING MSWDO CALLED USER ID TO CALL LOG: " +
                            pendingMSWDOCalledUserId
            );
        }

        /*
         * MSWDO gets barangay_id.
         *
         * 911 normally receives null here.
         */
        if (barangayId != null
                && !barangayId.trim().isEmpty()) {

            callData.put(
                    "barangay_id",
                    barangayId
            );
        }

        callData.put(
                "called_phone_number",
                phoneNumber
        );

        callData.put(
                "call_type",
                callType
        );

        Log.e(
                TAG,
                "ACTUAL CALL DATA BEFORE API: " +
                        callData.toString()
        );

        callData.put(
                "call_status",
                "active"
        );

        callData.put(
                "admin_notified",
                true
        );

        callData.put(
                "mswdo_notified",
                true
        );

        Log.d(
                TAG,
                "================================"
        );

        Log.d(
                TAG,
                "CREATING " +
                        callType.toUpperCase() +
                        " CALL LOG"
        );

        Log.d(
                TAG,
                "CALLER USER ID: " +
                        callerUserId
        );

        Log.d(
                TAG,
                "BARANGAY ID: " +
                        barangayId
        );

        Log.d(
                TAG,
                "PHONE NUMBER: " +
                        phoneNumber
        );

        Log.d(
                TAG,
                "CALL TYPE: " +
                        callType
        );

        Log.d(
                TAG,
                "================================"
        );

        callLogAPI
                .createCallLog(
                        bearerToken,
                        callData
                )
                .enqueue(
                        new Callback<List<Map<String, Object>>>() {

                            @Override
                            public void onResponse(
                                    Call<List<Map<String, Object>>> call,
                                    Response<List<Map<String, Object>>> response
                            ) {

                                Log.d(
                                        TAG,
                                        callType.toUpperCase() +
                                                " CALL LOG HTTP CODE: " +
                                                response.code()
                                );

                                if (response.isSuccessful()
                                        && response.body() != null
                                        && !response.body().isEmpty()) {

                                    Map<String, Object> row =
                                            response.body().get(0);

                                    Object id =
                                            row.get("id");

                                    if (id != null
                                            && !id.toString().trim().isEmpty()) {

                                        activeCallLogId =
                                                id.toString().trim();

                                        if ("mswdo".equalsIgnoreCase(callType)) {

                                            pendingMSWDOCalledUserId = null;

                                            Log.d(
                                                    TAG,
                                                    "CLEARED PENDING MSWDO CALLED USER ID"
                                            );
                                        }

                                        SharedPreferences callPreferences =
                                                getSharedPreferences(
                                                        "HavenCall",
                                                        MODE_PRIVATE
                                                );

                                        callPreferences.edit()
                                                .putString(
                                                        "active_call_log_id",
                                                        activeCallLogId
                                                )
                                                .apply();

                                        Log.d(
                                                TAG,
                                                "ACTIVE CALL LOG ID SAVED LOCALLY: " +
                                                        activeCallLogId
                                        );

                                        Log.d(
                                                TAG,
                                                "================================"
                                        );

                                        Log.d(
                                                TAG,
                                                callType.toUpperCase() +
                                                        " CALL LOG CREATED"
                                        );

                                        Log.d(
                                                TAG,
                                                "CALL LOG ID: " +
                                                        activeCallLogId
                                        );

                                        Log.d(
                                                TAG,
                                                "================================"
                                        );

                                        /*
                                         * IMPORTANT:
                                         *
                                         * The same activeCallLogId will be used
                                         * later by:
                                         *
                                         * updateCallLogAsActive()
                                         * updateCallLogAsEnded()
                                         *
                                         * Therefore no second row should be created.
                                         */

                                        makeDirectPhoneCall(
                                                phoneNumber,
                                                true
                                        );

                                    } else {

                                        Log.e(
                                                TAG,
                                                "CALL LOG CREATED BUT ID IS EMPTY"
                                        );

                                        /*
                                         * Still allow emergency call.
                                         */
                                        makeDirectPhoneCall(
                                                phoneNumber,
                                                true
                                        );
                                    }

                                } else {

                                    String errorBody = "";

                                    try {

                                        if (response.errorBody() != null) {

                                            errorBody =
                                                    response.errorBody().string();
                                        }

                                    } catch (Exception e) {

                                        Log.e(
                                                TAG,
                                                "ERROR READING CALL LOG ERROR",
                                                e
                                        );
                                    }

                                    Log.e(
                                            TAG,
                                            callType.toUpperCase() +
                                                    " CALL LOG FAILED: " +
                                                    response.code()
                                    );

                                    Log.e(
                                            TAG,
                                            "ERROR BODY: " +
                                                    errorBody
                                    );

                                    /*
                                     * Logging failure must never
                                     * prevent the emergency call.
                                     */
                                    makeDirectPhoneCall(
                                            phoneNumber,
                                            true
                                    );
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<List<Map<String, Object>>> call,
                                    Throwable t
                            ) {

                                Log.e(
                                        TAG,
                                        callType.toUpperCase() +
                                                " CALL LOG NETWORK FAILURE",
                                        t
                                );

                                /*
                                 * Still make the emergency call.
                                 */
                                makeDirectPhoneCall(
                                        phoneNumber,
                                        true
                                );
                            }
                        }
                );
    }


    // =========================================================
    // PANIC → REFRESH SESSION → BARANGAY ADMIN
    // =========================================================

    private void callMSWDO() {

        String userId =
                getLoggedInUserId();

        if (userId.isEmpty()) {

            Toast.makeText(
                    this,
                    "Unable to identify your account.",
                    Toast.LENGTH_LONG
            ).show();

            Log.e(
                    TAG,
                    "USER ID IS EMPTY"
            );

            return;
        }

        startEmergencyLiveLocationTracking();

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

        String existingAccessToken =
                preferences.getString(
                        "access_token",
                        ""
                );

        if (refreshToken != null
                && !refreshToken.trim().isEmpty()) {

            Log.d(
                    TAG,
                    "REFRESH TOKEN FOUND"
            );

            Log.d(
                    TAG,
                    "REFRESHING SUPABASE SESSION BEFORE PANIC CALL"
            );

            authAPI.refreshToken(
                    "refresh_token",
                    new RefreshTokenRequest(
                            refreshToken
                    )
            ).enqueue(
                    new Callback<AuthResponse>() {

                        @Override
                        public void onResponse(
                                Call<AuthResponse> call,
                                Response<AuthResponse> response
                        ) {

                            Log.d(
                                    TAG,
                                    "TOKEN REFRESH HTTP CODE: " +
                                            response.code()
                            );

                            if (!response.isSuccessful()
                                    || response.body() == null) {

                                String errorBody = "";

                                try {

                                    if (response.errorBody() != null) {

                                        errorBody =
                                                response.errorBody().string();
                                    }

                                } catch (Exception e) {

                                    Log.e(
                                            TAG,
                                            "FAILED TO READ TOKEN REFRESH ERROR",
                                            e
                                    );
                                }

                                Log.e(
                                        TAG,
                                        "TOKEN REFRESH FAILED: " +
                                                errorBody
                                );

                                if (existingAccessToken != null
                                        && !existingAccessToken.trim().isEmpty()) {

                                    Log.d(
                                            TAG,
                                            "USING EXISTING ACCESS TOKEN"
                                    );

                                    getUserBarangayAndCallAdmin(
                                            "Bearer " +
                                                    existingAccessToken.trim(),
                                            userId
                                    );

                                    return;
                                }

                                Toast.makeText(
                                        HomeActivity.this,
                                        "Your login session has expired. Please log in again.",
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }

                            AuthResponse auth =
                                    response.body();

                            String newAccessToken =
                                    auth.getAccess_token();

                            String newRefreshToken =
                                    auth.getRefresh_token();

                            if (newAccessToken == null
                                    || newAccessToken.trim().isEmpty()) {

                                Log.e(
                                        TAG,
                                        "NEW ACCESS TOKEN IS EMPTY"
                                );

                                if (existingAccessToken != null
                                        && !existingAccessToken.trim().isEmpty()) {

                                    getUserBarangayAndCallAdmin(
                                            "Bearer " +
                                                    existingAccessToken.trim(),
                                            userId
                                    );

                                    return;
                                }

                                Toast.makeText(
                                        HomeActivity.this,
                                        "Unable to refresh your session. Please log in again.",
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }

                            SharedPreferences.Editor editor =
                                    preferences.edit();

                            editor.putString(
                                    "access_token",
                                    newAccessToken
                            );

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
                                    "SUPABASE SESSION REFRESHED SUCCESSFULLY"
                            );

                            getUserBarangayAndCallAdmin(
                                    "Bearer " + newAccessToken,
                                    userId
                            );
                        }

                        @Override
                        public void onFailure(
                                Call<AuthResponse> call,
                                Throwable t
                        ) {

                            Log.e(
                                    TAG,
                                    "TOKEN REFRESH NETWORK FAILURE",
                                    t
                            );

                            if (existingAccessToken != null
                                    && !existingAccessToken.trim().isEmpty()) {

                                getUserBarangayAndCallAdmin(
                                        "Bearer " +
                                                existingAccessToken.trim(),
                                        userId
                                );

                                return;
                            }

                            Toast.makeText(
                                    HomeActivity.this,
                                    "Unable to refresh your session. Please check your internet connection.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );

            return;
        }

        Log.d(
                TAG,
                "REFRESH TOKEN IS EMPTY"
        );

        if (existingAccessToken != null
                && !existingAccessToken.trim().isEmpty()) {

            Log.d(
                    TAG,
                    "NO REFRESH TOKEN BUT ACCESS TOKEN EXISTS"
            );

            getUserBarangayAndCallAdmin(
                    "Bearer " +
                            existingAccessToken.trim(),
                    userId
            );

            return;
        }

        Toast.makeText(
                this,
                "Your login session cannot be refreshed. Please log in again.",
                Toast.LENGTH_LONG
        ).show();

        Log.e(
                TAG,
                "NO REFRESH TOKEN AND NO ACCESS TOKEN"
        );
    }

    // =========================================================
    // GET USER BARANGAY → FIND BARANGAY ADMIN
    // =========================================================

    private void getUserBarangayAndCallAdmin(
            String token,
            String userId
    ) {

        Log.d(
                TAG,
                "GETTING USER PROFILE FOR BARANGAY"
        );

        userAPI.getProfile(
                token,
                "eq." + userId
        ).enqueue(
                new Callback<List<Profile>>() {

                    @Override
                    public void onResponse(
                            Call<List<Profile>> call,
                            Response<List<Profile>> response
                    ) {

                        Log.d(
                                TAG,
                                "USER BARANGAY HTTP CODE: " +
                                        response.code()
                        );

                        if (!response.isSuccessful()) {

                            String errorBody = "";

                            try {

                                if (response.errorBody() != null) {

                                    errorBody =
                                            response.errorBody().string();
                                }

                            } catch (Exception e) {

                                Log.e(
                                        TAG,
                                        "ERROR READING USER BARANGAY RESPONSE",
                                        e
                                );
                            }

                            Log.e(
                                    TAG,
                                    "USER BARANGAY ERROR: " +
                                            errorBody
                            );

                            Toast.makeText(
                                    HomeActivity.this,
                                    "Unable to determine your barangay.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        if (response.body() == null
                                || response.body().isEmpty()) {

                            Log.e(
                                    TAG,
                                    "USER PROFILE NOT FOUND"
                            );

                            Toast.makeText(
                                    HomeActivity.this,
                                    "Unable to determine your barangay.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        Profile profile =
                                response.body().get(0);

                        String callerAuthUserId =
                                profile.getAuth_id();

                        String barangayId =
                                profile.getBarangayId();

                        Log.d(
                                TAG,
                                "USER BARANGAY ID: " +
                                        barangayId
                        );

                        if (barangayId == null
                                || barangayId.trim().isEmpty()) {

                            Log.e(
                                    TAG,
                                    "USER BARANGAY ID IS EMPTY"
                            );

                            Toast.makeText(
                                    HomeActivity.this,
                                    "Your barangay is not assigned.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }


                        findBarangayAdminForPanic(
                            token,
                            callerAuthUserId,
                            barangayId
                            );
                }

                    @Override
                    public void onFailure(
                            Call<List<Profile>> call,
                            Throwable t
                    ) {

                        Log.e(
                                TAG,
                                "FAILED TO GET USER BARANGAY",
                                t
                        );

                        Toast.makeText(
                                HomeActivity.this,
                                "Unable to determine your barangay.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    // =========================================================
    // FIND BARANGAY ADMIN FOR PANIC
    // =========================================================

    private void findBarangayAdminForPanic(
            String token,
            String callerAuthUserId,
            String barangayId
    ) {

        Log.d(
                TAG,
                "================================"
        );

        Log.d(
                TAG,
                "SEARCHING FOR BARANGAY ADMIN"
        );

        Log.d(
                TAG,
                "BARANGAY ID: " +
                        barangayId
        );

        Log.d(
                TAG,
                "================================"
        );

        userAPI.getBarangayAdmin(
                token,
                "eq." + barangayId,
                "eq.barangay_admin",
                "eq.active",
                "id,first_name,last_name,phone_number,barangay_id"
        ).enqueue(
                new Callback<List<Map<String, Object>>>() {

                    @Override
                    public void onResponse(
                            Call<List<Map<String, Object>>> call,
                            Response<List<Map<String, Object>>> response
                    ) {

                        Log.d(
                                TAG,
                                "BARANGAY ADMIN HTTP CODE: " +
                                        response.code()
                        );

                        if (!response.isSuccessful()) {

                            String errorBody = "";

                            try {

                                if (response.errorBody() != null) {

                                    errorBody =
                                            response.errorBody().string();
                                }

                            } catch (Exception e) {

                                Log.e(
                                        TAG,
                                        "ERROR READING ADMIN RESPONSE",
                                        e
                                );
                            }

                            Log.e(
                                    TAG,
                                    "BARANGAY ADMIN ERROR: " +
                                            errorBody
                            );

                            Toast.makeText(
                                    HomeActivity.this,
                                    "Unable to contact your Barangay Admin.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        if (response.body() == null
                                || response.body().isEmpty()) {

                            Log.e(
                                    TAG,
                                    "NO MATCHING BARANGAY ADMIN FOUND"
                            );

                            Toast.makeText(
                                    HomeActivity.this,
                                    "No Barangay Admin is assigned to your barangay.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        Map<String, Object> admin =
                                response.body().get(0);

                        Object phoneObject =
                                admin.get("phone_number");

                        if (phoneObject == null) {

                            Toast.makeText(
                                    HomeActivity.this,
                                    "The Barangay Admin has no phone number.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        String adminNumber =
                                phoneObject
                                        .toString()
                                        .trim();

                        if (adminNumber.isEmpty()) {

                            Toast.makeText(
                                    HomeActivity.this,
                                    "The Barangay Admin has no phone number.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        String calledUserId = null;

                        if (admin.get("id") != null) {

                            calledUserId =
                                    admin.get("id").toString();
                        }

                        Object firstNameObject =
                                admin.get("first_name");

                        Object lastNameObject =
                                admin.get("last_name");

                        String firstName =
                                firstNameObject == null
                                        ? ""
                                        : firstNameObject.toString();

                        String lastName =
                                lastNameObject == null
                                        ? ""
                                        : lastNameObject.toString();

                        String adminName =
                                (firstName + " " + lastName)
                                        .trim();

                        Log.d(
                                TAG,
                                "================================"
                        );

                        Log.d(
                                TAG,
                                "BARANGAY ADMIN FOUND"
                        );

                        Log.d(
                                TAG,
                                "ADMIN NAME: " +
                                        adminName
                        );

                        Log.d(
                                TAG,
                                "ADMIN PHONE: " +
                                        adminNumber
                        );

                        Log.d(
                                TAG,
                                "================================"
                        );

                        /*
                         * IMPORTANT:
                         *
                         * Create the call log first.
                         * Once the database returns the call-log ID,
                         * we make the actual phone call.
                         *
                         * This prevents the phone call from connecting
                         * before activeCallLogId exists.
                         */

                        logPanicCall(
                                callerAuthUserId,
                                barangayId,
                                calledUserId,
                                adminNumber
                        );

                        Toast.makeText(
                                HomeActivity.this,
                                "Connecting to " +
                                        (adminName.isEmpty()
                                                ? "your Barangay Admin"
                                                : adminName) +
                                        "...",
                                Toast.LENGTH_LONG
                        ).show();
                    }

                    @Override
                    public void onFailure(
                            Call<List<Map<String, Object>>> call,
                            Throwable t
                    ) {

                        Log.e(
                                TAG,
                                "FAILED TO FIND BARANGAY ADMIN",
                                t
                        );

                        Toast.makeText(
                                HomeActivity.this,
                                "Unable to contact your Barangay Admin.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    // =========================================================
    // LOG PANIC CALL
    // =========================================================

    private void logPanicCall(
            String callerAuthUserId,
            String barangayId,
            String calledUserId,
            String adminNumber
    ) {
        if (callLogAPI == null) {

            Log.e(
                    TAG,
                    "CALL LOG API IS NULL"
            );

            /*
             * Safety:
             * If logging is unavailable, still make the emergency call.
             */
            makeDirectPhoneCall(
                    adminNumber,
                    true
            );

            return;
        }

        Map<String, Object> callData =
                new HashMap<>();

        callData.put(
                "caller_user_id",
                callerAuthUserId
        );

        callData.put(
                "barangay_id",
                barangayId
        );

        if (calledUserId != null
                && !calledUserId.trim().isEmpty()) {

            callData.put(
                    "called_user_id",
                    calledUserId
            );
        }

        callData.put(
                "called_phone_number",
                adminNumber
        );

        callData.put(
                "call_type",
                "panic"
        );

        /*
         * The phone call has not necessarily connected yet.
         *
         * "active" will be written once Android reports
         * CALL_STATE_OFFHOOK.
         */
        callData.put(
                "call_status",
                "active"
        );

        callData.put(
                "admin_notified",
                true
        );

        callData.put(
                "mswdo_notified",
                true
        );

        Log.d(
                TAG,
                "================================"
        );

        Log.d(
                TAG,
                "CREATING PANIC CALL LOG"
        );

        Log.d(
                TAG,
                "AUTH USER ID: " +
                        callerAuthUserId
        );

        Log.d(
                TAG,
                "BARANGAY ID: " +
                        barangayId
        );

        Log.d(
                TAG,
                "CALLED USER ID: " +
                        calledUserId
        );

        Log.d(
                TAG,
                "PHONE: " +
                        adminNumber
        );

        Log.d(
                TAG,
                "LATITUDE: " +
                        currentLatitude
        );

        Log.d(
                TAG,
                "LONGITUDE: " +
                        currentLongitude
        );

        Log.d(
                TAG,
                "================================"
        );

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

        if (accessToken == null || accessToken.trim().isEmpty()) {

            Log.e(
                    TAG,
                    "CANNOT CREATE CALL LOG: ACCESS TOKEN IS EMPTY"
            );

            // Still allow the emergency call
            makeDirectPhoneCall(
                    adminNumber,
                    true
            );

            return;
        }

        callLogAPI
                .createCallLog(
                        "Bearer " + accessToken.trim(),
                        callData
                )
                .enqueue(
                        new Callback<List<Map<String, Object>>>() {

                            @Override
                            public void onResponse(
                                    Call<List<Map<String, Object>>> call,
                                    Response<List<Map<String, Object>>> response
                            ) {

                                Log.d(
                                        TAG,
                                        "CALL LOG HTTP CODE: " +
                                                response.code()
                                );

                                if (response.isSuccessful()) {

                                    Log.d(
                                            TAG,
                                            "PANIC CALL LOG SAVED SUCCESSFULLY"
                                    );

                                    if (response.body() != null
                                            && !response.body().isEmpty()) {

                                        Map<String, Object> row =
                                                response.body().get(0);

                                        Object id =
                                                row.get("id");

                                        if (id != null
                                                && !id.toString().trim().isEmpty()) {

                                            activeCallLogId =
                                                    id.toString().trim();

                                            Log.d(
                                                    TAG,
                                                    "================================"
                                            );

                                            Log.d(
                                                    TAG,
                                                    "CALL LOG CREATED SUCCESSFULLY"
                                            );

                                            Log.d(
                                                    TAG,
                                                    "CALL LOG ID: " +
                                                            activeCallLogId
                                            );

                                            Log.d(
                                                    TAG,
                                                    "================================"
                                            );

                                            /*
                                             * IMPORTANT:
                                             *
                                             * Only start the emergency call AFTER
                                             * we successfully receive the call log ID.
                                             */

                                            Log.d(
                                                    TAG,
                                                    "CALL LOG CREATED. STARTING ACTUAL PHONE CALL."
                                            );

                                            makeDirectPhoneCall(
                                                    adminNumber,
                                                    true
                                            );

                                        } else {

                                            Log.e(
                                                    TAG,
                                                    "CALL LOG CREATED BUT ID IS EMPTY"
                                            );

                                            Log.e(
                                                    TAG,
                                                    "RESPONSE BODY: " +
                                                            response.body()
                                            );

                                            /*
                                             * Do not start the tracked call because
                                             * there is no call-log ID to update.
                                             */

                                            return;
                                        }

                                    } else {

                                        Log.e(
                                                TAG,
                                                "CALL LOG CREATED BUT RESPONSE BODY IS EMPTY"
                                        );

                                        /*
                                         * The insert may have succeeded, but we don't
                                         * have the ID needed for ACTIVE/ENDED updates.
                                         */

                                        return;
                                    }
                                } else {

                                    String errorBody = "";

                                    try {

                                        if (response.errorBody() != null) {

                                            errorBody =
                                                    response.errorBody().string();
                                        }

                                    } catch (Exception e) {

                                        Log.e(
                                                TAG,
                                                "ERROR READING CALL LOG ERROR",
                                                e
                                        );
                                    }

                                    Log.e(
                                            TAG,
                                            "PANIC CALL LOG FAILED: " +
                                                    response.code()
                                    );

                                    Log.e(
                                            TAG,
                                            "ERROR BODY: " +
                                                    errorBody
                                    );

                                    /*
                                     * IMPORTANT:
                                     *
                                     * Logging failure must NOT stop
                                     * the emergency phone call.
                                     */

                                    Log.d(
                                            TAG,
                                            "CALL LOG FAILED, BUT EMERGENCY CALL WILL CONTINUE."
                                    );

                                    makeDirectPhoneCall(
                                            adminNumber,
                                            true
                                    );
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<List<Map<String, Object>>> call,
                                    Throwable t
                            ) {

                                Log.e(
                                        TAG,
                                        "CALL LOG NETWORK FAILURE",
                                        t
                                );

                                /*
                                 * IMPORTANT:
                                 *
                                 * Still make the emergency call.
                                 */

                                Log.d(
                                        TAG,
                                        "CALL LOG NETWORK FAILURE, BUT EMERGENCY CALL WILL CONTINUE."
                                );

                                makeDirectPhoneCall(
                                        adminNumber,
                                        true
                                );
                            }
                        }
                );
    }

    // =========================================================
    // UPDATE CALL LOG AS ACTIVE
    // =========================================================

    private void updateCallLogAsActive() {

        if (activeCallLogId == null
                || activeCallLogId.trim().isEmpty()) {

            Log.e(
                    TAG,
                    "CANNOT UPDATE ACTIVE CALL: CALL LOG ID IS EMPTY"
            );

            return;
        }

        if (callLogAPI == null) {

            Log.e(
                    TAG,
                    "CANNOT UPDATE ACTIVE CALL: CALL LOG API IS NULL"
            );

            return;
        }

        Map<String, Object> callData =
                new HashMap<>();

        callData.put(
                "call_status",
                "active"
        );

        Log.d(
                TAG,
                "================================"
        );

        Log.d(
                TAG,
                "UPDATING CALL LOG TO ACTIVE"
        );

        Log.d(
                TAG,
                "CALL LOG ID: " +
                        activeCallLogId
        );

        Log.d(
                TAG,
                "================================"
        );

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

        if (accessToken == null || accessToken.trim().isEmpty()) {

            Log.e(
                    TAG,
                    "CANNOT UPDATE ACTIVE CALL: ACCESS TOKEN IS EMPTY"
            );

            return;
        }

        callLogAPI
                .updateCallLog(
                        "Bearer " + accessToken.trim(),
                        "eq." + activeCallLogId,
                        callData
                )
                .enqueue(
                        new Callback<List<Map<String, Object>>>() {

                            @Override
                            public void onResponse(
                                    Call<List<Map<String, Object>>> call,
                                    Response<List<Map<String, Object>>> response
                            ) {

                                Log.d(
                                        TAG,
                                        "ACTIVE CALL UPDATE HTTP CODE: " +
                                                response.code()
                                );

                                if (response.isSuccessful()) {

                                    Log.d(
                                            TAG,
                                            "CALL LOG STATUS = ACTIVE"
                                    );

                                } else {

                                    String errorBody = "";

                                    try {

                                        if (response.errorBody() != null) {

                                            errorBody =
                                                    response.errorBody().string();
                                        }

                                    } catch (Exception e) {

                                        Log.e(
                                                TAG,
                                                "ERROR READING ACTIVE CALL ERROR",
                                                e
                                        );
                                    }

                                    Log.e(
                                            TAG,
                                            "FAILED TO UPDATE CALL STATUS TO ACTIVE"
                                    );

                                    Log.e(
                                            TAG,
                                            "ERROR BODY: " +
                                                    errorBody
                                    );
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<List<Map<String, Object>>> call,
                                    Throwable t
                            ) {

                                Log.e(
                                        TAG,
                                        "ACTIVE CALL UPDATE NETWORK FAILURE",
                                        t
                                );
                            }
                        }
                );
    }

    // =========================================================
    // UPDATE CALL LOG AS ENDED
    // =========================================================

    private void updateCallLogAsEnded() {

        if (activeCallLogId == null
                || activeCallLogId.trim().isEmpty()) {

            SharedPreferences callPreferences =
                    getSharedPreferences(
                            "HavenCall",
                            MODE_PRIVATE
                    );

            String savedCallLogId =
                    callPreferences.getString(
                            "active_call_log_id",
                            ""
                    );

            if (savedCallLogId != null
                    && !savedCallLogId.trim().isEmpty()) {

                activeCallLogId =
                        savedCallLogId.trim();

                Log.d(
                        TAG,
                        "RECOVERED ACTIVE CALL LOG ID: " +
                                activeCallLogId
                );

            } else {

                Log.e(
                        TAG,
                        "CANNOT UPDATE ENDED CALL: CALL LOG ID IS EMPTY"
                );

                return;
            }
        }

        if (callLogAPI == null) {

            Log.e(
                    TAG,
                    "CANNOT UPDATE ENDED CALL: CALL LOG API IS NULL"
            );

            return;
        }

        Map<String, Object> callData =
                new HashMap<>();

        callData.put(
                "call_status",
                "ended"
        );

        String endedAt =
                new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                        Locale.US
                ).format(
                        new Date()
                );

        callData.put(
                "ended_at",
                endedAt
        );

        Log.d(
                TAG,
                "================================"
        );

        Log.d(
                TAG,
                "UPDATING CALL LOG TO ENDED"
        );

        Log.d(
                TAG,
                "CALL LOG ID: " +
                        activeCallLogId
        );

        Log.d(
                TAG,
                "ENDED AT: " +
                        endedAt
        );

        Log.d(
                TAG,
                "================================"
        );

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

            Log.e(
                    TAG,
                    "CANNOT UPDATE ENDED CALL: ACCESS TOKEN IS EMPTY"
            );

            return;
        }

        /*
         * IMPORTANT:
         *
         * We are UPDATING the existing call log.
         *
         * Do NOT use createCallLog() here.
         *
         * The parameters are:
         *
         * 1. Authorization token
         * 2. Existing call-log UUID
         * 3. Fields to update
         */

        callLogAPI.updateCallLog(
                        "Bearer " + accessToken.trim(),
                        "eq." + activeCallLogId,
                        callData
                )
                .enqueue(
                        new Callback<List<Map<String, Object>>>() {

                            @Override
                            public void onResponse(
                                    Call<List<Map<String, Object>>> call,
                                    Response<List<Map<String, Object>>> response
                            ) {

                                Log.d(
                                        TAG,
                                        "ENDED CALL UPDATE HTTP CODE: " +
                                                response.code()
                                );

                                if (response.isSuccessful()) {

                                    Log.d(
                                            TAG,
                                            "================================"
                                    );

                                    if (response.body() != null) {

                                        Log.d(
                                                TAG,
                                                "ENDED CALL RESPONSE BODY: " +
                                                        response.body().toString()
                                        );

                                    } else {

                                        Log.d(
                                                TAG,
                                                "ENDED CALL RESPONSE BODY IS NULL"
                                        );
                                    }
                                    Log.d(
                                            TAG,
                                            "CALL LOG STATUS = ENDED"
                                    );

                                    Log.d(
                                            TAG,
                                            "CALL LOG ID: " +
                                                    activeCallLogId
                                    );

                                    Log.d(
                                            TAG,
                                            "================================"
                                    );

                                    /*
                                     * Clear the active call-log ID only
                                     * after Supabase successfully updates
                                     * the existing row.
                                     */

                                    activeCallLogId = null;

                                    SharedPreferences callPreferences =
                                            getSharedPreferences(
                                                    "HavenCall",
                                                    MODE_PRIVATE
                                            );

                                    callPreferences.edit()
                                            .remove("active_call_log_id")
                                            .apply();

                                    Log.d(
                                            TAG,
                                            "SAVED ACTIVE CALL LOG ID CLEARED"
                                    );

                                } else {

                                    String errorBody = "";

                                    try {

                                        if (response.errorBody() != null) {

                                            errorBody =
                                                    response.errorBody().string();
                                        }

                                    } catch (Exception e) {

                                        Log.e(
                                                TAG,
                                                "ERROR READING ENDED CALL ERROR",
                                                e
                                        );
                                    }

                                    Log.e(
                                            TAG,
                                            "FAILED TO UPDATE CALL LOG TO ENDED"
                                    );

                                    Log.e(
                                            TAG,
                                            "ERROR: " +
                                                    errorBody
                                    );
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<List<Map<String, Object>>> call,
                                    Throwable t
                            ) {

                                Log.e(
                                        TAG,
                                        "ENDED CALL UPDATE NETWORK FAILURE"
                                );

                                Log.e(
                                        TAG,
                                        "ERROR TYPE: " +
                                                (t != null
                                                        ? t.getClass().getName()
                                                        : "null")
                                );

                                Log.e(
                                        TAG,
                                        "ERROR MESSAGE: " +
                                                (t != null
                                                        ? t.getMessage()
                                                        : "null")
                                );

                                if (t != null) {
                                    Log.e(
                                            TAG,
                                            "FULL ERROR",
                                            t
                                    );
                                }
                            }
                        }
                );
    }


    // =========================================================
    // FIND BARANGAY ADMIN
    // =========================================================

    private void findBarangayAdmin(
            String token,
            String barangayId
    ) {

        Log.d(
                TAG,
                "SEARCHING FOR BARANGAY ADMIN"
        );

        Log.d(
                TAG,
                "BARANGAY ID: " +
                        barangayId
        );

        userAPI.getBarangayAdmin(
                token,
                "eq." + barangayId,
                "eq.barangay_admin",
                "eq.active",
                "id,first_name,last_name,phone_number,barangay_id"
        ).enqueue(
                new Callback<List<Map<String, Object>>>() {

                    @Override
                    public void onResponse(
                            Call<List<Map<String, Object>>> call,
                            Response<List<Map<String, Object>>> response
                    ) {

                        Log.d(
                                TAG,
                                "BARANGAY ADMIN HTTP CODE: " +
                                        response.code()
                        );

                        if (!response.isSuccessful()) {

                            String errorBody = "";

                            try {

                                if (response.errorBody() != null) {

                                    errorBody =
                                            response.errorBody().string();
                                }

                            } catch (Exception ignored) {
                            }

                            Log.e(
                                    TAG,
                                    "BARANGAY ADMIN ERROR: " +
                                            errorBody
                            );

                            Toast.makeText(
                                    HomeActivity.this,
                                    "Unable to contact your Barangay Admin.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        List<Map<String, Object>> admins =
                                response.body();

                        if (admins == null
                                || admins.isEmpty()) {

                            Log.e(
                                    TAG,
                                    "NO ACTIVE BARANGAY ADMIN FOUND"
                            );

                            Toast.makeText(
                                    HomeActivity.this,
                                    "No active Barangay Admin is assigned to your barangay.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        Map<String, Object> admin =
                                admins.get(0);

                        Object phoneObject =
                                admin.get("phone_number");

                        if (phoneObject == null) {

                            Toast.makeText(
                                    HomeActivity.this,
                                    "The Barangay Admin has no phone number.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        String adminNumber =
                                phoneObject
                                        .toString()
                                        .trim();

                        if (adminNumber.isEmpty()) {

                            Toast.makeText(
                                    HomeActivity.this,
                                    "The Barangay Admin has no phone number.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        Object firstNameObject =
                                admin.get("first_name");

                        Object lastNameObject =
                                admin.get("last_name");

                        String firstName =
                                firstNameObject == null
                                        ? ""
                                        : firstNameObject.toString();

                        String lastName =
                                lastNameObject == null
                                        ? ""
                                        : lastNameObject.toString();

                        String adminName =
                                (firstName + " " + lastName)
                                        .trim();

                        Log.d(
                                TAG,
                                "MATCHING BARANGAY ADMIN FOUND: " +
                                        adminName
                        );

                        Log.d(
                                TAG,
                                "ADMIN PHONE: " +
                                        adminNumber
                        );

                        Toast.makeText(
                                HomeActivity.this,
                                "Connecting to " +
                                        (adminName.isEmpty()
                                                ? "your Barangay Admin"
                                                : adminName) +
                                        "...",
                                Toast.LENGTH_LONG
                        ).show();

                        makePhoneCall(
                                adminNumber,
                                true
                        );
                    }

                    @Override
                    public void onFailure(
                            Call<List<Map<String, Object>>> call,
                            Throwable t
                    ) {

                        Log.e(
                                TAG,
                                "FAILED TO FIND BARANGAY ADMIN",
                                t
                        );

                        Toast.makeText(
                                HomeActivity.this,
                                "Unable to contact your Barangay Admin.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    // =========================================================
// FLOATING / MOVABLE HOPE ASSISTANT
// =========================================================

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
    // ==========================
// BOTTOM NAVIGATION
// ==========================

    private void setupNavigation() {

        // HOME
        findViewById(R.id.navHome).setOnClickListener(v -> {
            // Already on Home
        });


        // SUPPORT
        findViewById(R.id.navSupport).setOnClickListener(v -> {

            startActivity(
                    new Intent(
                            HomeActivity.this,
                            SupportActivity.class
                    )
            );

            finish();
        });


        // EXPLORE
        findViewById(R.id.navExplore).setOnClickListener(v -> {

            startActivity(
                    new Intent(
                            HomeActivity.this,
                            ExploreActivity.class
                    )
            );

            finish();
        });


        // PROFILE
        findViewById(R.id.navProfile).setOnClickListener(v -> {

            startActivity(
                    new Intent(
                            HomeActivity.this,
                            ProfileActivity.class
                    )
            );

            finish();
        });
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    @Override
    protected void onDestroy() {

        panicHandler.removeCallbacks(
                panicCallRunnable
        );

        stopPanicRotation();

        stopVibration();


        if (!emergencyStillActive) {
            stopEmergencyLiveLocationTracking();
        }

        emergencyCallStarted = false;

        if (telephonyManager != null
                && phoneStateListener != null) {

            telephonyManager.listen(
                    phoneStateListener,
                    PhoneStateListener.LISTEN_NONE
            );
        }

        super.onDestroy();
    }
}