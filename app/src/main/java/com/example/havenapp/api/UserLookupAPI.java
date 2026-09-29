package com.example.havenapp.api;

import com.example.havenapp.model.Profile;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface UserLookupAPI {

    @Headers({
            "apikey: " + SupabaseConfig.API_KEY,
            "Content-Type: application/json"
    })
    @GET("rest/v1/users")
    Call<List<Profile>> findByEmail(
            @Header("Authorization") String token,
            @Query("email") String email
    );

    @Headers({
            "apikey: " + SupabaseConfig.API_KEY,
            "Content-Type: application/json"
    })
    @GET("rest/v1/users")
    Call<List<Profile>> findByPhone(
            @Header("Authorization") String token,
            @Query("phone_number") String phone
    );

}