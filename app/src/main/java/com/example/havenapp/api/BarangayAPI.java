package com.example.havenapp.api;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface BarangayAPI {

    @GET("rest/v1/barangays")
    Call<List<Map<String, Object>>> getBarangays(
            @Header("Authorization") String authorization,
            @Query("select") String select,
            @Query("order") String order
    );
}