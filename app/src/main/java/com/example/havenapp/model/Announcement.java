package com.example.havenapp.model;

import com.google.gson.annotations.SerializedName;

public class Announcement {

    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("summary")
    private String summary;

    @SerializedName("category")
    private String category;

    @SerializedName("published_at")
    private String publishedAt;

    @SerializedName("source_name")
    private String sourceName;

    @SerializedName("source_url")
    private String sourceUrl;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("external_id")
    private String externalId;


    public String getId() {
        return id;
    }


    public String getTitle() {
        return title;
    }


    public String getSummary() {
        return summary;
    }


    public String getCategory() {
        return category;
    }


    public String getPublishedAt() {
        return publishedAt;
    }


    public String getSourceName() {
        return sourceName;
    }


    public String getSourceUrl() {
        return sourceUrl;
    }


    public String getImageUrl() {
        return imageUrl;
    }


    public String getExternalId() {
        return externalId;
    }
}