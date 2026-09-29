package com.example.havenapp.model;


public class VerifyOTPRequest {

    private String email;
    private String otp;


    public VerifyOTPRequest(
            String email,
            String otp
    ){

        this.email = email;
        this.otp = otp;

    }


    public String getEmail(){

        return email;

    }


    public String getOtp(){

        return otp;

    }

}