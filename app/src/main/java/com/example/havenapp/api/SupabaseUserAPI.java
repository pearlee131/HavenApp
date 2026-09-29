package com.example.havenapp.api;

import com.example.havenapp.model.Profile;

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

public interface SupabaseUserAPI {

    // =========================================================
    // CHECK EMAIL EXISTS
    // =========================================================

    @Headers({
            "Content-Type: application/json"
    })
    @POST("rest/v1/rpc/check_email_exists")
    Call<Boolean> checkEmailExists(
            @Body Map<String, String> body
    );


    // =========================================================
    // GET USER PROFILE BY AUTH ID
    // =========================================================

    @Headers({
            "Content-Type: application/json"
    })
    @GET("rest/v1/users")
    Call<List<Profile>> getProfile(

            @Header("Authorization") String token,

            @Query("auth_id") String authId
    );


    // =========================================================
    // GET USER PROFILE BY EMAIL
    // =========================================================

    @Headers({
            "Content-Type: application/json"
    })
    @GET("rest/v1/users")
    Call<List<Profile>> getProfileByEmail(

            @Header("Authorization") String token,

            @Query("email") String email
    );


    // =========================================================
    // UPDATE FIRST LOGIN STATUS
    // =========================================================

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @PATCH("rest/v1/users")
    Call<List<Profile>> updateFirstLogin(

            @Header("Authorization") String token,

            @Query("auth_id") String authId,

            @Body Map<String, Boolean> body
    );


    // =========================================================
    // UPDATE PROFILE IMAGE
    // =========================================================

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @PATCH("rest/v1/users")
    Call<List<Profile>> updateProfileImage(

            @Header("Authorization") String token,

            @Query("auth_id") String authId,

            @Body Map<String, String> body
    );


    // =========================================================
    // GET USER'S BARANGAY ID
    // =========================================================
    //
    // This directly requests barangay_id from Supabase.
    // It avoids depending on Profile.java to deserialize
    // the barangay_id field during the panic call.
    //
    // =========================================================

    @Headers({
            "Content-Type: application/json"
    })
    @GET("rest/v1/users")
    Call<List<Map<String, Object>>> getUserBarangay(

            @Header("Authorization") String token,

            @Query("auth_id") String authId,

            @Query("select") String select
    );


    // =========================================================
    // GET BARANGAY ADMIN
    // =========================================================
    //
    // Finds the active Barangay Admin assigned to the
    // same barangay as the logged-in user.
    //
    // =========================================================

    @Headers({
            "Content-Type: application/json"
    })
    @GET("rest/v1/users")
    Call<List<Map<String, Object>>> getBarangayAdmin(

            @Header("Authorization") String token,

            @Query("barangay_id") String barangayId,

            @Query("role") String role,

            @Query("status") String status,

            @Query("select") String select
    );

    // =========================================================
// GET PUBLIC USER ID BY AUTH ID
// =========================================================

    @Headers({
            "Content-Type: application/json"
    })
    @GET("rest/v1/users")
    Call<List<Map<String, Object>>> getUserByAuthId(

            @Header("Authorization") String token,

            @Query("auth_id") String authId,

            @Query("select") String select
    );

    // =========================================================
// GET ACTIVE SUPER ADMIN
// =========================================================
//
// Finds the active Super Admin.
// Used to get the MSWDO contact number dynamically.
//
// =========================================================

    @Headers({
            "Content-Type: application/json"
    })
    @GET("rest/v1/users")
    Call<List<Map<String, Object>>> getActiveSuperAdmin(

            @Header("Authorization") String token,

            @Query("role") String role,

            @Query("status") String status,

            @Query("select") String select
    );
}

