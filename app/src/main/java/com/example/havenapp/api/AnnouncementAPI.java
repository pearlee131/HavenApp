package com.example.havenapp.api;

import com.example.havenapp.model.Announcement;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface AnnouncementAPI {

    @GET(
            "rest/v1/latest_announcements"
                    + "?select=id,title,summary,category,published_at,source_name,source_url,image_url,external_id"
                    + "&order=published_at.desc"
                    + "&limit=10"
    )
    Call<List<Announcement>> getLatestAnnouncements(
            @Header("Authorization") String authorization
    );
}