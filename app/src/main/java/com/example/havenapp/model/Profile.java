package com.example.havenapp.model;

import com.google.gson.annotations.SerializedName;

public class Profile {

    private String id;
    private String auth_id;

    private String first_name;
    private String middle_name;
    private String last_name;

    private String alias;

    private String phone_number;
    private String email;

    private String address;

    private String role;
    private String status;

    private String avatar_url;

    // BARANGAY ID
    private String barangay_id;

    // PROFILE IMAGE
    private String profile_image;

    // FIRST LOGIN
    @SerializedName("first_login")
    private boolean firstLogin;


    // ==========================
    // GETTERS
    // ==========================

    public String getId() {
        return id;
    }

    public String getAuth_id() {
        return auth_id;
    }

    public String getFirst_name() {
        return first_name;
    }

    public String getMiddle_name() {
        return middle_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public String getAlias() {
        return alias;
    }

    public String getPhone_number() {
        return phone_number;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public String getAvatar_url() {
        return avatar_url;
    }

    // ==========================
    // BARANGAY ID GETTER
    // ==========================

    public String getBarangayId() {
        return barangay_id;
    }

    // ==========================
    // PROFILE IMAGE GETTER
    // ==========================

    public String getProfile_image() {
        return profile_image;
    }

    // ==========================
    // FIRST LOGIN CHECK
    // ==========================

    public boolean isFirstLogin() {
        return firstLogin;
    }


    // ==========================s
    // SETTERS
    // ==========================

    public void setFirstLogin(boolean firstLogin) {
        this.firstLogin = firstLogin;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
    }

    // ==========================
    // BARANGAY ID SETTER
    // ==========================

    public void setBarangayId(String barangay_id) {
        this.barangay_id = barangay_id;
    }

    // ==========================
    // PROFILE IMAGE SETTER
    // ==========================

    public void setProfile_image(String profile_image) {
        this.profile_image = profile_image;
    }
}