package com.example.havenapp.model;

import java.util.List;

public class EmergencyHotline {

    private String id;
    private String name;
    private List<String> phone_numbers;
    private int display_order;
    private boolean is_active;

    public EmergencyHotline() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getPhoneNumbers() {
        return phone_numbers;
    }

    public int getDisplayOrder() {
        return display_order;
    }

    public boolean isActive() {
        return is_active;
    }
}