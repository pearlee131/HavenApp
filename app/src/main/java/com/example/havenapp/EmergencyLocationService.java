package com.example.havenapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.havenapp.api.SupabaseClient;
import com.example.havenapp.api.SupabaseLocationAPI;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmergencyLocationService extends Service {

    // =========================================================
    // TAG
    // =========================================================

    private static final String TAG =
            "EMERGENCY_LOCATION";


    // =========================================================
    // NOTIFICATION
    // =========================================================

    private static final String CHANNEL_ID =
            "emergency_location_channel";

    private static final int NOTIFICATION_ID =
            2001;


    // =========================================================
    // LOCATION UPDATE INTERVAL
    // =========================================================

    private static final long LOCATION_UPDATE_INTERVAL =
            3000;


    // =========================================================
    // LOCATION
    // =========================================================

    private FusedLocationProviderClient fusedLocationClient;

    private LocationCallback locationCallback;

    private LocationRequest locationRequest;


    // =========================================================
    // SUPABASE
    // =========================================================

    private SupabaseLocationAPI locationAPI;


    // =========================================================
    // SERVICE START
    // =========================================================

    @Override
    public void onCreate() {

        super.onCreate();

        Log.d(
                TAG,
                "EmergencyLocationService CREATED"
        );

        fusedLocationClient =
                LocationServices
                        .getFusedLocationProviderClient(this);

        locationAPI =
                SupabaseClient
                        .getClient()
                        .create(
                                SupabaseLocationAPI.class
                        );

        createNotificationChannel();

        createLocationRequest();

        createLocationCallback();
    }


    // =========================================================
    // START COMMAND
    // =========================================================

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        Log.d(
                TAG,
                "EmergencyLocationService STARTED"
        );

        startForeground(
                NOTIFICATION_ID,
                createNotification()
        );

        /*
         * Only send an immediate location if
         * HavenApp location sharing is currently enabled.
         */
        if (isLocationSharingEnabled()) {

            getImmediateLocation();

        } else {

            Log.d(
                    TAG,
                    "LOCATION SHARING IS OFF - SKIPPING IMMEDIATE LOCATION"
            );
        }

        /*
         * Keep the service alive during the emergency.
         *
         * The callback itself decides whether a new
         * location should be uploaded.
         */
        startLocationUpdates();

        return START_STICKY;
    }


    // =========================================================
    // CHECK HAVENAPP LOCATION SHARING STATE
    // =========================================================

    private boolean isLocationSharingEnabled() {

        SharedPreferences preferences =
                getSharedPreferences(
                        "HavenLocation",
                        MODE_PRIVATE
                );

        return preferences.getBoolean(
                "location_shared",
                false
        );
    }


    // =========================================================
    // GET IMMEDIATE CURRENT LOCATION
    // =========================================================

    private void getImmediateLocation() {

        if (!isLocationSharingEnabled()) {

            Log.d(
                    TAG,
                    "LOCATION SHARING IS OFF - IMMEDIATE LOCATION NOT SENT"
            );

            return;
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {

            Log.e(
                    TAG,
                    "CANNOT GET IMMEDIATE LOCATION: PERMISSION NOT GRANTED"
            );

            return;
        }

        CancellationTokenSource tokenSource =
                new CancellationTokenSource();

        fusedLocationClient
                .getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        tokenSource.getToken()
                )
                .addOnSuccessListener(location -> {

                    if (location == null) {

                        Log.e(
                                TAG,
                                "IMMEDIATE LOCATION IS NULL"
                        );

                        return;
                    }

                    /*
                     * Check again because the user may have
                     * pressed Unshare while GPS was detecting
                     * the current location.
                     */
                    if (!isLocationSharingEnabled()) {

                        Log.d(
                                TAG,
                                "LOCATION WAS UNSHARED BEFORE IMMEDIATE LOCATION UPLOAD"
                        );

                        return;
                    }

                    double latitude =
                            location.getLatitude();

                    double longitude =
                            location.getLongitude();

                    Log.d(
                            TAG,
                            "IMMEDIATE LOCATION: "
                                    + latitude
                                    + ", "
                                    + longitude
                    );

                    updateLocationInSupabase(
                            latitude,
                            longitude
                    );

                })
                .addOnFailureListener(e -> {

                    Log.e(
                            TAG,
                            "FAILED TO GET IMMEDIATE LOCATION",
                            e
                    );
                });
    }


    // =========================================================
    // LOCATION REQUEST
    // =========================================================

    private void createLocationRequest() {

        locationRequest =
                new LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        LOCATION_UPDATE_INTERVAL
                )
                        .setMinUpdateIntervalMillis(
                                LOCATION_UPDATE_INTERVAL
                        )
                        .setWaitForAccurateLocation(
                                false
                        )
                        .build();
    }


    // =========================================================
    // LOCATION CALLBACK
    // =========================================================

    private void createLocationCallback() {

        locationCallback =
                new LocationCallback() {

                    @Override
                    public void onLocationResult(
                            LocationResult locationResult
                    ) {

                        if (locationResult == null) {
                            return;
                        }

                        if (locationResult.getLastLocation() == null) {
                            return;
                        }

                        /*
                         * IMPORTANT:
                         *
                         * Emergency service can remain alive,
                         * but when the user presses Unshare,
                         * we stop sending new coordinates.
                         *
                         * Supabase therefore keeps the last
                         * successfully shared location.
                         */
                        if (!isLocationSharingEnabled()) {

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

                        double latitude =
                                locationResult
                                        .getLastLocation()
                                        .getLatitude();

                        double longitude =
                                locationResult
                                        .getLastLocation()
                                        .getLongitude();

                        Log.d(
                                TAG,
                                "LIVE LOCATION: "
                                        + latitude
                                        + ", "
                                        + longitude
                        );

                        updateLocationInSupabase(
                                latitude,
                                longitude
                        );
                    }
                };
    }


    // =========================================================
    // START LOCATION UPDATES
    // =========================================================

    private void startLocationUpdates() {

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
                    "LOCATION PERMISSION NOT GRANTED"
            );

            stopSelf();

            return;
        }

        fusedLocationClient
                .requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        getMainLooper()
                )
                .addOnSuccessListener(unused -> {

                    Log.d(
                            TAG,
                            "LIVE LOCATION TRACKING STARTED"
                    );

                })
                .addOnFailureListener(e -> {

                    Log.e(
                            TAG,
                            "FAILED TO START LOCATION TRACKING",
                            e
                    );
                });
    }


    // =========================================================
    // UPDATE SUPABASE
    // =========================================================

    private void updateLocationInSupabase(
            double latitude,
            double longitude
    ) {

        /*
         * Final safety check before network upload.
         */
        if (!isLocationSharingEnabled()) {

            Log.d(
                    TAG,
                    "LOCATION SHARING OFF - SUPABASE UPDATE SKIPPED"
            );

            return;
        }

        String userId =
                getLoggedInUserId();

        if (userId.isEmpty()) {

            Log.e(
                    TAG,
                    "USER ID IS EMPTY"
            );

            return;
        }

        Map<String, Object> locationData =
                new HashMap<>();

        locationData.put(
                "latitude",
                latitude
        );

        locationData.put(
                "longitude",
                longitude
        );

        /*
         * IMPORTANT:
         *
         * DO NOT set is_shared = true here.
         *
         * is_shared is controlled by the user's
         * Share / Unshare action in HomeActivity.
         */
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
                                            "LIVE LOCATION UPDATED"
                                    );

                                } else {

                                    Log.e(
                                            TAG,
                                            "SUPABASE LOCATION UPDATE FAILED: "
                                                    + response.code()
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
                                        "NETWORK ERROR WHILE UPDATING LOCATION",
                                        t
                                );
                            }
                        }
                );
    }


    // =========================================================
    // GET LOGGED-IN USER ID
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

        if (userId == null
                || userId.trim().isEmpty()) {

            userId =
                    preferences.getString(
                            "user_id",
                            ""
                    );
        }

        if (userId == null
                || userId.trim().isEmpty()) {

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
    // NOTIFICATION CHANNEL
    // =========================================================

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Emergency Location Tracking",
                            NotificationManager.IMPORTANCE_LOW
                    );

            channel.setDescription(
                    "Keeps HavenApp emergency location tracking active."
            );

            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class
                    );

            if (manager != null) {

                manager.createNotificationChannel(
                        channel
                );
            }
        }
    }


    // =========================================================
    // FOREGROUND NOTIFICATION
    // =========================================================

    private Notification createNotification() {

        return new NotificationCompat.Builder(
                this,
                CHANNEL_ID
        )
                .setContentTitle(
                        "HavenApp Emergency Active"
                )
                .setContentText(
                        "Emergency protection is active."
                )
                .setSmallIcon(
                        R.drawable.ic_launcher_foreground
                )
                .setOngoing(true)
                .setPriority(
                        NotificationCompat.PRIORITY_LOW
                )
                .build();
    }


    // =========================================================
    // STOP SERVICE
    // =========================================================

    @Override
    public void onDestroy() {

        Log.d(
                TAG,
                "EmergencyLocationService STOPPED"
        );

        if (fusedLocationClient != null
                && locationCallback != null) {

            fusedLocationClient
                    .removeLocationUpdates(
                            locationCallback
                    );
        }

        super.onDestroy();
    }


    // =========================================================
    // BINDING
    // =========================================================

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }
}