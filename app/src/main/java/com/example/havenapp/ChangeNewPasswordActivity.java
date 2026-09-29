package com.example.havenapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class ChangeNewPasswordActivity extends AppCompatActivity {


    private MaterialButton btnChangePassword;
    private MaterialButton btnSkip;


    private String email;
    private String authId;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.changenewpassword_activity);



        btnChangePassword =
                findViewById(R.id.btnChangePassword);


        btnSkip =
                findViewById(R.id.btnSkip);



        // GET USER DATA
        SharedPreferences pref =
                getSharedPreferences(
                        "HavenUser",
                        MODE_PRIVATE
                );


        email =
                pref.getString(
                        "email",
                        ""
                );


        authId =
                pref.getString(
                        "auth_id",
                        ""
                );




        // CHANGE PASSWORD BUTTON
        btnChangePassword.setOnClickListener(v -> {


            Intent intent =
                    new Intent(
                            ChangeNewPasswordActivity.this,
                            SetPasswordActivity.class
                    );


            intent.putExtra(
                    "email",
                    email
            );


            intent.putExtra(
                    "auth_id",
                    authId
            );


            intent.putExtra(
                    "from_first_login",
                    true
            );


            startActivity(intent);



        });





        // SKIP FOR NOW
        btnSkip.setOnClickListener(v -> {


            Intent intent =
                    new Intent(
                            ChangeNewPasswordActivity.this,
                            HomeActivity.class
                    );


            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );


            startActivity(intent);


            finish();


        });



    }

}