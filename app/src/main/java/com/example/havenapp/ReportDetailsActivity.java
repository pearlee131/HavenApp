package com.example.havenapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.example.havenapp.api.IncidentReportAPI;
import com.example.havenapp.api.PerpetratorAPI;
import com.example.havenapp.api.SupabaseClient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class ReportDetailsActivity extends AppCompatActivity {

    private static final String TAG = "REPORT_DETAILS";


    // =====================================================
    // SUPABASE APIs
    // =====================================================

    private IncidentReportAPI incidentReportAPI;
    private PerpetratorAPI perpetratorAPI;
    private ReportRelatedDataAPI reportRelatedDataAPI;
    private ReportAttachmentDataAPI reportAttachmentDataAPI;


    // =====================================================
    // HEADER
    // =====================================================

    private TextView textCaseNumber;


    // =====================================================
    // CASE OVERVIEW
    // =====================================================

    private TextView textReportStatus;
    private TextView textIncidentDate;
    private TextView textIncidentTime;
    private TextView textIncidentType;
    private TextView textIncidentSubtype;


    // =====================================================
    // VICTIM INFORMATION
    // =====================================================

    private TextView textVictimFullName;
    private TextView textVictimPhone;
    private TextView textVictimEmail;
    private TextView textVictimAddress;


    // =====================================================
    // REPORTER INFORMATION
    // =====================================================

    private TextView textReporterType;
    private TextView textReporterName;
    private TextView textReporterRelationship;
    private TextView textReporterContact;
    private TextView textReporterAddress;

    private boolean currentReporterIsVictim = false;


    // =====================================================
    // ALLEGED PERPETRATOR INFORMATION
    // =====================================================

    private LinearLayout perpetratorDetailsContainer;


    // =====================================================
    // INCIDENT LOCATION
    // =====================================================

    private TextView textIncidentPlaceType;
    private TextView textIncidentAddress;
    private TextView textIncidentBarangay;
    private TextView textIncidentMunicipality;


    // =====================================================
    // ELECTRONIC / ONLINE INVOLVEMENT
    // =====================================================

    private TextView textElectronicMeans;
    private TextView textElectronicPlatform;
    private LinearLayout electronicPlatformContainer;


    // =====================================================
    // INCIDENT DESCRIPTION
    // =====================================================

    private TextView textDescription;


    // =====================================================
    // ATTACHMENTS
    // =====================================================

    private LinearLayout reportAttachmentsContainer;
    private TextView textNoAttachments;


    // =====================================================
    // REPORT INFORMATION
    // =====================================================

    private TextView textCreatedAt;
    private TextView textUpdatedAt;


    // =====================================================
    // ACTIVITY
    // =====================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.reportdetails_activity);


        // =================================================
        // VIEW REFERENCES
        // =================================================

        setupViews();


        // =================================================
        // SUPABASE APIs
        // =================================================

        incidentReportAPI =
                SupabaseClient
                        .getClient()
                        .create(IncidentReportAPI.class);

        perpetratorAPI =
                SupabaseClient
                        .getClient()
                        .create(PerpetratorAPI.class);

        reportRelatedDataAPI =
                SupabaseClient
                        .getClient()
                        .create(ReportRelatedDataAPI.class);


        reportAttachmentDataAPI =
                SupabaseClient
                        .getClient()
                        .create(ReportAttachmentDataAPI.class);


        // =================================================
        // BACK BUTTON
        // =================================================

        setupBackButton();


        // =================================================
        // GET SELECTED REPORT ID
        // =================================================

        String reportId =
                getIntent().getStringExtra(
                        "report_id"
                );


        if (reportId == null
                || reportId.trim().isEmpty()) {

            Log.e(
                    TAG,
                    "REPORT ID IS MISSING"
            );

            Toast.makeText(
                    this,
                    "Unable to identify the selected report.",
                    Toast.LENGTH_LONG
            ).show();

            finish();

            return;
        }


        reportId =
                reportId.trim();


        Log.d(
                TAG,
                "SELECTED REPORT ID: " + reportId
        );


        // =================================================
        // SHOW TEMPORARY TITLE WHILE LOADING
        // =================================================

        textCaseNumber.setText(
                "Loading report..."
        );


        // =================================================
        // LOAD SELECTED REPORT
        // =================================================

        loadReportDetails(
                reportId
        );
    }


    // =====================================================
    // SETUP VIEWS
    // =====================================================

    private void setupViews() {

        // HEADER

        textCaseNumber =
                findViewById(
                        R.id.textCaseNumber
                );


        // CASE OVERVIEW

        textReportStatus =
                findViewById(
                        R.id.textReportStatus
                );

        textIncidentDate =
                findViewById(
                        R.id.textIncidentDate
                );

        textIncidentTime =
                findViewById(
                        R.id.textIncidentTime
                );

        textIncidentType =
                findViewById(
                        R.id.textIncidentType
                );

        textIncidentSubtype =
                findViewById(
                        R.id.textIncidentSubtype
                );


        // VICTIM INFORMATION

        textVictimFullName =
                findViewById(
                        R.id.textVictimFullName
                );

        textVictimPhone =
                findViewById(
                        R.id.textVictimPhone
                );

        textVictimEmail =
                findViewById(
                        R.id.textVictimEmail
                );

        textVictimAddress =
                findViewById(
                        R.id.textVictimAddress
                );


        // REPORTER INFORMATION

        textReporterType =
                findViewById(
                        R.id.textReporterType
                );

        textReporterName =
                findViewById(
                        R.id.textReporterName
                );

        textReporterRelationship =
                findViewById(
                        R.id.textReporterRelationship
                );

        textReporterContact =
                findViewById(
                        R.id.textReporterContact
                );

        textReporterAddress =
                findViewById(
                        R.id.textReporterAddress
                );


        // ALLEGED PERPETRATOR INFORMATION

        perpetratorDetailsContainer =
                findViewById(
                        R.id.perpetratorDetailsContainer
                );


        // INCIDENT LOCATION

        textIncidentPlaceType =
                findViewById(
                        R.id.textIncidentPlaceType
                );

        textIncidentAddress =
                findViewById(
                        R.id.textIncidentAddress
                );

        textIncidentBarangay =
                findViewById(
                        R.id.textIncidentBarangay
                );

        textIncidentMunicipality =
                findViewById(
                        R.id.textIncidentMunicipality
                );


        // ELECTRONIC / ONLINE INVOLVEMENT

        textElectronicMeans =
                findViewById(
                        R.id.textElectronicMeans
                );

        textElectronicPlatform =
                findViewById(
                        R.id.textElectronicPlatform
                );

        electronicPlatformContainer =
                findViewById(
                        R.id.electronicPlatformContainer
                );


        // INCIDENT DESCRIPTION

        textDescription =
                findViewById(
                        R.id.textDescription
                );


        // ATTACHMENTS

        reportAttachmentsContainer =
                findViewById(
                        R.id.reportAttachmentsContainer
                );

        textNoAttachments =
                findViewById(
                        R.id.textNoAttachments
                );


        // REPORT INFORMATION

        textCreatedAt =
                findViewById(
                        R.id.textCreatedAt
                );

        textUpdatedAt =
                findViewById(
                        R.id.textUpdatedAt
                );
    }


    // =====================================================
    // BACK BUTTON
    // =====================================================

    private void setupBackButton() {

        findViewById(
                R.id.btnBack
        ).setOnClickListener(
                v -> finish()
        );
    }


    // =====================================================
    // LOAD REPORT DETAILS
    // =====================================================

    private void loadReportDetails(
            String reportId
    ) {

        SharedPreferences preferences =
                getSharedPreferences(
                        "HavenUser",
                        MODE_PRIVATE
                );


        String accessToken =
                preferences.getString(
                        "access_token",
                        ""
                );


        String userId =
                preferences.getString(
                        "user_id",
                        ""
                );


        // =================================================
        // SAFETY CHECK
        // =================================================

        if (accessToken == null
                || accessToken.trim().isEmpty()
                || userId == null
                || userId.trim().isEmpty()) {

            Log.e(
                    TAG,
                    "ACCESS TOKEN OR USER ID IS MISSING"
            );

            Toast.makeText(
                    this,
                    "Unable to load report details.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        accessToken =
                accessToken.trim();

        userId =
                userId.trim();


        final String finalAccessToken =
                accessToken;

        final String finalUserId =
                userId;


        // =================================================
        // GET SELECTED REPORT
        //
        // IMPORTANT:
        // FILTER BY BOTH REPORT ID AND LOGGED-IN USER ID
        // =================================================

        incidentReportAPI
                .getReportDetails(

                        "Bearer " + finalAccessToken,

                        "eq." + reportId,

                        "eq." + finalUserId,

                        "id,"
                                + "case_number,"
                                + "user_id,"
                                + "barangay_id,"
                                + "incident_barangay_id,"
                                + "incident_barangay,"
                                + "incident_municipality,"
                                + "incident_type,"
                                + "incident_subtype,"
                                + "incident_date,"
                                + "incident_time,"
                                + "incident_address,"
                                + "description,"
                                + "report_status,"
                                + "reporter_type,"
                                + "reporter_name,"
                                + "reporter_relationship,"
                                + "reporter_contact,"
                                + "victim_name,"
                                + "victim_phone,"
                                + "victim_email,"
                                + "victim_address,"
                                + "incident_place_type,"
                                + "incident_place_other,"
                                + "involved_electronic_means,"
                                + "electronic_platform,"
                                + "created_at,"
                                + "updated_at"
                )
                .enqueue(
                        new Callback<List<Map<String, Object>>>() {

                            @Override
                            public void onResponse(
                                    Call<List<Map<String, Object>>> call,
                                    Response<List<Map<String, Object>>> response
                            ) {

                                // =========================================
                                // HTTP ERROR
                                // =========================================

                                if (!response.isSuccessful()) {

                                    Log.e(
                                            TAG,
                                            "REPORT DETAILS HTTP ERROR: "
                                                    + response.code()
                                    );

                                    Toast.makeText(
                                            ReportDetailsActivity.this,
                                            "Unable to load report details.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }


                                List<Map<String, Object>> reports =
                                        response.body();


                                // =========================================
                                // REPORT NOT FOUND
                                // =========================================

                                if (reports == null
                                        || reports.isEmpty()) {

                                    Log.e(
                                            TAG,
                                            "REPORT NOT FOUND OR DOES NOT BELONG TO USER"
                                    );

                                    Toast.makeText(
                                            ReportDetailsActivity.this,
                                            "Report not found.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }


                                // =========================================
                                // GET FIRST MATCHING REPORT
                                // =========================================

                                Map<String, Object> report =
                                        reports.get(0);


                                Log.d(
                                        TAG,
                                        "REPORT DETAILS LOADED SUCCESSFULLY"
                                );


                                // =========================================
                                // DISPLAY MAIN REPORT DATA
                                // =========================================

                                displayReportDetails(
                                        report
                                );


                                // =========================================
                                // LOAD VICTIM INFORMATION
                                // =========================================

                                String reportUserId =
                                        getSafeValue(
                                                report,
                                                "user_id",
                                                finalUserId
                                        );

                                String victimName =
                                        getSafeValue(
                                                report,
                                                "victim_name",
                                                ""
                                        );

                                String victimPhone =
                                        getSafeValue(
                                                report,
                                                "victim_phone",
                                                ""
                                        );

                                String victimEmail =
                                        getSafeValue(
                                                report,
                                                "victim_email",
                                                ""
                                        );

                                String victimAddress =
                                        getSafeValue(
                                                report,
                                                "victim_address",
                                                ""
                                        );

                                boolean hasVictimSnapshot =
                                        !victimName.trim().isEmpty()
                                                || !victimPhone.trim().isEmpty()
                                                || !victimEmail.trim().isEmpty()
                                                || !victimAddress.trim().isEmpty();

                                if (hasVictimSnapshot) {

                                    displayVictimSnapshot(
                                            victimName,
                                            victimPhone,
                                            victimEmail,
                                            victimAddress
                                    );
                                }

                                // When the reporter is the victim, also load the
                                // registered user details so Home Address can include
                                // the user's assigned barangay/municipality.
                                if (currentReporterIsVictim
                                        || !hasVictimSnapshot) {

                                    loadVictimInformation(
                                            reportUserId,
                                            finalAccessToken
                                    );
                                }


                                // =========================================
                                // LOAD ALLEGED PERPETRATOR INFORMATION
                                // =========================================

                                loadPerpetratorDetails(
                                        reportId,
                                        finalAccessToken
                                );


                                // =========================================
                                // LOAD REPORT ATTACHMENTS
                                // =========================================

                                loadReportAttachments(
                                        reportId,
                                        finalAccessToken
                                );


                                // =========================================
                                // LOAD INCIDENT BARANGAY + MUNICIPALITY
                                // =========================================

                                String incidentBarangayText =
                                        getSafeValue(
                                                report,
                                                "incident_barangay",
                                                ""
                                        );

                                String incidentMunicipalityText =
                                        getSafeValue(
                                                report,
                                                "incident_municipality",
                                                ""
                                        );

                                if (!incidentBarangayText.isEmpty()) {

                                    textIncidentBarangay.setText(
                                            incidentBarangayText
                                    );

                                } else {

                                    String incidentBarangayId =
                                            getSafeValue(
                                                    report,
                                                    "incident_barangay_id",
                                                    ""
                                            );

                                    if (!incidentBarangayId.isEmpty()) {

                                        loadIncidentBarangay(
                                                incidentBarangayId,
                                                finalAccessToken
                                        );

                                    } else {

                                        textIncidentBarangay.setText(
                                                "Not provided"
                                        );
                                    }
                                }

                                textIncidentMunicipality.setText(
                                        incidentMunicipalityText.isEmpty()
                                                ? "Not provided"
                                                : incidentMunicipalityText
                                );
                            }


                            @Override
                            public void onFailure(
                                    Call<List<Map<String, Object>>> call,
                                    Throwable t
                            ) {

                                Log.e(
                                        TAG,
                                        "REPORT DETAILS NETWORK FAILURE",
                                        t
                                );

                                Toast.makeText(
                                        ReportDetailsActivity.this,
                                        "Unable to load report details.",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }


    // =====================================================
    // DISPLAY MAIN REPORT DETAILS
    // =====================================================

    private void displayReportDetails(
            Map<String, Object> report
    ) {

        // =================================================
        // CASE OVERVIEW
        // =================================================

        String reportStatus =
                getSafeValue(
                        report,
                        "report_status",
                        "submitted"
                );

        textReportStatus.setText(
                formatStatus(
                        reportStatus
                )
        );


        String incidentDate =
                getSafeValue(
                        report,
                        "incident_date",
                        ""
                );

        textIncidentDate.setText(
                formatDateOnly(
                        incidentDate
                )
        );


        String incidentTime =
                getSafeValue(
                        report,
                        "incident_time",
                        ""
                );

        textIncidentTime.setText(
                formatTimeOnly(
                        incidentTime
                )
        );


        String incidentType =
                getSafeValue(
                        report,
                        "incident_type",
                        "Not specified"
                );

        textIncidentType.setText(
                incidentType
        );

        String caseNumber =
                getSafeValue(
                        report,
                        "case_number",
                        ""
                );

        textCaseNumber.setText(
                caseNumber.isEmpty()
                        ? "Pending"
                        : caseNumber
        );


        String incidentSubtype =
                getSafeValue(
                        report,
                        "incident_subtype",
                        "Not specified"
                );

        textIncidentSubtype.setText(
                incidentSubtype
        );


        // =================================================
        // REPORTER INFORMATION
        // =================================================

        String reporterType =
                getSafeValue(
                        report,
                        "reporter_type",
                        "Not specified"
                );

        textReporterType.setText(
                formatDatabaseText(
                        reporterType
                )
        );

        currentReporterIsVictim =
                reporterType.trim().equalsIgnoreCase(
                        "victim"
                );

        if (!currentReporterIsVictim) {

            textReporterAddress.setText(
                    "Not provided"
            );
        }


        String reporterName =
                getSafeValue(
                        report,
                        "reporter_name",
                        "Not provided"
                );

        textReporterName.setText(
                reporterName
        );


        String reporterRelationship =
                getSafeValue(
                        report,
                        "reporter_relationship",
                        "Not provided"
                );

        textReporterRelationship.setText(
                reporterRelationship
        );


        String reporterContact =
                getSafeValue(
                        report,
                        "reporter_contact",
                        "Not provided"
                );

        textReporterContact.setText(
                reporterContact
        );


        // =================================================
        // INCIDENT LOCATION
        // =================================================

        String incidentPlaceType =
                getSafeValue(
                        report,
                        "incident_place_type",
                        "Not specified"
                );

        textIncidentPlaceType.setText(
                formatPlaceType(
                        incidentPlaceType
                )
        );


        String incidentAddress =
                getSafeValue(
                        report,
                        "incident_address",
                        "Not provided"
                );

        textIncidentAddress.setText(
                incidentAddress
        );


        // =================================================
        // ELECTRONIC / ONLINE INVOLVEMENT
        // =================================================

        boolean electronicMeansInvolved =
                getBooleanValue(
                        report,
                        "involved_electronic_means"
                );

        textElectronicMeans.setText(
                electronicMeansInvolved
                        ? "Yes"
                        : "No"
        );


        if (electronicMeansInvolved) {

            electronicPlatformContainer.setVisibility(
                    View.VISIBLE
            );

            String electronicPlatform =
                    getSafeValue(
                            report,
                            "electronic_platform",
                            "Not specified"
                    );

            textElectronicPlatform.setText(
                    electronicPlatform
            );

        } else {

            electronicPlatformContainer.setVisibility(
                    View.GONE
            );
        }


        // =================================================
        // INCIDENT DESCRIPTION
        // =================================================

        String description =
                getSafeValue(
                        report,
                        "description",
                        "No description provided."
                );

        textDescription.setText(
                description
        );


        // =================================================
        // REPORT INFORMATION
        // =================================================

        String createdAt =
                getSafeValue(
                        report,
                        "created_at",
                        ""
                );

        textCreatedAt.setText(
                formatDateTime(
                        createdAt
                )
        );


        String updatedAt =
                getSafeValue(
                        report,
                        "updated_at",
                        ""
                );

        textUpdatedAt.setText(
                formatDateTime(
                        updatedAt
                )
        );
    }


    private void displayVictimSnapshot(
            String victimName,
            String victimPhone,
            String victimEmail,
            String victimAddress
    ) {

        textVictimFullName.setText(
                victimName == null
                        || victimName.trim().isEmpty()
                        ? "Not provided"
                        : victimName.trim()
        );

        textVictimPhone.setText(
                victimPhone == null
                        || victimPhone.trim().isEmpty()
                        ? "Not provided"
                        : victimPhone.trim()
        );

        textVictimEmail.setText(
                victimEmail == null
                        || victimEmail.trim().isEmpty()
                        ? "Not provided"
                        : victimEmail.trim()
        );

        textVictimAddress.setText(
                victimAddress == null
                        || victimAddress.trim().isEmpty()
                        ? "Not provided"
                        : victimAddress.trim()
        );

        if (currentReporterIsVictim) {

            textReporterName.setText(
                    victimName == null
                            || victimName.trim().isEmpty()
                            ? "Not provided"
                            : victimName.trim()
            );

            textReporterContact.setText(
                    victimPhone == null
                            || victimPhone.trim().isEmpty()
                            ? "Not provided"
                            : victimPhone.trim()
            );

            textReporterAddress.setText(
                    victimAddress == null
                            || victimAddress.trim().isEmpty()
                            ? "Not provided"
                            : victimAddress.trim()
            );
        }
    }


    // =====================================================
    // LOAD VICTIM INFORMATION
    // =====================================================

    private void loadVictimInformation(
            String userId,
            String accessToken
    ) {

        if (reportRelatedDataAPI == null
                || userId == null
                || userId.trim().isEmpty()) {

            setVictimInformationUnavailable();

            return;
        }


        reportRelatedDataAPI
                .getUserById(

                        "Bearer " + accessToken,

                        "eq." + userId.trim(),

                        "id,"
                                + "first_name,"
                                + "middle_name,"
                                + "last_name,"
                                + "phone_number,"
                                + "email,"
                                + "address,"
                                + "barangay_id",

                        "1"
                )
                .enqueue(
                        new Callback<List<Map<String, Object>>>() {

                            @Override
                            public void onResponse(
                                    Call<List<Map<String, Object>>> call,
                                    Response<List<Map<String, Object>>> response
                            ) {

                                if (!response.isSuccessful()) {

                                    Log.e(
                                            TAG,
                                            "VICTIM INFORMATION HTTP ERROR: "
                                                    + response.code()
                                    );

                                    setVictimInformationUnavailable();

                                    return;
                                }


                                List<Map<String, Object>> users =
                                        response.body();


                                if (users == null
                                        || users.isEmpty()) {

                                    Log.e(
                                            TAG,
                                            "VICTIM USER RECORD NOT FOUND"
                                    );

                                    setVictimInformationUnavailable();

                                    return;
                                }


                                Map<String, Object> user =
                                        users.get(0);


                                String firstName =
                                        getSafeValue(
                                                user,
                                                "first_name",
                                                ""
                                        );

                                String middleName =
                                        getSafeValue(
                                                user,
                                                "middle_name",
                                                ""
                                        );

                                String lastName =
                                        getSafeValue(
                                                user,
                                                "last_name",
                                                ""
                                        );


                                String fullName =
                                        buildFullName(
                                                firstName,
                                                middleName,
                                                lastName
                                        );


                                textVictimFullName.setText(
                                        fullName.isEmpty()
                                                ? "Not provided"
                                                : fullName
                                );


                                textVictimPhone.setText(
                                        getSafeValue(
                                                user,
                                                "phone_number",
                                                "Not provided"
                                        )
                                );


                                textVictimEmail.setText(
                                        getSafeValue(
                                                user,
                                                "email",
                                                "Not provided"
                                        )
                                );


                                String loadedVictimPhone =
                                        getSafeValue(
                                                user,
                                                "phone_number",
                                                "Not provided"
                                        );

                                String loadedVictimAddress =
                                        getSafeValue(
                                                user,
                                                "address",
                                                ""
                                        );

                                String victimBarangayId =
                                        getSafeValue(
                                                user,
                                                "barangay_id",
                                                ""
                                        );

                                String initialVictimAddress =
                                        loadedVictimAddress.isEmpty()
                                                ? "Not provided"
                                                : loadedVictimAddress;

                                textVictimAddress.setText(
                                        initialVictimAddress
                                );

                                if (currentReporterIsVictim) {

                                    textReporterName.setText(
                                            fullName.isEmpty()
                                                    ? "Not provided"
                                                    : fullName
                                    );

                                    textReporterContact.setText(
                                            loadedVictimPhone
                                    );

                                    textReporterAddress.setText(
                                            initialVictimAddress
                                    );
                                }

                                if (!victimBarangayId.isEmpty()) {

                                    loadVictimBarangayAddress(
                                            victimBarangayId,
                                            accessToken,
                                            loadedVictimAddress
                                    );
                                }
                            }


                            @Override
                            public void onFailure(
                                    Call<List<Map<String, Object>>> call,
                                    Throwable t
                            ) {

                                Log.e(
                                        TAG,
                                        "VICTIM INFORMATION NETWORK FAILURE",
                                        t
                                );

                                setVictimInformationUnavailable();
                            }
                        }
                );
    }


    // =====================================================
    // LOAD ALLEGED PERPETRATOR INFORMATION
    // =====================================================

    private void loadPerpetratorDetails(
            String reportId,
            String accessToken
    ) {

        if (perpetratorAPI == null) {

            Log.e(
                    TAG,
                    "PERPETRATOR API IS NULL"
            );

            setPerpetratorInformationUnavailable();

            return;
        }


        perpetratorDetailsContainer.removeAllViews();


        perpetratorAPI
                .getPerpetratorByReportId(

                        "Bearer " + accessToken,

                        "eq." + reportId,

                        "id,"
                                + "report_id,"
                                + "full_name,"
                                + "age,"
                                + "sex,"
                                + "occupation,"
                                + "relationship_to_victim,"
                                + "address",

                        "100"
                )
                .enqueue(
                        new Callback<List<Map<String, Object>>>() {

                            @Override
                            public void onResponse(
                                    Call<List<Map<String, Object>>> call,
                                    Response<List<Map<String, Object>>> response
                            ) {

                                if (!response.isSuccessful()) {

                                    Log.e(
                                            TAG,
                                            "PERPETRATOR DETAILS HTTP ERROR: "
                                                    + response.code()
                                    );

                                    setPerpetratorInformationUnavailable();

                                    return;
                                }


                                List<Map<String, Object>> perpetrators =
                                        response.body();


                                if (perpetrators == null
                                        || perpetrators.isEmpty()) {

                                    Log.d(
                                            TAG,
                                            "NO PERPETRATOR RECORD FOUND FOR REPORT"
                                    );

                                    setPerpetratorInformationUnavailable();

                                    return;
                                }


                                perpetratorDetailsContainer.removeAllViews();


                                for (int index = 0;
                                     index < perpetrators.size();
                                     index++) {

                                    Map<String, Object> perpetrator =
                                            perpetrators.get(index);

                                    addPerpetratorDetailsCard(
                                            perpetrator,
                                            index + 1
                                    );
                                }


                                Log.d(
                                        TAG,
                                        "PERPETRATORS DISPLAYED: "
                                                + perpetrators.size()
                                );
                            }


                            @Override
                            public void onFailure(
                                    Call<List<Map<String, Object>>> call,
                                    Throwable t
                            ) {

                                Log.e(
                                        TAG,
                                        "PERPETRATOR DETAILS NETWORK FAILURE",
                                        t
                                );

                                setPerpetratorInformationUnavailable();
                            }
                        }
                );
    }


    private void addPerpetratorDetailsCard(
            Map<String, Object> perpetrator,
            int number
    ) {

        CardView cardView =
                new CardView(this);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.topMargin =
                dpToPx(
                        number == 1
                                ? 10
                                : 12
                );

        cardView.setLayoutParams(
                cardParams
        );

        cardView.setRadius(
                dpToPx(18)
        );

        cardView.setCardElevation(
                dpToPx(3)
        );

        cardView.setCardBackgroundColor(
                0xFFFFFFFF
        );


        LinearLayout content =
                new LinearLayout(this);

        content.setOrientation(
                LinearLayout.VERTICAL
        );

        content.setPadding(
                dpToPx(18),
                dpToPx(16),
                dpToPx(18),
                dpToPx(16)
        );


        TextView title =
                new TextView(this);

        title.setText(
                "Perpetrator " + number
        );

        title.setTextColor(
                0xFF21115D
        );

        title.setTextSize(
                14
        );

        title.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        content.addView(
                title
        );


        addPerpetratorDetailRow(
                content,
                "Full Name",
                getSafeValue(
                        perpetrator,
                        "full_name",
                        "Not provided"
                ),
                12
        );


        addPerpetratorDetailRow(
                content,
                "Relationship to Victim",
                getSafeValue(
                        perpetrator,
                        "relationship_to_victim",
                        "Not provided"
                ),
                12
        );


        LinearLayout ageSexRow =
                new LinearLayout(this);

        ageSexRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        ageSexRow.setBaselineAligned(
                false
        );

        LinearLayout.LayoutParams ageSexParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        ageSexParams.topMargin =
                dpToPx(12);

        ageSexRow.setLayoutParams(
                ageSexParams
        );


        LinearLayout ageColumn =
                createPerpetratorDetailColumn(
                        "Age",
                        formatAge(
                                perpetrator.get(
                                        "age"
                                )
                        )
                );

        LinearLayout.LayoutParams ageParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );

        ageParams.rightMargin =
                dpToPx(8);

        ageColumn.setLayoutParams(
                ageParams
        );


        LinearLayout sexColumn =
                createPerpetratorDetailColumn(
                        "Sex",
                        getSafeValue(
                                perpetrator,
                                "sex",
                                "Not provided"
                        )
                );

        LinearLayout.LayoutParams sexParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );

        sexParams.leftMargin =
                dpToPx(8);

        sexColumn.setLayoutParams(
                sexParams
        );


        ageSexRow.addView(
                ageColumn
        );

        ageSexRow.addView(
                sexColumn
        );

        content.addView(
                ageSexRow
        );


        addPerpetratorDetailRow(
                content,
                "Occupation",
                getSafeValue(
                        perpetrator,
                        "occupation",
                        "Not provided"
                ),
                12
        );


        addPerpetratorDetailRow(
                content,
                "Current Address",
                getSafeValue(
                        perpetrator,
                        "address",
                        "Not provided"
                ),
                12
        );


        cardView.addView(
                content
        );

        perpetratorDetailsContainer.addView(
                cardView
        );
    }


    private void addPerpetratorDetailRow(
            LinearLayout parent,
            String label,
            String value,
            int topMarginDp
    ) {

        LinearLayout container =
                createPerpetratorDetailColumn(
                        label,
                        value
                );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.topMargin =
                dpToPx(topMarginDp);

        container.setLayoutParams(
                params
        );

        parent.addView(
                container
        );
    }


    private LinearLayout createPerpetratorDetailColumn(
            String label,
            String value
    ) {

        LinearLayout column =
                new LinearLayout(this);

        column.setOrientation(
                LinearLayout.VERTICAL
        );


        TextView labelView =
                new TextView(this);

        labelView.setText(
                label
        );

        labelView.setTextColor(
                0xFF948AAA
        );

        labelView.setTextSize(
                11
        );


        TextView valueView =
                new TextView(this);

        valueView.setText(
                value == null
                        || value.trim().isEmpty()
                        ? "Not provided"
                        : value
        );

        valueView.setTextColor(
                0xFF392174
        );

        valueView.setTextSize(
                13
        );

        LinearLayout.LayoutParams valueParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        valueParams.topMargin =
                dpToPx(2);

        valueView.setLayoutParams(
                valueParams
        );


        column.addView(
                labelView
        );

        column.addView(
                valueView
        );


        return column;
    }


    // =====================================================
    // LOAD VICTIM REGISTERED BARANGAY FOR HOME ADDRESS
    // =====================================================

    private void loadVictimBarangayAddress(
            String barangayId,
            String accessToken,
            String baseAddress
    ) {

        if (reportRelatedDataAPI == null
                || barangayId == null
                || barangayId.trim().isEmpty()) {

            return;
        }


        reportRelatedDataAPI
                .getBarangayById(

                        "Bearer " + accessToken,

                        "eq." + barangayId.trim(),

                        "id,"
                                + "barangay_name,"
                                + "municipality",

                        "1"
                )
                .enqueue(
                        new Callback<List<Map<String, Object>>>() {

                            @Override
                            public void onResponse(
                                    Call<List<Map<String, Object>>> call,
                                    Response<List<Map<String, Object>>> response
                            ) {

                                if (!response.isSuccessful()
                                        || response.body() == null
                                        || response.body().isEmpty()) {

                                    return;
                                }


                                Map<String, Object> barangay =
                                        response.body().get(0);

                                String barangayName =
                                        getSafeValue(
                                                barangay,
                                                "barangay_name",
                                                ""
                                        );

                                String municipality =
                                        getSafeValue(
                                                barangay,
                                                "municipality",
                                                ""
                                        );


                                StringBuilder fullAddress =
                                        new StringBuilder();

                                if (baseAddress != null
                                        && !baseAddress.trim().isEmpty()) {

                                    fullAddress.append(
                                            baseAddress.trim()
                                    );
                                }

                                if (!barangayName.isEmpty()) {

                                    if (fullAddress.length() > 0) {
                                        fullAddress.append(", ");
                                    }

                                    fullAddress.append(
                                            barangayName
                                    );
                                }

                                if (!municipality.isEmpty()) {

                                    if (fullAddress.length() > 0) {
                                        fullAddress.append(", ");
                                    }

                                    fullAddress.append(
                                            municipality
                                    );
                                }


                                String displayAddress =
                                        fullAddress.length() == 0
                                                ? "Not provided"
                                                : fullAddress.toString();


                                textVictimAddress.setText(
                                        displayAddress
                                );

                                if (currentReporterIsVictim) {

                                    textReporterAddress.setText(
                                            displayAddress
                                    );
                                }
                            }


                            @Override
                            public void onFailure(
                                    Call<List<Map<String, Object>>> call,
                                    Throwable t
                            ) {

                                Log.e(
                                        TAG,
                                        "VICTIM BARANGAY LOAD FAILURE",
                                        t
                                );
                            }
                        }
                );
    }


    // =====================================================
    // LOAD INCIDENT BARANGAY
    // =====================================================

    private void loadIncidentBarangay(
            String barangayId,
            String accessToken
    ) {

        if (reportRelatedDataAPI == null
                || barangayId == null
                || barangayId.trim().isEmpty()) {

            textIncidentBarangay.setText(
                    "Not provided"
            );

            return;
        }


        reportRelatedDataAPI
                .getBarangayById(

                        "Bearer " + accessToken,

                        "eq." + barangayId.trim(),

                        "id,"
                                + "barangay_name,"
                                + "municipality",

                        "1"
                )
                .enqueue(
                        new Callback<List<Map<String, Object>>>() {

                            @Override
                            public void onResponse(
                                    Call<List<Map<String, Object>>> call,
                                    Response<List<Map<String, Object>>> response
                            ) {

                                if (!response.isSuccessful()) {

                                    Log.e(
                                            TAG,
                                            "INCIDENT BARANGAY HTTP ERROR: "
                                                    + response.code()
                                    );

                                    textIncidentBarangay.setText(
                                            "Not provided"
                                    );

                                    return;
                                }


                                List<Map<String, Object>> barangays =
                                        response.body();


                                if (barangays == null
                                        || barangays.isEmpty()) {

                                    textIncidentBarangay.setText(
                                            "Not provided"
                                    );

                                    return;
                                }


                                Map<String, Object> barangay =
                                        barangays.get(0);


                                String barangayName =
                                        getSafeValue(
                                                barangay,
                                                "barangay_name",
                                                ""
                                        );

                                String municipality =
                                        getSafeValue(
                                                barangay,
                                                "municipality",
                                                ""
                                        );


                                String displayBarangay =
                                        barangayName;


                                if (!municipality.isEmpty()) {

                                    if (!displayBarangay.isEmpty()) {

                                        displayBarangay +=
                                                ", " + municipality;

                                    } else {

                                        displayBarangay =
                                                municipality;
                                    }
                                }


                                textIncidentBarangay.setText(
                                        displayBarangay.isEmpty()
                                                ? "Not provided"
                                                : displayBarangay
                                );
                            }


                            @Override
                            public void onFailure(
                                    Call<List<Map<String, Object>>> call,
                                    Throwable t
                            ) {

                                Log.e(
                                        TAG,
                                        "INCIDENT BARANGAY NETWORK FAILURE",
                                        t
                                );

                                textIncidentBarangay.setText(
                                        "Not provided"
                                );
                            }
                        }
                );
    }


    // =====================================================
    // RELATED READ-ONLY SUPABASE ENDPOINTS
    //
    // Kept inside this Activity so no extra API Java file is
    // required for the victim and barangay reads.
    // =====================================================


    // =====================================================
    // LOAD REPORT ATTACHMENTS
    // =====================================================

    private void loadReportAttachments(
            String reportId,
            String accessToken
    ) {

        if (reportAttachmentDataAPI == null
                || reportId == null
                || reportId.trim().isEmpty()) {

            showNoAttachments(
                    "No attachments."
            );

            return;
        }


        textNoAttachments.setVisibility(
                View.VISIBLE
        );

        textNoAttachments.setText(
                "Loading attachments..."
        );

        reportAttachmentsContainer.removeAllViews();


        reportAttachmentDataAPI
                .getReportAttachments(

                        "Bearer " + accessToken,

                        "eq." + reportId.trim(),

                        "id,"
                                + "report_id,"
                                + "file_url,"
                                + "file_type,"
                                + "file_name,"
                                + "mime_type,"
                                + "file_size,"
                                + "uploaded_at",

                        "uploaded_at.asc"
                )
                .enqueue(
                        new Callback<List<Map<String, Object>>>() {

                            @Override
                            public void onResponse(
                                    Call<List<Map<String, Object>>> call,
                                    Response<List<Map<String, Object>>> response
                            ) {

                                if (!response.isSuccessful()) {

                                    Log.e(
                                            TAG,
                                            "ATTACHMENTS HTTP ERROR: "
                                                    + response.code()
                                    );

                                    showNoAttachments(
                                            "Unable to load attachments."
                                    );

                                    return;
                                }


                                List<Map<String, Object>> attachments =
                                        response.body();


                                if (attachments == null
                                        || attachments.isEmpty()) {

                                    showNoAttachments(
                                            "No attachments were added to this report."
                                    );

                                    return;
                                }


                                textNoAttachments.setVisibility(
                                        View.GONE
                                );

                                reportAttachmentsContainer.removeAllViews();


                                for (Map<String, Object> attachment : attachments) {

                                    addAttachmentItem(
                                            attachment,
                                            accessToken
                                    );
                                }
                            }


                            @Override
                            public void onFailure(
                                    Call<List<Map<String, Object>>> call,
                                    Throwable t
                            ) {

                                Log.e(
                                        TAG,
                                        "ATTACHMENTS NETWORK FAILURE",
                                        t
                                );

                                showNoAttachments(
                                        "Unable to load attachments."
                                );
                            }
                        }
                );
    }


    // =====================================================
    // ADD ONE ATTACHMENT TO THE SCREEN
    // =====================================================

    private void addAttachmentItem(
            Map<String, Object> attachment,
            String accessToken
    ) {

        String fileName =
                getSafeValue(
                        attachment,
                        "file_name",
                        "Attachment"
                );

        String fileType =
                getSafeValue(
                        attachment,
                        "file_type",
                        "document"
                );

        String mimeType =
                getSafeValue(
                        attachment,
                        "mime_type",
                        ""
                );

        String storagePath =
                getSafeValue(
                        attachment,
                        "file_url",
                        ""
                );

        long fileSize =
                getLongValue(
                        attachment,
                        "file_size"
                );


        LinearLayout item =
                new LinearLayout(
                        this
                );

        item.setOrientation(
                LinearLayout.HORIZONTAL
        );

        item.setGravity(
                android.view.Gravity.CENTER_VERTICAL
        );

        item.setPadding(
                dpToPx(10),
                dpToPx(10),
                dpToPx(10),
                dpToPx(10)
        );


        LinearLayout.LayoutParams itemParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        itemParams.topMargin =
                dpToPx(8);

        item.setLayoutParams(
                itemParams
        );

        item.setBackgroundColor(
                android.graphics.Color.parseColor(
                        "#FAF8FF"
                )
        );


        ImageView icon =
                new ImageView(
                        this
                );

        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(
                        dpToPx(38),
                        dpToPx(38)
                );

        icon.setLayoutParams(
                iconParams
        );

        icon.setPadding(
                dpToPx(8),
                dpToPx(8),
                dpToPx(8),
                dpToPx(8)
        );


        int iconRes =
                getAttachmentIcon(
                        fileType
                );

        icon.setImageResource(
                iconRes
        );

        icon.setColorFilter(
                android.graphics.Color.parseColor(
                        "#6C2BD9"
                )
        );


        LinearLayout textColumn =
                new LinearLayout(
                        this
                );

        textColumn.setOrientation(
                LinearLayout.VERTICAL
        );

        LinearLayout.LayoutParams textColumnParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );

        textColumnParams.leftMargin =
                dpToPx(10);

        textColumn.setLayoutParams(
                textColumnParams
        );


        TextView nameView =
                new TextView(
                        this
                );

        nameView.setText(
                fileName
        );

        nameView.setTextColor(
                android.graphics.Color.parseColor(
                        "#21115D"
                )
        );

        nameView.setTextSize(
                12
        );

        nameView.setMaxLines(
                2
        );

        nameView.setEllipsize(
                android.text.TextUtils.TruncateAt.END
        );

        nameView.setTypeface(
                nameView.getTypeface(),
                android.graphics.Typeface.BOLD
        );


        TextView detailsView =
                new TextView(
                        this
                );

        detailsView.setText(
                formatAttachmentDetails(
                        fileType,
                        fileSize
                )
        );

        detailsView.setTextColor(
                android.graphics.Color.parseColor(
                        "#7C7390"
                )
        );

        detailsView.setTextSize(
                11
        );


        TextView openView =
                new TextView(
                        this
                );

        openView.setText(
                "View"
        );

        openView.setTextColor(
                android.graphics.Color.parseColor(
                        "#6C2BD9"
                )
        );

        openView.setTextSize(
                12
        );

        openView.setTypeface(
                openView.getTypeface(),
                android.graphics.Typeface.BOLD
        );

        openView.setPadding(
                dpToPx(10),
                dpToPx(8),
                0,
                dpToPx(8)
        );


        textColumn.addView(
                nameView
        );

        textColumn.addView(
                detailsView
        );

        item.addView(
                icon
        );

        item.addView(
                textColumn
        );

        item.addView(
                openView
        );


        View.OnClickListener openListener =
                v -> openAttachment(
                        storagePath,
                        mimeType,
                        accessToken
                );

        item.setOnClickListener(
                openListener
        );

        openView.setOnClickListener(
                openListener
        );


        reportAttachmentsContainer.addView(
                item
        );


        // Load an image thumbnail when the attachment is an image.
        // The item remains clickable even if thumbnail loading fails.
        if (fileType.equalsIgnoreCase("image")
                && !storagePath.isEmpty()) {

            requestSignedAttachmentUrl(
                    storagePath,
                    accessToken,
                    signedUrl -> Glide
                            .with(ReportDetailsActivity.this)
                            .load(signedUrl)
                            .centerCrop()
                            .into(icon)
            );
        }
    }


    // =====================================================
    // OPEN PRIVATE ATTACHMENT
    // =====================================================

    private void openAttachment(
            String storagePath,
            String mimeType,
            String accessToken
    ) {

        if (storagePath == null
                || storagePath.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Attachment file is unavailable.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        requestSignedAttachmentUrl(
                storagePath.trim(),
                accessToken,
                signedUrl -> {

                    try {

                        Intent intent =
                                new Intent(
                                        Intent.ACTION_VIEW
                                );

                        intent.setDataAndType(
                                Uri.parse(
                                        signedUrl
                                ),
                                mimeType == null
                                        || mimeType.trim().isEmpty()
                                        ? "*/*"
                                        : mimeType.trim()
                        );

                        intent.addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                        );

                        startActivity(
                                Intent.createChooser(
                                        intent,
                                        "Open attachment"
                                )
                        );

                    } catch (Exception e) {

                        Log.e(
                                TAG,
                                "FAILED TO OPEN ATTACHMENT",
                                e
                        );

                        // Fallback: let Android/browser handle the URL.
                        try {

                            Intent browserIntent =
                                    new Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(
                                                    signedUrl
                                            )
                                    );

                            startActivity(
                                    browserIntent
                            );

                        } catch (Exception fallbackError) {

                            Toast.makeText(
                                    ReportDetailsActivity.this,
                                    "No app is available to open this attachment.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
                }
        );
    }


    // =====================================================
    // CREATE TEMPORARY SIGNED URL FOR PRIVATE STORAGE FILE
    // =====================================================

    private void requestSignedAttachmentUrl(
            String storagePath,
            String accessToken,
            SignedUrlCallback callback
    ) {

        if (storagePath == null
                || storagePath.trim().isEmpty()) {

            return;
        }


        Map<String, Object> body =
                new java.util.HashMap<>();

        body.put(
                "expiresIn",
                3600
        );


        reportAttachmentDataAPI
                .createSignedAttachmentUrl(

                        "Bearer " + accessToken,

                        storagePath.trim(),

                        body
                )
                .enqueue(
                        new Callback<Map<String, Object>>() {

                            @Override
                            public void onResponse(
                                    Call<Map<String, Object>> call,
                                    Response<Map<String, Object>> response
                            ) {

                                if (!response.isSuccessful()
                                        || response.body() == null) {

                                    Log.e(
                                            TAG,
                                            "SIGNED URL HTTP ERROR: "
                                                    + response.code()
                                    );

                                    return;
                                }


                                Object signedUrlObject =
                                        response.body().get(
                                                "signedURL"
                                        );

                                if (signedUrlObject == null) {

                                    signedUrlObject =
                                            response.body().get(
                                                    "signedUrl"
                                            );
                                }


                                if (signedUrlObject == null) {

                                    Log.e(
                                            TAG,
                                            "SIGNED URL IS MISSING"
                                    );

                                    return;
                                }


                                String signedUrl =
                                        String.valueOf(
                                                signedUrlObject
                                        ).trim();


                                if (signedUrl.isEmpty()) {

                                    return;
                                }


                                if (signedUrl.startsWith("/")) {

                                    String baseUrl =
                                            SupabaseClient
                                                    .getClient()
                                                    .baseUrl()
                                                    .toString();

                                    if (baseUrl.endsWith("/")) {

                                        baseUrl =
                                                baseUrl.substring(
                                                        0,
                                                        baseUrl.length() - 1
                                                );
                                    }

                                    signedUrl =
                                            baseUrl
                                                    + "/storage/v1"
                                                    + signedUrl;
                                }


                                callback.onReady(
                                        signedUrl
                                );
                            }


                            @Override
                            public void onFailure(
                                    Call<Map<String, Object>> call,
                                    Throwable t
                            ) {

                                Log.e(
                                        TAG,
                                        "SIGNED URL NETWORK FAILURE",
                                        t
                                );
                            }
                        }
                );
    }


    private void showNoAttachments(
            String message
    ) {

        reportAttachmentsContainer.removeAllViews();

        textNoAttachments.setVisibility(
                View.VISIBLE
        );

        textNoAttachments.setText(
                message
        );
    }


    private int getAttachmentIcon(
            String fileType
    ) {

        if (fileType == null) {

            return android.R.drawable.ic_menu_save;
        }


        switch (fileType.toLowerCase(Locale.US)) {

            case "image":
                return android.R.drawable.ic_menu_gallery;

            case "video":
                return android.R.drawable.ic_media_play;

            case "audio":
                return android.R.drawable.ic_btn_speak_now;

            case "document":
            default:
                return android.R.drawable.ic_menu_save;
        }
    }


    private String formatAttachmentDetails(
            String fileType,
            long fileSize
    ) {

        String type =
                fileType == null
                        || fileType.trim().isEmpty()
                        ? "Attachment"
                        : formatDatabaseText(
                        fileType
                );


        if (fileSize <= 0) {

            return type;
        }


        double sizeKb =
                fileSize / 1024.0;


        if (sizeKb < 1024.0) {

            return String.format(
                    Locale.US,
                    "%s • %.1f KB",
                    type,
                    sizeKb
            );
        }


        double sizeMb =
                sizeKb / 1024.0;


        return String.format(
                Locale.US,
                "%s • %.1f MB",
                type,
                sizeMb
        );
    }


    private long getLongValue(
            Map<String, Object> data,
            String key
    ) {

        if (data == null
                || key == null
                || !data.containsKey(key)
                || data.get(key) == null) {

            return 0L;
        }


        Object value =
                data.get(
                        key
                );


        if (value instanceof Number) {

            return ((Number) value).longValue();
        }


        try {

            return Long.parseLong(
                    String.valueOf(
                            value
                    )
            );

        } catch (Exception ignored) {

            return 0L;
        }
    }


    private int dpToPx(
            int dp
    ) {

        return Math.round(
                dp
                        * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }


    private interface SignedUrlCallback {

        void onReady(
                String signedUrl
        );
    }


    // =====================================================
    // REPORT ATTACHMENTS API
    // =====================================================

    private interface ReportAttachmentDataAPI {

        @GET("rest/v1/report_attachments")
        Call<List<Map<String, Object>>> getReportAttachments(

                @Header("Authorization")
                String authorization,

                @Query("report_id")
                String reportId,

                @Query("select")
                String select,

                @Query("order")
                String order
        );


        @POST("storage/v1/object/sign/incident-attachments/{filePath}")
        Call<Map<String, Object>> createSignedAttachmentUrl(

                @Header("Authorization")
                String authorization,

                @Path(
                        value = "filePath",
                        encoded = true
                )
                String filePath,

                @Body
                Map<String, Object> body
        );
    }


    private interface ReportRelatedDataAPI {

        @GET("rest/v1/users")
        Call<List<Map<String, Object>>> getUserById(
                @Header("Authorization") String authorization,
                @Query("id") String userIdFilter,
                @Query("select") String select,
                @Query("limit") String limit
        );


        @GET("rest/v1/barangays")
        Call<List<Map<String, Object>>> getBarangayById(
                @Header("Authorization") String authorization,
                @Query("id") String barangayIdFilter,
                @Query("select") String select,
                @Query("limit") String limit
        );
    }


    // =====================================================
    // FALLBACK DISPLAY HELPERS
    // =====================================================

    private void setVictimInformationUnavailable() {

        textVictimFullName.setText(
                "Not provided"
        );

        textVictimPhone.setText(
                "Not provided"
        );

        textVictimEmail.setText(
                "Not provided"
        );

        textVictimAddress.setText(
                "Not provided"
        );
    }


    private void setPerpetratorInformationUnavailable() {

        if (perpetratorDetailsContainer == null) {
            return;
        }

        perpetratorDetailsContainer.removeAllViews();

        CardView cardView =
                new CardView(this);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.topMargin =
                dpToPx(10);

        cardView.setLayoutParams(
                cardParams
        );

        cardView.setRadius(
                dpToPx(18)
        );

        cardView.setCardElevation(
                dpToPx(3)
        );

        cardView.setCardBackgroundColor(
                0xFFFFFFFF
        );


        TextView message =
                new TextView(this);

        message.setText(
                "No perpetrator information provided."
        );

        message.setTextColor(
                0xFF392174
        );

        message.setTextSize(
                13
        );

        message.setPadding(
                dpToPx(18),
                dpToPx(18),
                dpToPx(18),
                dpToPx(18)
        );


        cardView.addView(
                message
        );

        perpetratorDetailsContainer.addView(
                cardView
        );
    }


    // =====================================================
    // SAFE MAP VALUE
    // =====================================================

    private String getSafeValue(
            Map<String, Object> data,
            String key,
            String defaultValue
    ) {

        if (data == null) {

            return defaultValue;
        }


        Object value =
                data.get(
                        key
                );


        if (value == null) {

            return defaultValue;
        }


        String result =
                String.valueOf(
                        value
                ).trim();


        if (result.isEmpty()
                || result.equalsIgnoreCase(
                "null"
        )) {

            return defaultValue;
        }


        return result;
    }


    // =====================================================
    // BOOLEAN VALUE
    // =====================================================

    private boolean getBooleanValue(
            Map<String, Object> data,
            String key
    ) {

        if (data == null) {

            return false;
        }


        Object value =
                data.get(
                        key
                );


        if (value == null) {

            return false;
        }


        if (value instanceof Boolean) {

            return (Boolean) value;
        }


        String text =
                String.valueOf(
                        value
                ).trim();


        return text.equalsIgnoreCase(
                "true"
        )
                || text.equals(
                "1"
        );
    }


    // =====================================================
    // BUILD FULL NAME
    // =====================================================

    private String buildFullName(
            String firstName,
            String middleName,
            String lastName
    ) {

        StringBuilder builder =
                new StringBuilder();


        if (firstName != null
                && !firstName.trim().isEmpty()) {

            builder.append(
                    firstName.trim()
            );
        }


        if (middleName != null
                && !middleName.trim().isEmpty()) {

            if (builder.length() > 0) {

                builder.append(
                        " "
                );
            }

            builder.append(
                    middleName.trim()
            );
        }


        if (lastName != null
                && !lastName.trim().isEmpty()) {

            if (builder.length() > 0) {

                builder.append(
                        " "
                );
            }

            builder.append(
                    lastName.trim()
            );
        }


        return builder
                .toString()
                .trim();
    }


    // =====================================================
    // FORMAT AGE
    // =====================================================

    private String formatAge(
            Object ageValue
    ) {

        if (ageValue == null) {

            return "Not provided";
        }


        if (ageValue instanceof Number) {

            return String.valueOf(
                    ((Number) ageValue).intValue()
            );
        }


        String ageText =
                String.valueOf(
                        ageValue
                ).trim();


        if (ageText.isEmpty()
                || ageText.equalsIgnoreCase(
                "null"
        )) {

            return "Not provided";
        }


        try {

            double age =
                    Double.parseDouble(
                            ageText
                    );

            return String.valueOf(
                    (int) age
            );

        } catch (NumberFormatException e) {

            return ageText;
        }
    }


    // =====================================================
    // FORMAT REPORT STATUS
    // =====================================================

    private String formatStatus(
            String status
    ) {

        if (status == null
                || status.trim().isEmpty()) {

            return "Submitted";
        }


        return formatDatabaseText(
                status
        );
    }


    // =====================================================
    // FORMAT PLACE TYPE
    // =====================================================

    private String formatPlaceType(
            String placeType
    ) {

        if (placeType == null
                || placeType.trim().isEmpty()
                || placeType.equalsIgnoreCase(
                "Not specified"
        )) {

            return "Not specified";
        }


        return formatDatabaseText(
                placeType
        );
    }


    // =====================================================
    // FORMAT DATABASE TEXT
    //
    // Example:
    // public_place -> Public Place
    // under_review -> Under Review
    // =====================================================

    private String formatDatabaseText(
            String value
    ) {

        if (value == null
                || value.trim().isEmpty()) {

            return "Not specified";
        }


        String cleaned =
                value
                        .trim()
                        .toLowerCase(
                                Locale.US
                        )
                        .replace(
                                "_",
                                " "
                        );


        String[] words =
                cleaned.split(
                        " "
                );


        StringBuilder builder =
                new StringBuilder();


        for (String word : words) {

            if (word.isEmpty()) {

                continue;
            }


            builder.append(
                    Character.toUpperCase(
                            word.charAt(0)
                    )
            );


            if (word.length() > 1) {

                builder.append(
                        word.substring(1)
                );
            }


            builder.append(
                    " "
            );
        }


        return builder
                .toString()
                .trim();
    }


    // =====================================================
    // FORMAT DATE ONLY
    // =====================================================

    private String formatDateOnly(
            String databaseDate
    ) {

        if (databaseDate == null
                || databaseDate.trim().isEmpty()) {

            return "Not specified";
        }


        try {

            SimpleDateFormat input =
                    new SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.US
                    );

            input.setLenient(
                    false
            );


            Date parsedDate =
                    input.parse(
                            databaseDate.trim()
                    );


            if (parsedDate == null) {

                return databaseDate;
            }


            SimpleDateFormat output =
                    new SimpleDateFormat(
                            "MMM d, yyyy",
                            Locale.US
                    );


            return output.format(
                    parsedDate
            );

        } catch (ParseException e) {

            return databaseDate;
        }
    }


    // =====================================================
    // FORMAT TIME ONLY
    // =====================================================

    private String formatTimeOnly(
            String databaseTime
    ) {

        if (databaseTime == null
                || databaseTime.trim().isEmpty()) {

            return "Not specified";
        }


        String cleaned =
                databaseTime.trim();


        String[] patterns = {
                "HH:mm:ss",
                "HH:mm"
        };


        for (String pattern : patterns) {

            try {

                SimpleDateFormat input =
                        new SimpleDateFormat(
                                pattern,
                                Locale.US
                        );

                input.setLenient(
                        false
                );


                Date parsedTime =
                        input.parse(
                                cleaned
                        );


                if (parsedTime != null) {

                    SimpleDateFormat output =
                            new SimpleDateFormat(
                                    "h:mm a",
                                    Locale.US
                            );


                    return output.format(
                            parsedTime
                    );
                }

            } catch (ParseException ignored) {
            }
        }


        return databaseTime;
    }


    // =====================================================
    // FORMAT TIMESTAMP
    //
    // Supabase timestamps are commonly UTC/offset timestamps.
    // The method normalizes fractional seconds and converts
    // the displayed value to the device's local timezone.
    // =====================================================

    private String formatDateTime(
            String databaseTimestamp
    ) {

        if (databaseTimestamp == null
                || databaseTimestamp.trim().isEmpty()) {

            return "Not available";
        }

        String cleaned =
                normalizeTimestamp(
                        databaseTimestamp.trim()
                );

        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss"
        };

        for (String pattern : patterns) {

            try {

                SimpleDateFormat input =
                        new SimpleDateFormat(
                                pattern,
                                Locale.US
                        );

                input.setLenient(false);

                if (pattern.endsWith("'Z'")
                        || pattern.equals("yyyy-MM-dd HH:mm:ss")) {

                    input.setTimeZone(
                            TimeZone.getTimeZone("UTC")
                    );
                }

                Date parsedDate =
                        input.parse(cleaned);

                if (parsedDate != null) {

                    SimpleDateFormat output =
                            new SimpleDateFormat(
                                    "MMM d, yyyy, h:mm a",
                                    Locale.US
                            );

                    output.setTimeZone(
                            TimeZone.getTimeZone(
                                    "Asia/Manila"
                            )
                    );

                    return output.format(parsedDate);
                }

            } catch (ParseException ignored) {
            }
        }

        return databaseTimestamp;
    }


    // =====================================================
    // NORMALIZE SUPABASE TIMESTAMP FRACTION
    //
    // Example:
    // 2026-09-04T15:52:12.123456+00:00
    // becomes:
    // 2026-09-04T15:52:12.123+00:00
    // =====================================================

    private String normalizeTimestamp(
            String timestamp
    ) {

        if (timestamp == null
                || timestamp.trim().isEmpty()) {

            return "";
        }


        String cleaned =
                timestamp.trim();


        int dotIndex =
                cleaned.indexOf(
                        '.'
                );


        if (dotIndex < 0) {

            return cleaned;
        }


        int fractionEnd =
                dotIndex + 1;


        while (fractionEnd < cleaned.length()
                && Character.isDigit(
                cleaned.charAt(
                        fractionEnd
                )
        )) {

            fractionEnd++;
        }


        String fraction =
                cleaned.substring(
                        dotIndex + 1,
                        fractionEnd
                );


        if (fraction.length() > 3) {

            fraction =
                    fraction.substring(
                            0,
                            3
                    );

        } else {

            while (fraction.length() < 3) {

                fraction +=
                        "0";
            }
        }


        return cleaned.substring(
                0,
                dotIndex + 1
        )
                + fraction
                + cleaned.substring(
                fractionEnd
        );
    }
}




