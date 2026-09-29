package com.example.havenapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class PasswordUpdatedActivity extends AppCompatActivity {


    private MaterialButton btnBackToLogin;

    private LinearLayout progressLayout;

    private boolean fromFirstLogin;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.passwordupdated_activity);



        btnBackToLogin =
                findViewById(R.id.btnBackToLogin);



        progressLayout =
                findViewById(R.id.progressLayout);




        // Check where this screen came from

        fromFirstLogin =
                getIntent()
                        .getBooleanExtra(
                                "from_first_login",
                                false
                        );





        // Hide progress only for first login flow

        if(fromFirstLogin){

            progressLayout.setVisibility(
                    LinearLayout.GONE
            );

        }





        // Change button text based on the flow

        if(fromFirstLogin){

            btnBackToLogin.setText(
                    "Continue"
            );

        }

        else{

            btnBackToLogin.setText(
                    "Back to Log In"
            );

        }





        btnBackToLogin.setOnClickListener(v -> {


            Intent intent;



            if(fromFirstLogin){


                // First login flow
                // Password updated → Home

                intent =
                        new Intent(
                                PasswordUpdatedActivity.this,
                                HomeActivity.class
                        );


            }

            else{


                intent =
                        new Intent(
                                PasswordUpdatedActivity.this,
                                LoginActivity.class
                        );


            }




            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );



            startActivity(intent);


            finish();



        });



    }


}