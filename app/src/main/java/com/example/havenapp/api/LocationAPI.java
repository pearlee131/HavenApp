package com.example.havenapp.api;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface LocationAPI {

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=minimal"
    })
    @POST("rest/v1/user_locations")
    Call<Void> shareLocation(
            @Body JsonObject locationData
    );
}