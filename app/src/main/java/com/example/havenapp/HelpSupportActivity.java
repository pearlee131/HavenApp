package com.example.havenapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class HelpSupportActivity extends AppCompatActivity {

    private View btnExit;
    private View cardHopeAssistant;
    private View cardUsingHavenApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_help_support);

        initializeViews();
        setupButtons();
        setupHopeAssistant();
    }

    private void initializeViews() {

        btnExit =
                findViewById(
                        R.id.btnHelpSupportExit
                );

        cardHopeAssistant =
                findViewById(
                        R.id.cardHopeAssistantOption
                );

        cardUsingHavenApp =
                findViewById(
                        R.id.cardUsingHavenApp
                );
    }

    private void setupButtons() {

        /*
         * Exit Help & Support
         */
        btnExit.setOnClickListener(v -> finish());


        /*
         * Hope Assistant
         */
        cardHopeAssistant.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            HelpSupportActivity.this,
                            ChatbotActivity.class
                    );

            startActivity(intent);
        });


        /*
         * Using HavenApp
         */
        cardUsingHavenApp.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            HelpSupportActivity.this,
                            UsingHavenAppActivity.class
                    );

            startActivity(intent);
        });
    }

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