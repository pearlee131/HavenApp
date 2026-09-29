package com.example.havenapp;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class UsingHavenAppActivity extends AppCompatActivity {

    private View btnExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_using_haven_app);

        initializeViews();
        setupButtons();
        setupHopeAssistant();
    }

    // =====================================================
    // INITIALIZE VIEWS
    // =====================================================

    private void initializeViews() {

        btnExit =
                findViewById(
                        R.id.btnUsingHavenAppExit
                );
    }

    // =====================================================
    // BUTTONS
    // =====================================================

    private void setupButtons() {

        btnExit.setOnClickListener(v -> finish());
    }

    // =====================================================
    // FLOATING HOPE ASSISTANT
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
}