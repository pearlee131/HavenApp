package com.example.havenapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        // OPEN HOME SCREEN
        Intent intent = new Intent(
                MainActivity.this,
                HomeActivity.class
        );


        startActivity(intent);


        // close MainActivity so back button won't return here
        finish();

    }


}