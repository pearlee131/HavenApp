package com.example.havenapp.model;

public class AuthResponse {

    private String access_token;
    private String refresh_token;
    private String token_type;
    private User user;

    public String getAccess_token() {
        return access_token;
    }

    public String getRefresh_token() {
        return refresh_token;
    }

    public String getToken_type() {
        return token_type;
    }

    public User getUser() {
        return user;
    }
}

