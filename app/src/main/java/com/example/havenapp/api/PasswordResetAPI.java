package com.example.havenapp.api;

import com.example.havenapp.model.EmailRequest;
import com.example.havenapp.model.ResetPasswordRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface PasswordResetAPI {

    @Headers({
            "Content-Type: application/json"
    })
    @POST("functions/v1/send-reset-otp")
    Call<Void> sendResetOTP(
            @Body EmailRequest request
    );

    @Headers({
            "Content-Type: application/json"
    })
    @POST("functions/v1/reset-password")
    Call<Void> resetPassword(
            @Body ResetPasswordRequest request
    );

}