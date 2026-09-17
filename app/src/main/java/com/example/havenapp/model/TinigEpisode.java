package com.example.havenapp.model;

public class TinigEpisode {

    private String id;

    private String title;

    private String description;

    private String published_at;

    private Integer duration_seconds;

    private String thumbnail_url;

    private String youtube_url;

    private String youtube_video_id;

    private String source_name;

    private String created_at;

    private String updated_at;


    // =====================================================
    // GETTERS
    // =====================================================

    public String getId() {
        return id;
    }


    public String getTitle() {
        return title;
    }


    public String getDescription() {
        return description;
    }


    public String getPublished_at() {
        return published_at;
    }


    public Integer getDuration_seconds() {
        return duration_seconds;
    }


    public String getThumbnail_url() {
        return thumbnail_url;
    }


    public String getYoutube_url() {
        return youtube_url;
    }


    public String getYoutube_video_id() {
        return youtube_video_id;
    }


    public String getSource_name() {
        return source_name;
    }


    public String getCreated_at() {
        return created_at;
    }


    public String getUpdated_at() {
        return updated_at;
    }
}