package com.example.havenapp.api;

import com.example.havenapp.model.HopeChatRequest;
import com.example.havenapp.model.HopeChatResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface HopeChatAPI {

    @POST("functions/v1/hope-chat")
    Call<HopeChatResponse> askHope(
            @Header("Authorization") String authorization,
            @Body HopeChatRequest request
    );
}