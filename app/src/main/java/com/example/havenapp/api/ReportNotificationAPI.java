package com.example.havenapp.api;

import com.example.havenapp.model.ReportNotification;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.Query;

public interface ReportNotificationAPI {

    // =========================================================
    // GET USER NOTIFICATIONS
    // =========================================================

    @GET("rest/v1/report_notifications")
    Call<List<ReportNotification>> getNotifications(
            @Header("Authorization") String authorization,
            @Query("user_id") String userIdFilter,
            @Query("select") String select,
            @Query("order") String order
    );


    // =========================================================
    // GET UNREAD NOTIFICATIONS
    // =========================================================

    @GET("rest/v1/report_notifications")
    Call<List<ReportNotification>> getUnreadNotifications(
            @Header("Authorization") String authorization,
            @Query("user_id") String userIdFilter,
            @Query("is_read") String isReadFilter,
            @Query("select") String select,
            @Query("order") String order
    );


    // =========================================================
    // MARK ONE NOTIFICATION AS READ
    // =========================================================

    @PATCH("rest/v1/report_notifications")
    Call<Void> markNotificationAsRead(
            @Header("Authorization") String authorization,
            @Query("id") String notificationIdFilter,
            @Query("user_id") String userIdFilter,
            @Body Map<String, Object> updateData
    );


    // =========================================================
    // MARK ALL USER NOTIFICATIONS AS READ
    // =========================================================

    @PATCH("rest/v1/report_notifications")
    Call<Void> markAllNotificationsAsRead(
            @Header("Authorization") String authorization,
            @Query("user_id") String userIdFilter,
            @Query("is_read") String isReadFilter,
            @Body Map<String, Object> updateData
    );
}