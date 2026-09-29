package com.example.havenapp;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.havenapp.api.PasswordResetAPI;
import com.example.havenapp.api.SupabaseClient;
import com.example.havenapp.api.SupabaseUserAPI;
import com.example.havenapp.model.Profile;
import com.example.havenapp.model.ResetPasswordRequest;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class SetPasswordActivity extends AppCompatActivity {


    private TextInputEditText etNewPassword;
    private TextInputEditText etConfirmPassword;

    private TextInputLayout tilNewPassword;
    private TextInputLayout tilConfirmPassword;

    private MaterialButton btnResetPassword;

    private LinearLayout progressLayout;


    private PasswordResetAPI passwordResetAPI;
    private SupabaseUserAPI userAPI;


    private String email;
    private String authId;
    private String accessToken;


    private boolean fromFirstLogin;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.setpassword_activity);


        email = getIntent()
                .getStringExtra("email");


        authId = getIntent()
                .getStringExtra("auth_id");


        fromFirstLogin =
                getIntent()
                        .getBooleanExtra(
                                "from_first_login",
                                false
                        );


        // ===============================
        // GET SAVED USER TOKEN
        // ===============================

        SharedPreferences pref =
                getSharedPreferences(
                        "HavenUser",
                        MODE_PRIVATE
                );


        accessToken =
                pref.getString(
                        "access_token",
                        ""
                );


        Log.d(
                "SET_PASSWORD",
                "AUTH ID = " + authId
        );


        Log.d(
                "SET_PASSWORD",
                "TOKEN EMPTY = " + accessToken.isEmpty()
        );


        passwordResetAPI =
                SupabaseClient
                        .getClient()
                        .create(PasswordResetAPI.class);


        userAPI =
                SupabaseClient
                        .getClient()
                        .create(SupabaseUserAPI.class);


        initializeViews();

        clearErrorsWhenTyping();


        if (fromFirstLogin) {

            progressLayout.setVisibility(
                    LinearLayout.GONE
            );

        }


        // ===============================
        // RESET PASSWORD BUTTON
        // ===============================

        btnResetPassword.setOnClickListener(v -> {


            String password =
                    etNewPassword
                            .getText()
                            .toString();


            String confirm =
                    etConfirmPassword
                            .getText()
                            .toString();


            clearErrors();


            // ===============================
            // EMPTY PASSWORD
            // ===============================

            if (password.isEmpty()) {

                tilNewPassword.setError(
                        "Please enter a new password."
                );

                return;

            }


            // ===============================
            // PASSWORD REQUIREMENTS
            // ===============================

            String passwordError =
                    getPasswordValidationError(password);


            if (passwordError != null) {

                tilNewPassword.setError(
                        passwordError
                );

                return;

            }


            // ===============================
            // EMPTY CONFIRM PASSWORD
            // ===============================

            if (confirm.isEmpty()) {

                tilConfirmPassword.setError(
                        "Please confirm your password."
                );

                return;

            }


            // ===============================
            // PASSWORD MATCH
            // ===============================

            if (!password.equals(confirm)) {

                tilConfirmPassword.setError(
                        "Passwords do not match."
                );

                return;

            }


            resetPassword(password);


        });


    }


    // ===============================
    // INITIALIZE VIEWS
    // ===============================

    private void initializeViews() {


        etNewPassword =
                findViewById(R.id.etNewPassword);


        etConfirmPassword =
                findViewById(R.id.etConfirmPassword);


        tilNewPassword =
                findViewById(R.id.tilNewPassword);


        tilConfirmPassword =
                findViewById(R.id.tilConfirmPassword);


        btnResetPassword =
                findViewById(R.id.btnResetPassword);


        progressLayout =
                findViewById(R.id.progressLayout);


    }


    // ===============================
    // PASSWORD VALIDATION
    // ===============================

    private String getPasswordValidationError(
            String password
    ) {


        if (password.length() < 8) {

            return "Password must be at least 8 characters.";

        }


        if (password.contains(" ")) {

            return "Password must not contain spaces.";

        }


        if (!password.matches(".*[A-Z].*")) {

            return "Password must contain at least one uppercase letter.";

        }


        if (!password.matches(".*[a-z].*")) {

            return "Password must contain at least one lowercase letter.";

        }


        if (!password.matches(".*[0-9].*")) {

            return "Password must contain at least one number.";

        }


        if (!password.matches(
                ".*[^a-zA-Z0-9\\s].*"
        )) {

            return "Password must contain at least one special character.";

        }


        return null;

    }


    // ===============================
    // RESET PASSWORD
    // ===============================

    private void resetPassword(String password) {


        btnResetPassword.setEnabled(false);


        btnResetPassword.setText(
                "Updating..."
        );


        passwordResetAPI.resetPassword(

                new ResetPasswordRequest(
                        authId,
                        email,
                        password
                )

        ).enqueue(new Callback<Void>() {


            @Override
            public void onResponse(
                    Call<Void> call,
                    Response<Void> response
            ) {


                Log.d(
                        "RESET_PASSWORD",
                        "CODE = " + response.code()
                );


                if (response.isSuccessful()) {


                    passwordUpdated();


                } else {


                    btnResetPassword.setEnabled(true);


                    btnResetPassword.setText(
                            "Reset Password"
                    );


                    String errorMessage =
                            "Password update failed.";


                    if (response.errorBody() != null) {

                        try {

                            String errorBody =
                                    response.errorBody()
                                            .string();

                            Log.e(
                                    "RESET_PASSWORD_ERROR",
                                    errorBody
                            );

                        } catch (Exception e) {

                            Log.e(
                                    "RESET_PASSWORD_ERROR",
                                    "Could not read error response",
                                    e
                            );

                        }

                    }


                    tilNewPassword.setError(
                            errorMessage
                    );


                }


            }


            @Override
            public void onFailure(
                    Call<Void> call,
                    Throwable t
            ) {


                Log.e(
                        "RESET_PASSWORD",
                        t.getMessage(),
                        t
                );


                btnResetPassword.setEnabled(true);


                btnResetPassword.setText(
                        "Reset Password"
                );


                tilNewPassword.setError(
                        "Connection error."
                );


            }


        });


    }


    // ===============================
    // PASSWORD UPDATED
    // ===============================

    private void passwordUpdated() {


        Log.d(
                "FIRST_LOGIN_CHECK",
                "fromFirstLogin = " + fromFirstLogin
        );


        if (fromFirstLogin) {

            updateFirstLoginStatus();

        } else {

            openPasswordUpdated();

        }

    }


    // ===============================
    // UPDATE FIRST LOGIN STATUS
    // ===============================

    private void updateFirstLoginStatus() {


        Log.d(
                "FIRST_LOGIN_TEST",
                "updateFirstLoginStatus CALLED"
        );


        if (authId == null || authId.isEmpty()) {


            Log.e(
                    "FIRST_LOGIN",
                    "AUTH ID EMPTY"
            );


            openPasswordUpdated();

            return;

        }


        Map<String, Boolean> body =
                new HashMap<>();


        body.put(
                "first_login",
                false
        );


        Log.d(
                "FIRST_LOGIN",
                "Updating auth_id = " + authId
        );


        userAPI.updateFirstLogin(
                        "Bearer " + accessToken,
                        "eq." + authId,
                        body
                )

                .enqueue(new Callback<List<Profile>>() {


                    @Override
                    public void onResponse(
                            Call<List<Profile>> call,
                            Response<List<Profile>> response
                    ) {


                        Log.d(
                                "FIRST_LOGIN",
                                "UPDATE CODE = "
                                        + response.code()
                        );


                        if (response.errorBody() != null) {

                            try {

                                Log.e(
                                        "FIRST_LOGIN_ERROR",
                                        response.errorBody().string()
                                );

                            } catch (Exception e) {

                                e.printStackTrace();

                            }

                        }


                        if (response.body() != null
                                && !response.body().isEmpty()) {


                            Log.d(
                                    "FIRST_LOGIN",
                                    "NEW VALUE = "
                                            +
                                            response.body()
                                                    .get(0)
                                                    .isFirstLogin()
                            );


                        } else {


                            Log.e(
                                    "FIRST_LOGIN",
                                    "NO ROW UPDATED"
                            );


                        }


                        openPasswordUpdated();


                    }


                    @Override
                    public void onFailure(
                            Call<List<Profile>> call,
                            Throwable t
                    ) {


                        Log.e(
                                "FIRST_LOGIN",
                                t.getMessage(),
                                t
                        );


                        openPasswordUpdated();


                    }


                });


    }


    // ===============================
    // OPEN PASSWORD UPDATED
    // ===============================

    private void openPasswordUpdated() {


        Intent intent =
                new Intent(
                        SetPasswordActivity.this,
                        PasswordUpdatedActivity.class
                );


        intent.putExtra(
                "from_first_login",
                fromFirstLogin
        );


        startActivity(intent);


        finish();


    }


    // ===============================
    // CLEAR ERRORS WHEN TYPING
    // ===============================

    private void clearErrorsWhenTyping() {


        etNewPassword.addTextChangedListener(
                new TextWatcher() {


                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }


                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {


                        tilNewPassword.setError(null);


                    }


                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }


                });


        etConfirmPassword.addTextChangedListener(
                new TextWatcher() {


                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }


                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {


                        tilConfirmPassword.setError(null);


                    }


                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }


                });


    }


    // ===============================
    // CLEAR ERRORS
    // ===============================

    private void clearErrors() {


        tilNewPassword.setError(null);

        tilConfirmPassword.setError(null);


    }


}