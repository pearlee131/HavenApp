package com.example.havenapp.model;

public class ResetPasswordRequest {

    private String auth_id;
    private String email;
    private String new_password;


    public ResetPasswordRequest(
            String auth_id,
            String email,
            String new_password
    ) {
        this.auth_id = auth_id;
        this.email = email;
        this.new_password = new_password;
    }


    public String getAuth_id() {
        return auth_id;
    }


    public String getEmail() {
        return email;
    }


    public String getNew_password() {
        return new_password;
    }

}