package com.example.havenapp.api;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface IncidentAttachmentStorageAPI {

    @POST("storage/v1/object/incident-attachments/{filePath}")
    Call<Void> uploadIncidentAttachment(
            @Header("Authorization") String authorization,
            @Path(value = "filePath", encoded = true) String filePath,
            @Body RequestBody file
    );


    @DELETE("storage/v1/object/incident-attachments/{filePath}")
    Call<Void> deleteIncidentAttachment(
            @Header("Authorization") String authorization,
            @Path(value = "filePath", encoded = true) String filePath
    );
}
