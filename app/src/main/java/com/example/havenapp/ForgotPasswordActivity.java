package com.example.havenapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.havenapp.api.PasswordResetAPI;
import com.example.havenapp.api.SupabaseClient;
import com.example.havenapp.model.EmailRequest;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class ForgotPasswordActivity extends AppCompatActivity {

    private ImageView btnBack;

    private MaterialButton btnSend;

    private TextInputEditText etEmail;

    private TextInputLayout tilEmail;

    private PasswordResetAPI passwordResetAPI;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.forgotpassword_activity);


        btnBack = findViewById(R.id.btnBack);

        btnSend = findViewById(R.id.btnSend);

        etEmail = findViewById(R.id.etEmail);

        tilEmail = findViewById(R.id.tilEmail);


        passwordResetAPI =
                SupabaseClient
                        .getClient()
                        .create(PasswordResetAPI.class);


        btnBack.setOnClickListener(v -> finish());


        etEmail.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after) {}


            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count) {

                tilEmail.setError(null);

            }


            @Override
            public void afterTextChanged(
                    Editable s) {}

        });


        btnSend.setOnClickListener(v -> {


            String email =
                    etEmail.getText()
                            .toString()
                            .trim()
                            .toLowerCase();


            tilEmail.setError(null);


            if(email.isEmpty()){

                tilEmail.setError(
                        "Please enter your email"
                );

                return;
            }


            if(!isValidEmail(email)){


                tilEmail.setError(
                        "Please enter a valid email address"
                );

                return;
            }


            sendOTP(email);


        });


    }


    private boolean isValidEmail(String email){

        return android.util.Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches();

    }




    private void sendOTP(String email){


        btnSend.setEnabled(false);

        btnSend.setText("Sending...");


        // =========================
        // DEBUG LOGGING ONLY
        // =========================

        Log.d(
                "RESET_DEBUG",
                "Sending reset OTP for: " + email
        );


        passwordResetAPI
                .sendResetOTP(
                        new EmailRequest(email)
                )

                .enqueue(new Callback<Void>() {


                    @Override
                    public void onResponse(
                            Call<Void> call,
                            Response<Void> response) {


                        // =========================
                        // DEBUG LOGGING ONLY
                        // =========================

                        Log.d(
                                "RESET_DEBUG",
                                "Response code: "
                                        + response.code()
                        );

                        Log.d(
                                "RESET_DEBUG",
                                "Response successful: "
                                        + response.isSuccessful()
                        );


                        if(response.errorBody() != null){

                            try {

                                String errorBody =
                                        response.errorBody()
                                                .string();

                                Log.e(
                                        "RESET_DEBUG",
                                        "Server error: "
                                                + errorBody
                                );

                            } catch (Exception e) {

                                Log.e(
                                        "RESET_DEBUG",
                                        "Could not read server error",
                                        e
                                );

                            }

                        }


                        btnSend.setEnabled(true);

                        btnSend.setText("Send");


                        if(response.isSuccessful()){


                            Intent intent =
                                    new Intent(
                                            ForgotPasswordActivity.this,
                                            CheckYourEmailActivity.class
                                    );


                            intent.putExtra(
                                    "email",
                                    email
                            );


                            startActivity(intent);


                        }

                        else {


                            tilEmail.setError(
                                    "Email address not found. Please check and try again."
                            );


                        }


                    }


                    @Override
                    public void onFailure(
                            Call<Void> call,
                            Throwable t) {


                        Log.e(
                                "RESET_DEBUG",
                                "Connection error: "
                                        + t.getMessage(),
                                t
                        );


                        btnSend.setEnabled(true);

                        btnSend.setText("Send");


                        tilEmail.setError(
                                "Connection error"
                        );


                    }


                });


    }

}
