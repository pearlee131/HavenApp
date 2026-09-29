package com.example.havenapp;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.havenapp.api.SupabaseAuthAPI;
import com.example.havenapp.api.SupabaseClient;
import com.example.havenapp.api.SupabaseUserAPI;

import com.example.havenapp.model.AuthResponse;
import com.example.havenapp.model.LoginRequest;
import com.example.havenapp.model.Profile;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilPhone;
    private TextInputLayout tilPassword;

    private TextInputEditText etPhone;
    private TextInputEditText etPassword;

    private TextView txtForgotPassword;

    private ImageView imgLogo;

    private MaterialButton btnLogin;

    private SupabaseAuthAPI authAPI;
    private SupabaseUserAPI userAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.login_activity);

        authAPI =
                SupabaseClient
                        .getClient()
                        .create(SupabaseAuthAPI.class);

        userAPI =
                SupabaseClient
                        .getClient()
                        .create(SupabaseUserAPI.class);

        initViews();

        setupListeners();
    }

    private void initViews() {

        tilPhone =
                findViewById(R.id.tilPhone);

        tilPassword =
                findViewById(R.id.tilPassword);

        etPhone =
                findViewById(R.id.etPhone);

        etPassword =
                findViewById(R.id.etPassword);

        btnLogin =
                findViewById(R.id.btnLogin);

        txtForgotPassword =
                findViewById(R.id.txtForgotPassword);

        imgLogo =
                findViewById(R.id.imgLogo);

        txtForgotPassword.setPaintFlags(
                txtForgotPassword.getPaintFlags()
                        | Paint.UNDERLINE_TEXT_FLAG
        );
    }

    private void setupListeners() {

        btnLogin.setOnClickListener(v -> loginUser());

        txtForgotPassword.setOnClickListener(v -> {

            startActivity(
                    new Intent(
                            LoginActivity.this,
                            ForgotPasswordActivity.class
                    )
            );
        });

        etPhone.addTextChangedListener(new TextWatcher() {

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

                tilPhone.setError(null);
            }

            @Override
            public void afterTextChanged(
                    Editable s
            ) {
            }
        });

        etPassword.addTextChangedListener(new TextWatcher() {

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

                tilPassword.setError(null);
            }

            @Override
            public void afterTextChanged(
                    Editable s
            ) {
            }
        });

        etPassword.setOnFocusChangeListener((v, focus) -> {

            if (focus) {

                imgLogo.animate()
                        .scaleX(.90f)
                        .scaleY(.90f)
                        .setDuration(250);

            } else {

                imgLogo.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(250);
            }
        });
    }

    private void loginUser() {

        Log.d(
                "LOGIN_DEBUG",
                "LOGIN BUTTON PRESSED"
        );

        String email =
                etPhone.getText()
                        .toString()
                        .trim()
                        .toLowerCase();

        String password =
                etPassword.getText()
                        .toString();

        tilPhone.setError(null);
        tilPassword.setError(null);

        if (email.isEmpty()) {

            tilPhone.setError(
                    "Please enter your email"
            );

            return;
        }

        if (password.isEmpty()) {

            tilPassword.setError(
                    "Please enter your password"
            );

            return;
        }

        loginWithSupabase(
                email,
                password
        );
    }

    private void loginWithSupabase(
            String email,
            String password
    ) {

        Log.d(
                "LOGIN_DEBUG",
                "SENDING LOGIN REQUEST FOR: " + email
        );

        btnLogin.setEnabled(false);

        btnLogin.setText("Logging in...");

        authAPI.login(
                "password",
                new LoginRequest(
                        email,
                        password
                )
        ).enqueue(new Callback<AuthResponse>() {

            @Override
            public void onResponse(
                    Call<AuthResponse> call,
                    Response<AuthResponse> response
            ) {

                Log.d(
                        "LOGIN_DEBUG",
                        "LOGIN RESPONSE RECEIVED: " +
                                response.code()
                );

                if (response.errorBody() != null) {

                    try {

                        Log.e(
                                "LOGIN_DEBUG",
                                "LOGIN ERROR BODY: " +
                                        response.errorBody().string()
                        );

                    } catch (Exception e) {

                        Log.e(
                                "LOGIN_DEBUG",
                                "Could not read error body",
                                e
                        );
                    }
                }

                if (response.isSuccessful()
                        && response.body() != null) {

                    Log.d(
                            "LOGIN_DEBUG",
                            "AUTH SUCCESS - ACCESS TOKEN RECEIVED"
                    );

                    AuthResponse auth =
                            response.body();

                    if (auth.getAccess_token() == null
                            || auth.getAccess_token().trim().isEmpty()) {

                        Log.e(
                                "LOGIN_DEBUG",
                                "ACCESS TOKEN IS EMPTY"
                        );

                        Toast.makeText(
                                LoginActivity.this,
                                "Login session could not be created.",
                                Toast.LENGTH_LONG
                        ).show();

                        resetButton();

                        return;
                    }

                    if (auth.getRefresh_token() == null
                            || auth.getRefresh_token().trim().isEmpty()) {

                        Log.e(
                                "LOGIN_DEBUG",
                                "REFRESH TOKEN IS EMPTY"
                        );
                    } else {

                        Log.d(
                                "LOGIN_DEBUG",
                                "REFRESH TOKEN RECEIVED"
                        );
                    }

                    Log.d(
                            "LOGIN_DEBUG",
                            "CALLING getUserProfile()"
                    );

                    getUserProfile(




                            "Bearer " + auth.getAccess_token(),
                            auth.getAccess_token(),
                            auth.getRefresh_token(),
                            auth.getUser().getId(),
                            email
                    );

                } else {

                    tilPassword.setError(
                            "Invalid email or password"
                    );

                    resetButton();
                }
            }

            @Override
            public void onFailure(
                    Call<AuthResponse> call,
                    Throwable t
            ) {

                Log.e(
                        "LOGIN_DEBUG",
                        "LOGIN NETWORK FAILURE",
                        t
                );

                Toast.makeText(
                        LoginActivity.this,
                        "Connection error",
                        Toast.LENGTH_SHORT
                ).show();

                resetButton();
            }
        });
    }

    private void getUserProfile(
            String token,
            String accessToken,
            String refreshToken,
            String userId,
            String email
    ) {

        Log.d(
                "PROFILE_DEBUG",
                "Searching email = " + email
        );

        userAPI.getProfileByEmail(
                        token,
                        "eq." + email
                )
                .enqueue(new Callback<List<Profile>>() {

                    @Override
                    public void onResponse(
                            Call<List<Profile>> call,
                            Response<List<Profile>> response
                    ) {

                        Log.d(
                                "PROFILE_DEBUG",
                                "CODE = " + response.code()
                        );

                        Log.d(
                                "PROFILE_DEBUG",
                                "BODY = " + response.body()
                        );

                        if (response.isSuccessful()
                                && response.body() != null
                                && !response.body().isEmpty()) {

                            Profile profile =
                                    response.body().get(0);

                            // ACCOUNT STATUS CHECK
                            if (profile.getStatus() == null
                                    || !profile.getStatus()
                                    .equalsIgnoreCase("active")) {

                                showSuspendedDialog();

                                resetButton();

                                return;
                            }

                            // SAVE USER
                            saveUser(
                                    userId,
                                    email,
                                    profile,
                                    accessToken,
                                    refreshToken
                            );

                            Intent intent;

                            // FIRST LOGIN CHECK
                            if (profile.isFirstLogin()) {

                                intent =
                                        new Intent(
                                                LoginActivity.this,
                                                ChangeNewPasswordActivity.class
                                        );

                            } else {

                                intent =
                                        new Intent(
                                                LoginActivity.this,
                                                HomeActivity.class
                                        );
                            }

                            intent.setFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK |
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                            );

                            startActivity(intent);

                            finish();

                        } else {

                            Toast.makeText(
                                    LoginActivity.this,
                                    "Profile not found",
                                    Toast.LENGTH_SHORT
                            ).show();

                            resetButton();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<List<Profile>> call,
                            Throwable t
                    ) {

                        Log.e(
                                "PROFILE_DEBUG",
                                "FAILURE = " + t.getMessage(),
                                t
                        );

                        Toast.makeText(
                                LoginActivity.this,
                                "Profile loading failed",
                                Toast.LENGTH_LONG
                        ).show();

                        resetButton();
                    }
                });
    }

    private void showSuspendedDialog() {

        Dialog dialog =
                new Dialog(LoginActivity.this);

        dialog.setContentView(
                R.layout.dialog_account_suspended
        );

        dialog.setCancelable(false);

        Window window =
                dialog.getWindow();

        if (window != null) {

            window.setBackgroundDrawable(
                    new ColorDrawable(
                            Color.TRANSPARENT
                    )
            );
        }

        MaterialButton btnOK =
                dialog.findViewById(
                        R.id.btnDialogOK
                );

        btnOK.setOnClickListener(v ->
                dialog.dismiss()
        );

        dialog.show();
    }

    private void resetButton() {

        btnLogin.setEnabled(true);

        btnLogin.setText("LOG IN");
    }

    private void saveUser(
            String userId,
            String email,
            Profile profile,
            String accessToken,
            String refreshToken
    ) {

        SharedPreferences pref =
                getSharedPreferences(
                        "HavenUser",
                        MODE_PRIVATE
                );

        SharedPreferences.Editor editor =
                pref.edit();

        editor.putString(
                "auth_id",
                userId
        );

        editor.putString(
                "user_id",
                profile.getId()
        );

        editor.putString(
                "barangay_id",
                profile.getBarangayId()
        );

        editor.putString(
                "email",
                email
        );

        editor.putString(
                "username",
                profile.getFirst_name()
        );

        editor.putString(
                "fullname",
                profile.getFirst_name()
                        + " "
                        + profile.getLast_name()
        );

        String profileImage =
                profile.getProfile_image();

        if (profileImage == null
                || profileImage.trim().isEmpty()) {

            profileImage =
                    profile.getAvatar_url();
        }

        if (profileImage == null) {
            profileImage = "";
        }

        editor.putString(
                "profile_image",
                profileImage
        );

        editor.putString(
                "access_token",
                accessToken
        );

        editor.putString(
                "refresh_token",
                refreshToken
        );

        editor.apply();

        Log.d(
                "LOGIN_DEBUG",
                "USER SESSION SAVED"
        );

        Log.d(
                "LOGIN_DEBUG",
                "AUTH ID SAVED: " + userId
        );

        Log.d(
                "LOGIN_DEBUG",
                "ACCESS TOKEN SAVED"
        );

        Log.d(
                "LOGIN_DEBUG",
                "REFRESH TOKEN SAVED: " +
                        (!refreshToken.isEmpty())
        );
    }
}

