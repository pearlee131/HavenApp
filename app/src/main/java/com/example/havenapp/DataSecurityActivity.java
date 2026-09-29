package com.example.havenapp;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class DataSecurityActivity extends AppCompatActivity {

    // =========================================================
    // VIEWS
    // =========================================================

    private View btnDataSecurityExit;
    private View cardHopeAssistant;


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_security);

        initializeViews();
        setupExitButton();
        setupHopeAssistant();
    }


    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initializeViews() {

        btnDataSecurityExit =
                findViewById(R.id.btnDataSecurityExit);

        cardHopeAssistant =
                findViewById(R.id.cardHopeAssistant);
    }


    // =========================================================
    // EXIT BUTTON
    // =========================================================

    private void setupExitButton() {

        if (btnDataSecurityExit == null) {
            return;
        }

        btnDataSecurityExit.setOnClickListener(v ->
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