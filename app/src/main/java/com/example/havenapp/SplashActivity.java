package com.example.havenapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;


public class SplashActivity extends AppCompatActivity {


    private static final int SPLASH_TIME = 2000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.splash_activity);



        new Handler().postDelayed(() -> {


            SharedPreferences preferences =
                    getSharedPreferences(
                            "HavenUser",
                            MODE_PRIVATE
                    );


            String token =
                    preferences.getString(
                            "access_token",
                            null
                    );



            Intent intent;



            if(token != null && !token.isEmpty()){


                // USER ALREADY LOGGED IN

                intent =
                        new Intent(
                                SplashActivity.this,
                                HomeActivity.class
                        );


            }
            else{


                // NO LOGIN SESSION

                intent =
                        new Intent(
                                SplashActivity.this,
                                LoginActivity.class
                        );


            }



            startActivity(intent);

            finish();



        }, SPLASH_TIME);



    }


}