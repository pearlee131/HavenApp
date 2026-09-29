package com.example.havenapp;


import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;


import com.example.havenapp.api.PasswordResetAPI;
import com.example.havenapp.api.SupabaseClient;
import com.example.havenapp.api.VerifyOTPAPI;
import com.example.havenapp.model.EmailRequest;
import com.example.havenapp.model.VerifyOTPRequest;
import com.example.havenapp.model.VerifyOTPResponse;

import com.google.android.material.button.MaterialButton;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;



public class CheckYourEmailActivity extends AppCompatActivity {


    private MaterialButton btnVerify;

    private TextView txtResend;
    private TextView txtOtpError;


    private EditText otp1, otp2, otp3, otp4, otp5, otp6;


    private String email;


    private VerifyOTPAPI verifyOTPAPI;

    private PasswordResetAPI passwordResetAPI;



    private int resendAttempts = 0;

    private static final int MAX_RESEND = 3;

    private boolean canResend = true;

    private CountDownTimer resendTimer;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.checkyouremail_activity);



        email = getIntent().getStringExtra("email");



        verifyOTPAPI =
                SupabaseClient
                        .getClient()
                        .create(VerifyOTPAPI.class);



        passwordResetAPI =
                SupabaseClient
                        .getClient()
                        .create(PasswordResetAPI.class);



        initializeViews();

        setupOtpInputs();



        btnVerify.setOnClickListener(v -> {


            String otp =
                    otp1.getText().toString()
                            + otp2.getText().toString()
                            + otp3.getText().toString()
                            + otp4.getText().toString()
                            + otp5.getText().toString()
                            + otp6.getText().toString();



            if(otp.length() != 6){


                showOtpError(
                        "Please enter the complete OTP."
                );


                return;

            }



            hideOtpError();



            verifyOTP(
                    email,
                    otp
            );


        });





        txtResend.setOnClickListener(v -> {


            if(!canResend){

                return;

            }



            if(resendAttempts >= MAX_RESEND){


                txtResend.setText(
                        "Too many requests. Try again later."
                );


                txtResend.setTextColor(
                        Color.RED
                );


                txtResend.setEnabled(false);


                return;


            }





            resendAttempts++;


            resendOTP(email);


            startResendCountdown();



        });



    }





    private void initializeViews(){



        btnVerify =
                findViewById(R.id.btnVerify);



        txtResend =
                findViewById(R.id.txtResend);



        txtOtpError =
                findViewById(R.id.txtOtpError);




        otp1 =
                findViewById(R.id.otp1);

        otp2 =
                findViewById(R.id.otp2);

        otp3 =
                findViewById(R.id.otp3);

        otp4 =
                findViewById(R.id.otp4);

        otp5 =
                findViewById(R.id.otp5);

        otp6 =
                findViewById(R.id.otp6);



        otp1.requestFocus();


    }





    private void verifyOTP(
            String email,
            String otp
    ){


        btnVerify.setEnabled(false);

        btnVerify.setText("Verifying...");



        verifyOTPAPI
                .verifyOTP(
                        new VerifyOTPRequest(
                                email,
                                otp
                        )
                )

                .enqueue(new Callback<VerifyOTPResponse>() {



                    @Override
                    public void onResponse(
                            Call<VerifyOTPResponse> call,
                            Response<VerifyOTPResponse> response
                    ){


                        btnVerify.setEnabled(true);

                        btnVerify.setText("Verify");



                        if(response.isSuccessful()
                                && response.body()!=null){



                            VerifyOTPResponse result =
                                    response.body();



                            Toast.makeText(
                                    CheckYourEmailActivity.this,
                                    result.getMessage(),
                                    Toast.LENGTH_SHORT
                            ).show();




                            Intent intent =
                                    new Intent(
                                            CheckYourEmailActivity.this,
                                            SetPasswordActivity.class
                                    );



                            intent.putExtra(
                                    "email",
                                    email
                            );



                            intent.putExtra(
                                    "auth_id",
                                    result.getAuthId()
                            );



                            startActivity(intent);


                            finish();



                        }
                        else{


                            showOtpError(
                                    "Invalid OTP. Please try again."
                            );


                        }


                    }





                    @Override
                    public void onFailure(
                            Call<VerifyOTPResponse> call,
                            Throwable t
                    ){


                        btnVerify.setEnabled(true);

                        btnVerify.setText("Verify");



                        showOtpError(
                                "Connection error. Please try again."
                        );


                    }



                });



    }     private void resendOTP(String email){


        passwordResetAPI
                .sendResetOTP(
                        new EmailRequest(email)
                )

                .enqueue(new Callback<Void>() {



                    @Override
                    public void onResponse(
                            Call<Void> call,
                            Response<Void> response
                    ){


                        if(response.isSuccessful()){


                            Toast.makeText(
                                    CheckYourEmailActivity.this,
                                    "New OTP sent.",
                                    Toast.LENGTH_SHORT
                            ).show();



                        }
                        else{


                            Toast.makeText(
                                    CheckYourEmailActivity.this,
                                    "Failed to resend OTP.",
                                    Toast.LENGTH_SHORT
                            ).show();


                        }


                    }





                    @Override
                    public void onFailure(
                            Call<Void> call,
                            Throwable t
                    ){


                        Toast.makeText(
                                CheckYourEmailActivity.this,
                                "Connection error.",
                                Toast.LENGTH_SHORT
                        ).show();


                    }



                });



    }








    private void startResendCountdown(){



        canResend = false;



        resendTimer =
                new CountDownTimer(
                        60000,
                        1000
                ){



                    @Override
                    public void onTick(long millisUntilFinished){



                        txtResend.setText(
                                "Resend in "
                                        + millisUntilFinished / 1000
                                        + "s"
                        );



                        txtResend.setTextColor(
                                Color.GRAY
                        );



                    }




                    @Override
                    public void onFinish(){



                        canResend = true;



                        if(resendAttempts < MAX_RESEND){



                            txtResend.setText(
                                    "Click to resend"
                            );



                            txtResend.setTextColor(
                                    getColor(R.color.violet)
                            );



                        }


                    }



                };



        resendTimer.start();



    }








    private void showOtpError(String message){



        txtOtpError.setText(message);



        txtOtpError.setVisibility(
                TextView.VISIBLE
        );



        setOtpErrorBackground(true);



    }







    private void hideOtpError(){



        txtOtpError.setVisibility(
                TextView.GONE
        );



        setOtpErrorBackground(false);



    }








    private void setOtpErrorBackground(boolean error){



        int background =
                error
                        ? R.drawable.otp_box_error
                        : R.drawable.otp_box;



        otp1.setBackgroundResource(background);
        otp2.setBackgroundResource(background);
        otp3.setBackgroundResource(background);
        otp4.setBackgroundResource(background);
        otp5.setBackgroundResource(background);
        otp6.setBackgroundResource(background);



    }








    private void setupOtpInputs(){



        moveNext(otp1, otp2);

        moveNext(otp2, otp3);

        moveNext(otp3, otp4);

        moveNext(otp4, otp5);

        moveNext(otp5, otp6);




        movePrevious(otp2, otp1);

        movePrevious(otp3, otp2);

        movePrevious(otp4, otp3);

        movePrevious(otp5, otp4);

        movePrevious(otp6, otp5);



    }








    private void moveNext(
            EditText current,
            EditText next
    ){



        current.addTextChangedListener(
                new TextWatcher(){



                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ){}





                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ){



                        if(s.length() == 1){


                            hideOtpError();


                            next.requestFocus();


                        }



                    }





                    @Override
                    public void afterTextChanged(
                            Editable s
                    ){}



                });



    }









    private void movePrevious(
            EditText current,
            EditText previous
    ){



        current.setOnKeyListener(
                (v, keyCode, event) -> {



                    if(
                            keyCode == KeyEvent.KEYCODE_DEL
                                    &&
                                    event.getAction()
                                            == KeyEvent.ACTION_DOWN
                                    &&
                                    current.getText()
                                            .toString()
                                            .isEmpty()
                    ){



                        previous.requestFocus();



                        previous.setSelection(
                                previous.length()
                        );



                    }



                    return false;



                });



    }








    @Override
    protected void onDestroy(){


        super.onDestroy();



        if(resendTimer != null){

            resendTimer.cancel();

        }


    }



}