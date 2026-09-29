package com.example.havenapp.api;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabaseCallLogAPI {

    // =========================================================
    // CREATE CALL LOG
    // =========================================================

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("rest/v1/call_logs")
    Call<List<Map<String, Object>>> createCallLog(
            @Header("Authorization") String authorization,
            @Body Map<String, Object> callData
    );


    // =========================================================
    // UPDATE CALL LOG
    // =========================================================

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @PATCH("rest/v1/call_logs")
    Call<List<Map<String, Object>>> updateCallLog(
            @Header("Authorization") String authorization,
            @Query("id") String id,
            @Body Map<String, Object> callData
    );
}