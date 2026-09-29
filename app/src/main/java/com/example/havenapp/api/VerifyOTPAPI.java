package com.example.havenapp.api;

import com.example.havenapp.model.VerifyOTPRequest;
import com.example.havenapp.model.VerifyOTPResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface VerifyOTPAPI {

    @Headers({
            "Content-Type: application/json"
    })
    @POST("functions/v1/verify-reset-otp")
    Call<VerifyOTPResponse> verifyOTP(
            @Body VerifyOTPRequest request
    );

}

