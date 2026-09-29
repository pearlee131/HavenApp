package com.example.havenapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class ChatbotFloatingHelper {

    // =========================================================
    // DRAG SETTINGS
    // =========================================================

    private static final float CLICK_THRESHOLD = 10f;


    // =========================================================
    // SHARED POSITION STORAGE
    // =========================================================

    private static final String PREF_NAME =
            "HavenChatbotPosition";

    private static final String KEY_X_PERCENT =
            "chatbot_x_percent";

    private static final String KEY_Y_PERCENT =
            "chatbot_y_percent";

    private static final String KEY_HAS_POSITION =
            "chatbot_has_position";


    // =========================================================
    // SETUP FLOATING CHATBOT
    // =========================================================

    public static void setup(
            Activity activity,
            View chatbot
    ) {

        final float[] downRawX = new float[1];
        final float[] downRawY = new float[1];

        final float[] startX = new float[1];
        final float[] startY = new float[1];

        final boolean[] dragging = new boolean[1];


        // =====================================================
        // RESTORE SAVED POSITION
        // =====================================================

        chatbot.post(() -> {

            restorePosition(
                    activity,
                    chatbot
            );

        });


        // =====================================================
        // DRAG / TAP LISTENER
        // =====================================================

        chatbot.setOnTouchListener(
                (view, event) -> {

                    ViewGroup parent =
                            (ViewGroup) view.getParent();


                    switch (event.getActionMasked()) {


                        // =========================================
                        // TOUCH START
                        // =========================================

                        case MotionEvent.ACTION_DOWN:

                            downRawX[0] =
                                    event.getRawX();

                            downRawY[0] =
                                    event.getRawY();


                            startX[0] =
                                    view.getX();

                            startY[0] =
                                    view.getY();


                            dragging[0] = false;


                            // Bring Hope above other views
                            view.bringToFront();

                            return true;


                        // =========================================
                        // DRAGGING
                        // =========================================

                        case MotionEvent.ACTION_MOVE:

                            float deltaX =
                                    event.getRawX()
                                            - downRawX[0];

                            float deltaY =
                                    event.getRawY()
                                            - downRawY[0];


                            if (Math.abs(deltaX)
                                    > CLICK_THRESHOLD
                                    ||
                                    Math.abs(deltaY)
                                            > CLICK_THRESHOLD) {

                                dragging[0] = true;
                            }


                            float newX =
                                    startX[0]
                                            + deltaX;

                            float newY =
                                    startY[0]
                                            + deltaY;


                            float maxX =
                                    parent.getWidth()
                                            - view.getWidth();

                            float maxY =
                                    parent.getHeight()
                                            - view.getHeight();


                            // Prevent negative limits
                            maxX =
                                    Math.max(
                                            0,
                                            maxX
                                    );

                            maxY =
                                    Math.max(
                                            0,
                                            maxY
                                    );


                            // Keep chatbot inside screen
                            newX =
                                    Math.max(
                                            0,
                                            Math.min(
                                                    newX,
                                                    maxX
                                            )
                                    );

                            newY =
                                    Math.max(
                                            0,
                                            Math.min(
                                                    newY,
                                                    maxY
                                            )
                                    );


                            view.setX(newX);
                            view.setY(newY);

                            return true;


                        // =========================================
                        // TOUCH RELEASE
                        // =========================================

                        case MotionEvent.ACTION_UP:

                            if (dragging[0]) {

                                // Save the newest position
                                savePosition(
                                        activity,
                                        view
                                );

                            } else {

                                // Normal tap = open Hope
                                openChatbot(activity);
                            }


                            view.performClick();

                            return true;


                        // =========================================
                        // TOUCH CANCELLED
                        // =========================================

                        case MotionEvent.ACTION_CANCEL:

                            if (dragging[0]) {

                                savePosition(
                                        activity,
                                        view
                                );
                            }

                            return true;
                    }


                    return false;
                }
        );
    }


    // =========================================================
    // SAVE POSITION
    // =========================================================

    private static void savePosition(
            Activity activity,
            View chatbot
    ) {

        if (!(chatbot.getParent()
                instanceof ViewGroup)) {

            return;
        }


        ViewGroup parent =
                (ViewGroup) chatbot.getParent();


        float maxX =
                parent.getWidth()
                        - chatbot.getWidth();

        float maxY =
                parent.getHeight()
                        - chatbot.getHeight();


        if (maxX <= 0
                || maxY <= 0) {

            return;
        }


        // Convert current position into percentages.
        // Example:
        // X = 0.80 means 80% across the screen.
        // Y = 0.50 means halfway down the screen.

        float xPercent =
                chatbot.getX()
                        / maxX;

        float yPercent =
                chatbot.getY()
                        / maxY;


        // Safety clamp between 0 and 1

        xPercent =
                Math.max(
                        0f,
                        Math.min(
                                xPercent,
                                1f
                        )
                );

        yPercent =
                Math.max(
                        0f,
                        Math.min(
                                yPercent,
                                1f
                        )
                );


        SharedPreferences preferences =
                activity.getSharedPreferences(
                        PREF_NAME,
                        Activity.MODE_PRIVATE
                );


        preferences.edit()

                .putFloat(
                        KEY_X_PERCENT,
                        xPercent
                )

                .putFloat(
                        KEY_Y_PERCENT,
                        yPercent
                )

                .putBoolean(
                        KEY_HAS_POSITION,
                        true
                )

                .apply();
    }


    // =========================================================
    // RESTORE POSITION
    // =========================================================

    private static void restorePosition(
            Activity activity,
            View chatbot
    ) {

        if (!(chatbot.getParent()
                instanceof ViewGroup)) {

            return;
        }


        SharedPreferences preferences =
                activity.getSharedPreferences(
                        PREF_NAME,
                        Activity.MODE_PRIVATE
                );


        boolean hasSavedPosition =
                preferences.getBoolean(
                        KEY_HAS_POSITION,
                        false
                );


        // First time using Hope:
        // keep the XML default position.

        if (!hasSavedPosition) {

            return;
        }


        float xPercent =
                preferences.getFloat(
                        KEY_X_PERCENT,
                        1f
                );

        float yPercent =
                preferences.getFloat(
                        KEY_Y_PERCENT,
                        0.75f
                );


        ViewGroup parent =
                (ViewGroup) chatbot.getParent();


        float maxX =
                parent.getWidth()
                        - chatbot.getWidth();

        float maxY =
                parent.getHeight()
                        - chatbot.getHeight();


        if (maxX <= 0
                || maxY <= 0) {

            return;
        }


        float restoredX =
                xPercent
                        * maxX;

        float restoredY =
                yPercent
                        * maxY;


        // Keep restored position safely inside screen

        restoredX =
                Math.max(
                        0,
                        Math.min(
                                restoredX,
                                maxX
                        )
                );

        restoredY =
                Math.max(
                        0,
                        Math.min(
                                restoredY,
                                maxY
                        )
                );


        chatbot.setX(restoredX);
        chatbot.setY(restoredY);

        chatbot.bringToFront();
    }


    // =========================================================
    // OPEN CHATBOT
    // =========================================================

    private static void openChatbot(
            Activity activity
    ) {

        Intent intent =
                new Intent(
                        activity,
                        ChatbotActivity.class
                );

        activity.startActivity(intent);
    }
}