package com.example.havenapp.api;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ReportAttachmentAPI {

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("rest/v1/report_attachments")
    Call<List<Map<String, Object>>> createReportAttachment(
            @Header("Authorization") String authorization,
            @Body Map<String, Object> attachment
    );


    @GET("rest/v1/report_attachments")
    Call<List<Map<String, Object>>> getReportAttachments(
            @Header("Authorization") String authorization,

            @Query("report_id")
            String reportIdFilter,

            @Query("select")
            String select,

            @Query("order")
            String order
    );


    @Headers({
            "Prefer: return=minimal"
    })
    @DELETE("rest/v1/report_attachments")
    Call<Void> deleteReportAttachment(
            @Header("Authorization") String authorization,

            @Query("id")
            String attachmentIdFilter,

            @Query("report_id")
            String reportIdFilter
    );
}
