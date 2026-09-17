package com.example.havenapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.havenapp.api.HopeChatAPI;
import com.example.havenapp.api.SupabaseClient;
import com.example.havenapp.model.HopeChatRequest;
import com.example.havenapp.model.HopeChatResponse;
import com.example.havenapp.model.HopeResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HopeConnectionTestActivity extends AppCompatActivity {

    private static final String TAG = "HOPE_TEST";

    private HopeChatAPI hopeChatAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // No layout is needed for this temporary connection test.

        hopeChatAPI =
                SupabaseClient
                        .getClient()
                        .create(HopeChatAPI.class);

        testHopeConnection();
    }


    // =====================================================
    // TEST HOPE CONNECTION
    // =====================================================

    private void testHopeConnection() {

        // -------------------------------------------------
        // GET SAVED LOGIN SESSION
        // -------------------------------------------------

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
        // TOKEN CHECK
        // -------------------------------------------------

        if (accessToken == null
                || accessToken.trim().isEmpty()) {

            Log.e(
                    TAG,
                    "Hope test failed: access token is missing"
            );

            Toast.makeText(
                    this,
                    "Hope test: login session not found.",
                    Toast.LENGTH_LONG
            ).show();

            finish();

            return;
        }


        /*
         * IMPORTANT:
         *
         * We intentionally DO NOT print the access token
         * anywhere in Logcat.
         */

        Log.d(
                TAG,
                "Starting Hope connection test..."
        );


        // -------------------------------------------------
        // TEST QUESTION
        // -------------------------------------------------

        HopeChatRequest request =
                new HopeChatRequest(
                        "What is RA 9262?"
                );


        // -------------------------------------------------
        // CALL HOPE EDGE FUNCTION
        // -------------------------------------------------

        hopeChatAPI
                .askHope(
                        "Bearer " + accessToken,
                        request
                )
                .enqueue(
                        new Callback<HopeChatResponse>() {

                            @Override
                            public void onResponse(
                                    Call<HopeChatResponse> call,
                                    Response<HopeChatResponse> response
                            ) {

                                if (isFinishing()
                                        || isDestroyed()) {

                                    return;
                                }


                                // ---------------------------------
                                // HTTP ERROR
                                // ---------------------------------

                                if (!response.isSuccessful()) {

                                    Log.e(
                                            TAG,
                                            "Hope HTTP error: "
                                                    + response.code()
                                    );

                                    Toast.makeText(
                                            HopeConnectionTestActivity.this,
                                            "Hope HTTP error: "
                                                    + response.code(),
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }


                                // ---------------------------------
                                // RESPONSE BODY
                                // ---------------------------------

                                HopeChatResponse body =
                                        response.body();

                                if (body == null) {

                                    Log.e(
                                            TAG,
                                            "Hope response body is null"
                                    );

                                    Toast.makeText(
                                            HopeConnectionTestActivity.this,
                                            "Hope returned an empty response.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }


                                // ---------------------------------
                                // EDGE FUNCTION ERROR
                                // ---------------------------------

                                if (!body.isSuccess()) {

                                    String error =
                                            body.getError();

                                    Log.e(
                                            TAG,
                                            "Hope API error: "
                                                    + error
                                    );

                                    Toast.makeText(
                                            HopeConnectionTestActivity.this,
                                            error != null
                                                    ? error
                                                    : "Hope request failed.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }


                                // ---------------------------------
                                // HOPE RESPONSE
                                // ---------------------------------

                                HopeResponse hopeResponse =
                                        body.getResponse();

                                if (hopeResponse == null) {

                                    Log.e(
                                            TAG,
                                            "Hope inner response is null"
                                    );

                                    Toast.makeText(
                                            HopeConnectionTestActivity.this,
                                            "Hope response is unavailable.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }


                                // ---------------------------------
                                // SUCCESS
                                // ---------------------------------

                                Log.d(
                                        TAG,
                                        "HOPE CONNECTION SUCCESS"
                                );

                                Log.d(
                                        TAG,
                                        "Topic: "
                                                + hopeResponse.getTopicKey()
                                );

                                Log.d(
                                        TAG,
                                        "Action: "
                                                + hopeResponse.getActionType()
                                );

                                Log.d(
                                        TAG,
                                        "Title: "
                                                + hopeResponse.getTitle()
                                );


                                /*
                                 * Do NOT log hopeResponse.getMessage().
                                 *
                                 * Once Hope accepts real user questions,
                                 * conversations may contain sensitive
                                 * information.
                                 */


                                Toast.makeText(
                                        HopeConnectionTestActivity.this,
                                        "Hope connected successfully!\n"
                                                + hopeResponse.getTitle(),
                                        Toast.LENGTH_LONG
                                ).show();
                            }


                            @Override
                            public void onFailure(
                                    Call<HopeChatResponse> call,
                                    Throwable t
                            ) {

                                if (isFinishing()
                                        || isDestroyed()) {

                                    return;
                                }

                                Log.e(
                                        TAG,
                                        "Hope network failure",
                                        t
                                );

                                Toast.makeText(
                                        HopeConnectionTestActivity.this,
                                        "Hope connection failed: "
                                                + (
                                                t.getMessage() != null
                                                        ? t.getMessage()
                                                        : "Network error"
                                        ),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }
}