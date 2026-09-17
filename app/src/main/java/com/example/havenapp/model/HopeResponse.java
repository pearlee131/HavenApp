package com.example.havenapp.model;

import java.util.List;

public class HopeResponse {

    private boolean matched;

    private String topic_key;

    private String action_type;

    private String title;

    private String message;

    private HopeArticle article;

    private List<HopeHotline> hotlines;

    private int match_score;

    public boolean isMatched() {
        return matched;
    }

    public String getTopicKey() {
        return topic_key;
    }

    public String getActionType() {
        return action_type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public HopeArticle getArticle() {
        return article;
    }

    public List<HopeHotline> getHotlines() {
        return hotlines;
    }

    public int getMatchScore() {
        return match_score;
    }
}