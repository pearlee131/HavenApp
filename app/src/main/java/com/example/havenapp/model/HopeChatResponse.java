package com.example.havenapp.model;

public class HopeChatResponse {

    private boolean success;

    private HopeResponse response;

    private String error;

    public boolean isSuccess() {
        return success;
    }

    public HopeResponse getResponse() {
        return response;
    }

    public String getError() {
        return error;
    }
}