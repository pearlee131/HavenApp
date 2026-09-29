package com.example.havenapp.model;

import com.google.gson.annotations.SerializedName;

public class VerifyOTPResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("auth_id")
    private String authId;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getAuthId() {
        return authId;
    }
}
