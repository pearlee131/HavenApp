package com.example.havenapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class NotificationDetailsActivity extends AppCompatActivity {

    // =========================================================
    // VIEWS
    // =========================================================

    private View cardHopeAssistant;

    private ImageView imgNotificationDetailsIcon;

    private TextView txtNotificationDetailsTitle;

    private TextView txtNotificationDetailsTime;

    private TextView txtNotificationDetailsMessage;

    private TextView txtNotificationDetailsSupportingMessage;

    private TextView txtNotificationDetailsClosing;

    private MaterialButton btnNotificationGotIt;


    // =========================================================
    // NOTIFICATION DATA
    // =========================================================

    private String notificationTitle = "";

    private String notificationMessage = "";

    private String notificationType = "";

    private String caseNumber = "";

    private String createdAt = "";

    private String reportId = "";


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_notification_details
        );


        // =====================================================
        // HOPE ASSISTANT
        // =====================================================

        cardHopeAssistant =
                findViewById(
                        R.id.cardHopeAssistant
                );

        if (cardHopeAssistant != null) {

            ChatbotFloatingHelper.setup(
                    this,
                    cardHopeAssistant
            );
        }


        initializeViews();

        readNotificationData();

        displayNotification();

        setupActions();
    }


    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initializeViews() {

        imgNotificationDetailsIcon =
                findViewById(
                        R.id.imgNotificationDetailsIcon
                );


        txtNotificationDetailsTitle =
                findViewById(
                        R.id.txtNotificationDetailsTitle
                );


        txtNotificationDetailsTime =
                findViewById(
                        R.id.txtNotificationDetailsTime
                );


        txtNotificationDetailsMessage =
                findViewById(
                        R.id.txtNotificationDetailsMessage
                );


        txtNotificationDetailsSupportingMessage =
                findViewById(
                        R.id.txtNotificationDetailsSupportingMessage
                );


        txtNotificationDetailsClosing =
                findViewById(
                        R.id.txtNotificationDetailsClosing
                );


        btnNotificationGotIt =
                findViewById(
                        R.id.btnNotificationGotIt
                );
    }


    // =========================================================
    // READ DATA PASSED FROM NOTIFICATIONS ACTIVITY
    // =========================================================

    private void readNotificationData() {

        notificationTitle =
                getIntent().getStringExtra(
                        "notification_title"
                );


        notificationMessage =
                getIntent().getStringExtra(
                        "notification_message"
                );


        notificationType =
                getIntent().getStringExtra(
                        "notification_type"
                );


        caseNumber =
                getIntent().getStringExtra(
                        "case_number"
                );


        createdAt =
                getIntent().getStringExtra(
                        "created_at"
                );


        reportId =
                getIntent().getStringExtra(
                        "report_id"
                );


        // -----------------------------------------------------
        // NULL SAFETY
        // -----------------------------------------------------

        if (notificationTitle == null) {
            notificationTitle = "";
        }

        if (notificationMessage == null) {
            notificationMessage = "";
        }

        if (notificationType == null) {
            notificationType = "";
        }

        if (caseNumber == null) {
            caseNumber = "";
        }

        if (createdAt == null) {
            createdAt = "";
        }

        if (reportId == null) {
            reportId = "";
        }
    }


    // =========================================================
    // DISPLAY NOTIFICATION
    // =========================================================

    private void displayNotification() {

        // -----------------------------------------------------
        // TITLE
        // -----------------------------------------------------

        if (notificationTitle.trim().isEmpty()) {

            txtNotificationDetailsTitle.setText(
                    "Report Update"
            );

        } else {

            txtNotificationDetailsTitle.setText(
                    notificationTitle
            );
        }


        // -----------------------------------------------------
        // MAIN MESSAGE
        // -----------------------------------------------------

        if (notificationMessage.trim().isEmpty()) {

            txtNotificationDetailsMessage.setText(
                    "There is a new update regarding your report."
            );

        } else {

            txtNotificationDetailsMessage.setText(
                    notificationMessage
            );
        }


        // -----------------------------------------------------
        // DATE AND TIME
        // -----------------------------------------------------

        txtNotificationDetailsTime.setText(
                formatNotificationDate(
                        createdAt
                )
        );


        // -----------------------------------------------------
        // SUPPORTING MESSAGE
        // -----------------------------------------------------

        txtNotificationDetailsSupportingMessage.setText(
                getSupportingMessage(
                        notificationType
                )
        );


        // -----------------------------------------------------
        // CLOSING MESSAGE
        // -----------------------------------------------------

        txtNotificationDetailsClosing.setText(
                getClosingMessage(
                        notificationType
                )
        );


        // -----------------------------------------------------
        // REPORT ICON
        // -----------------------------------------------------

        imgNotificationDetailsIcon.setImageResource(
                R.drawable.ic_report
        );
    }


    // =========================================================
    // SUPPORTING MESSAGE
    // =========================================================

    private String getSupportingMessage(
            String type
    ) {

        if (type == null) {
            type = "";
        }


        switch (type.trim().toLowerCase(Locale.US)) {

            case "submitted":

                return "Your report has been received by HavenApp. "
                        + "You will be notified when there are updates "
                        + "to its status";


            case "under_review":

                return "Your report is currently being reviewed. "
                        + "You will be notified once there are further "
                        + "updates.";


            case "validated":

                return "Your report has been reviewed and validated. "
                        + "Further action may now be taken based on "
                        + "the details of your report.";


            case "referred":

                return "Your report has been referred to the appropriate "
                        + "office or personnel for further assistance "
                        + "and action.";


            case "resolved":

                return "Your report has been marked as resolved. "
                        + "This means the reported case has reached "
                        + "its resolved status in HavenApp.";


            case "closed":

                return "Your report has been closed. "
                        + "You can still review your report information "
                        + "from the Reports section of HavenApp.";


            default:

                return "There is a new update regarding your report. "
                        + "Please check your report information for "
                        + "the latest status.";
        }
    }


    // =========================================================
    // CLOSING MESSAGE
    // =========================================================

    private String getClosingMessage(
            String type
    ) {

        if (type == null) {
            type = "";
        }


        switch (type.trim().toLowerCase(Locale.US)) {

            case "submitted":
            case "under_review":
            case "validated":
            case "referred":

                return "Thank you for trusting HavenApp.";


            case "resolved":

                return "We hope HavenApp was able to support you "
                        + "throughout the reporting process.";


            case "closed":

                return "Thank you for using HavenApp.";


            default:

                return "Thank you for trusting HavenApp.";
        }
    }


    // =========================================================
    // FORMAT DATE
    // =========================================================

    private String formatNotificationDate(
            String timestamp
    ) {

        if (timestamp == null
                || timestamp.trim().isEmpty()) {

            return "";
        }


        try {

            String normalized =
                    timestamp.trim();


            /*
             * Supabase may return more than 3 fractional
             * second digits.
             *
             * Java SimpleDateFormat only needs milliseconds.
             */

            normalized =
                    normalized.replace(
                            "Z",
                            "+00:00"
                    );


            int dotIndex =
                    normalized.indexOf('.');


            if (dotIndex >= 0) {

                int timezonePlus =
                        normalized.indexOf(
                                '+',
                                dotIndex
                        );


                int timezoneMinus =
                        normalized.indexOf(
                                '-',
                                dotIndex
                        );


                int timezoneIndex =
                        timezonePlus >= 0
                                ? timezonePlus
                                : timezoneMinus;


                if (timezoneIndex > dotIndex) {

                    String fraction =
                            normalized.substring(
                                    dotIndex + 1,
                                    timezoneIndex
                            );


                    if (fraction.length() > 3) {

                        fraction =
                                fraction.substring(
                                        0,
                                        3
                                );
                    }


                    while (fraction.length() < 3) {

                        fraction += "0";
                    }


                    normalized =
                            normalized.substring(
                                    0,
                                    dotIndex + 1
                            )
                                    + fraction
                                    + normalized.substring(
                                    timezoneIndex
                            );
                }
            }


            SimpleDateFormat inputFormat =
                    new SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                            Locale.US
                    );


            inputFormat.setTimeZone(
                    TimeZone.getTimeZone("UTC")
            );


            Date notificationDate =
                    inputFormat.parse(
                            normalized
                    );


            if (notificationDate == null) {

                return "";
            }


            // -------------------------------------------------
            // CHECK IF TODAY
            // -------------------------------------------------

            java.util.Calendar notificationCalendar =
                    java.util.Calendar.getInstance();


            notificationCalendar.setTime(
                    notificationDate
            );


            java.util.Calendar today =
                    java.util.Calendar.getInstance();


            boolean isToday =
                    notificationCalendar.get(
                            java.util.Calendar.YEAR
                    )
                            == today.get(
                            java.util.Calendar.YEAR
                    )

                            &&

                            notificationCalendar.get(
                                    java.util.Calendar.DAY_OF_YEAR
                            )
                                    == today.get(
                                    java.util.Calendar.DAY_OF_YEAR
                            );


            if (isToday) {

                SimpleDateFormat todayFormat =
                        new SimpleDateFormat(
                                "'Today,' h:mm a",
                                Locale.US
                        );


                return todayFormat.format(
                        notificationDate
                );
            }


            // -------------------------------------------------
            // OLDER DATE
            // -------------------------------------------------

            SimpleDateFormat dateFormat =
                    new SimpleDateFormat(
                            "MMMM d, yyyy, h:mm a",
                            Locale.US
                    );


            return dateFormat.format(
                    notificationDate
            );


        } catch (Exception e) {

            return "";
        }
    }


    // =========================================================
    // ACTIONS
    // =========================================================

    private void setupActions() {

        btnNotificationGotIt
                .setOnClickListener(
                        v -> finish()
                );
    }
}