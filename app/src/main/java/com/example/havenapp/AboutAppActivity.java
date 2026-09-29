package com.example.havenapp;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class AboutAppActivity extends AppCompatActivity {

    // =========================================================
    // VIEWS
    // =========================================================

    private View btnAboutAppExit;
    private View cardHopeAssistant;


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_app);

        initializeViews();
        setupExitButton();
        setupHopeAssistant();
    }


    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initializeViews() {

        btnAboutAppExit =
                findViewById(R.id.btnAboutAppExit);

        cardHopeAssistant =
                findViewById(R.id.cardHopeAssistant);
    }


    // =========================================================
    // EXIT BUTTON
    // =========================================================

    private void setupExitButton() {

        if (btnAboutAppExit == null) {
            return;
        }

        btnAboutAppExit.setOnClickListener(v -> finish());
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