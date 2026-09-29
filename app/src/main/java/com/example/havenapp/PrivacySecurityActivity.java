package com.example.havenapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

public class PrivacySecurityActivity extends AppCompatActivity {

    // =========================================================
    // VIEWS
    // =========================================================

    private View btnPrivacyExit;
    private View cardHopeAssistant;

    private MaterialCardView cardLocationPermissions;
    private MaterialCardView cardDataSecurity;
    private MaterialCardView cardPrivacyPolicy;


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_security);

        initializeViews();

        setupExitButton();
        setupHopeAssistant();

        setupLocationPermissions();
        setupDataSecurity();
        setupPrivacyPolicy();
    }


    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initializeViews() {

        btnPrivacyExit =
                findViewById(R.id.btnPrivacyExit);

        cardHopeAssistant =
                findViewById(R.id.cardHopeAssistant);

        cardLocationPermissions =
                findViewById(R.id.cardLocationPermissions);

        cardDataSecurity =
                findViewById(R.id.cardDataSecurity);

        cardPrivacyPolicy =
                findViewById(R.id.cardPrivacyPolicy);
    }


    // =========================================================
    // EXIT
    // =========================================================

    private void setupExitButton() {

        if (btnPrivacyExit == null) {
            return;
        }

        btnPrivacyExit.setOnClickListener(v -> finish());
    }


    // =========================================================
    // HOPE ASSISTANT
    // =========================================================

    private void setupHopeAssistant() {

        if (cardHopeAssistant == null) {
            return;
        }

        ChatbotFloatingHelper.setup(
                this,
                cardHopeAssistant
        );
    }


    // =========================================================
    // LOCATION & PERMISSIONS
    // =========================================================

    private void setupLocationPermissions() {

        if (cardLocationPermissions == null) {
            return;
        }

        cardLocationPermissions.setOnClickListener(v ->
                openAppSettings()
        );
    }


    /**
     * Opens Android's App Info screen specifically for HavenApp.
     *
     * From here the user can manage:
     * - Location permission
     * - Phone permission
     * - Other HavenApp permissions
     */
    private void openAppSettings() {

        try {

            Intent intent =
                    new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    );

            Uri packageUri =
                    Uri.parse(
                            "package:" + getPackageName()
                    );

            intent.setData(packageUri);

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Unable to open app settings.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }


    // =========================================================
    // DATA & SECURITY
    // =========================================================

    private void setupDataSecurity() {

        if (cardDataSecurity == null) {
            return;
        }

        cardDataSecurity.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            PrivacySecurityActivity.this,
                            DataSecurityActivity.class
                    );

            startActivity(intent);
        });
    }

    // =========================================================
    // PRIVACY POLICY
    // =========================================================

    private void setupPrivacyPolicy() {

        if (cardPrivacyPolicy == null) {
            return;
        }

        cardPrivacyPolicy.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            PrivacySecurityActivity.this,
                            PrivacyPolicyActivity.class
                    );

            startActivity(intent);
        });
    }
}