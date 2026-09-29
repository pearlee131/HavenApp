package com.example.havenapp;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class PrivacyPolicyActivity extends AppCompatActivity {

    // =========================================================
    // VIEWS
    // =========================================================

    private View btnPrivacyPolicyExit;
    private View cardHopeAssistant;


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        initializeViews();
        setupExitButton();
        setupHopeAssistant();
    }


    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initializeViews() {

        btnPrivacyPolicyExit =
                findViewById(R.id.btnPrivacyPolicyExit);

        cardHopeAssistant =
                findViewById(R.id.cardHopeAssistant);
    }


    // =========================================================
    // EXIT BUTTON
    // =========================================================

    private void setupExitButton() {

        if (btnPrivacyPolicyExit == null) {
            return;
        }

        btnPrivacyPolicyExit.setOnClickListener(v ->
                finish()
        );
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
}