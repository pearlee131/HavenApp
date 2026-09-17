package com.example.havenapp.model;

import java.util.List;

public class HopeHotline {

    private String id;

    private String name;

    private int display_order;

    private List<String> phone_numbers;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getDisplayOrder() {
        return display_order;
    }

    public List<String> getPhoneNumbers() {
        return phone_numbers;
    }
}