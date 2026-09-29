package com.example.havenapp.api;

import com.example.havenapp.model.AuthResponse;
import com.example.havenapp.model.LoginRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabaseAuthAPI {

    // ==========================
    // LOGIN
    // ==========================

    @Headers({
            "Content-Type: application/json"
    })
    @POST("auth/v1/token")
    Call<AuthResponse> login(
            @Query("grant_type") String grantType,
            @Body LoginRequest loginRequest
    );


    // ==========================
    // REFRESH ACCESS TOKEN
    // ==========================

    @Headers({
            "Content-Type: application/json"
    })
    @POST("auth/v1/token")
    Call<AuthResponse> refreshToken(
            @Query("grant_type") String grantType,
            @Body RefreshTokenRequest request
    );
}
