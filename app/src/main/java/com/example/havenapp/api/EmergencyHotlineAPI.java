package com.example.havenapp.api;

import com.example.havenapp.model.EmergencyHotline;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface EmergencyHotlineAPI {

    @GET(
            "rest/v1/emergency_hotlines" +
                    "?select=*" +
                    "&is_active=eq.true" +
                    "&order=display_order.asc"
    )
    Call<List<EmergencyHotline>> getEmergencyHotlines(
            @Header("Authorization")
            String authorization
    );
}