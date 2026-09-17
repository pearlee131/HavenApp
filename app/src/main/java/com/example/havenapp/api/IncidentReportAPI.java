package com.example.havenapp.api;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface IncidentReportAPI {

    // =====================================================
    // CREATE INCIDENT REPORT
    // =====================================================

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("rest/v1/incident_reports")
    Call<List<Map<String, Object>>> createIncidentReport(
            @Header("Authorization") String authorization,
            @Body Map<String, Object> report
    );


    // =====================================================
    // UPDATE EXISTING INCIDENT REPORT / DRAFT
    // =====================================================

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @PATCH("rest/v1/incident_reports")
    Call<List<Map<String, Object>>> updateIncidentReport(
            @Header("Authorization") String authorization,

            @Query("id")
            String reportIdFilter,

            @Query("user_id")
            String userIdFilter,

            @Body
            Map<String, Object> report
    );


    // =====================================================
    // GET REPORTS FOR MY REPORTS OVERVIEW
    // =====================================================

    @GET("rest/v1/incident_reports")
    Call<List<Map<String, Object>>> getUserReports(
            @Header("Authorization") String authorization,

            @Query("user_id")
            String userIdFilter,

            @Query("select")
            String select
    );


    // =====================================================
    // GET REPORTS FOR MY REPORTS PAGE
    // =====================================================

    @GET("rest/v1/incident_reports")
    Call<List<Map<String, Object>>> getMyReports(
            @Header("Authorization") String authorization,

            @Query("user_id")
            String userIdFilter,

            @Query("select")
            String select,

            @Query("order")
            String order
    );


    // =====================================================
    // GET ONE REPORT FOR REPORT DETAILS PAGE
    // =====================================================

    @GET("rest/v1/incident_reports")
    Call<List<Map<String, Object>>> getReportDetails(
            @Header("Authorization") String authorization,

            @Query("id")
            String reportIdFilter,

            @Query("user_id")
            String userIdFilter,

            @Query("select")
            String select
    );
}
