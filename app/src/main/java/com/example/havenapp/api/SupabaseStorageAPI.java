package com.example.havenapp.api;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface SupabaseStorageAPI {

    @Headers({
            "x-upsert: true"
    })
    @POST("storage/v1/object/profile-images/{fileName}")
    Call<Void> uploadProfileImage(
            @Header("Authorization") String authorization,
            @Path("fileName") String fileName,
            @Body RequestBody image
    );
}