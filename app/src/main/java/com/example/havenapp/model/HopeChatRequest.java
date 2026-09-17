package com.example.havenapp.model;

public class HopeChatRequest {

    private final String question;

    public HopeChatRequest(String question) {
        this.question = question;
    }

    public String getQuestion() {
        return question;
    }
}