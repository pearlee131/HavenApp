package com.example.havenapp.api;

public class RefreshTokenRequest {

    private String refresh_token;

    public RefreshTokenRequest(String refresh_token) {
        this.refresh_token = refresh_token;
    }

    public String getRefresh_token() {
        return refresh_token;
    }
}
