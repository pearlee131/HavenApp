package com.example.havenapp.api;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface PerpetratorAPI {

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("rest/v1/perpetrators")
    Call<List<Map<String, Object>>> createPerpetrator(
            @Header("Authorization") String authorization,
            @Body Map<String, Object> perpetrator
    );

    @GET("rest/v1/perpetrators")
    Call<List<Map<String, Object>>> getPerpetratorByReportId(
            @Header("Authorization") String authorization,
            @Query("report_id") String reportIdFilter,
            @Query("select") String select,
            @Query("limit") String limit
    );
}
