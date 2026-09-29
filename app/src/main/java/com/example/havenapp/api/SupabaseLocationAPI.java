package com.example.havenapp.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

import java.util.List;
import java.util.Map;

public interface SupabaseLocationAPI {

    @Headers({
            "Prefer: return=representation"
    })
    @POST("rest/v1/user_locations")
    Call<List<Map<String, Object>>> createLocation(
            @Body Map<String, Object> location
    );

    @Headers({
            "Prefer: return=representation"
    })
    @PATCH("rest/v1/user_locations")
    Call<List<Map<String, Object>>> updateLocation(
            @Query("user_id") String userId,
            @Body Map<String, Object> location
    );
}