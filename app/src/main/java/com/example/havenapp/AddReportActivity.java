package com.example.havenapp;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.method.DigitsKeyListener;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.havenapp.api.IncidentAttachmentStorageAPI;
import com.example.havenapp.api.IncidentReportAPI;
import com.example.havenapp.api.PerpetratorAPI;
import com.example.havenapp.api.ReportAttachmentAPI;
import com.example.havenapp.api.SupabaseClient;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.RequestBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddReportActivity extends AppCompatActivity {

    // =====================================================
    // INCIDENT DETAILS
    // =====================================================

    private AutoCompleteTextView incidentTypeDropdown;
    private EditText incidentSubtypeInput;

    private EditText incidentDateInput;
    private EditText incidentTimeInput;


    // =====================================================
    // VICTIM INFORMATION
    // =====================================================

    private View victimInformationCard;

    private EditText victimFullNameInput;
    private EditText victimPhoneInput;
    private EditText victimEmailInput;
    private EditText victimAddressInput;


    // =====================================================
    // REPORTER INFORMATION
    // =====================================================

    private AutoCompleteTextView reporterTypeDropdown;
    private AutoCompleteTextView reporterRelationshipDropdown;

    private TextInputLayout otherReporterRelationshipLayout;
    private EditText otherReporterRelationshipInput;

    private EditText reporterNameInput;
    private EditText reporterContactInput;

    private View reporterExtraFields;


    // =====================================================
    // PERPETRATOR INFORMATION
    // =====================================================

    private EditText perpetratorCountInput;
    private LinearLayout perpetratorEntriesContainer;

    private final List<PerpetratorEntry> perpetratorEntries =
            new ArrayList<>();

    private List<Map<String, Object>> pendingPerpetratorPayloads =
            new ArrayList<>();

    private int nextPerpetratorIndexToSubmit = 0;


    // =====================================================
    // INCIDENT LOCATION
    // =====================================================

    private AutoCompleteTextView placeTypeDropdown;

    private TextInputLayout otherPlaceLayout;
    private EditText otherPlaceInput;

    private EditText incidentAddressInput;
    private EditText incidentBarangayInput;
    private EditText incidentMunicipalityInput;


    // =====================================================
    // ELECTRONIC / ONLINE INVOLVEMENT
    // =====================================================

    private RadioGroup electronicMeansRadioGroup;

    private TextInputLayout electronicPlatformLayout;
    private AutoCompleteTextView electronicPlatformDropdown;

    private TextInputLayout otherElectronicPlatformLayout;
    private EditText otherElectronicPlatformInput;


    // =====================================================
    // DESCRIPTION
    // =====================================================

    private EditText descriptionInput;
    private TextView descriptionCounter;


    // =====================================================
    // ATTACHMENTS
    // =====================================================

    private View attachmentPickerArea;
    private LinearLayout attachmentsListContainer;

    private final List<AttachmentItem> selectedAttachments =
            new ArrayList<>();

    private static final int MAX_ATTACHMENTS = 10;
    private int nextAttachmentIndexToSubmit = 0;


    // =====================================================
    // DYNAMIC PERPETRATOR ENTRY HOLDER
    // =====================================================

    private static class PerpetratorEntry {

        View root;
        TextView title;
        EditText fullNameInput;
        AutoCompleteTextView relationshipDropdown;
        TextInputLayout otherRelationshipLayout;
        EditText otherRelationshipInput;
        EditText ageInput;
        AutoCompleteTextView sexDropdown;
        EditText occupationInput;
        EditText addressInput;
    }


    private static class AttachmentItem {

        Uri uri;
        String fileName;
        String mimeType;
        String fileType;
        long fileSize;
        String storagePath = "";
        String metadataId = "";
        boolean storageUploaded = false;
        boolean metadataSaved = false;
        boolean persistedFromDraft = false;

        AttachmentItem(
                Uri uri,
                String fileName,
                String mimeType,
                String fileType,
                long fileSize
        ) {
            this.uri = uri;
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.fileType = fileType;
            this.fileSize = fileSize;
        }
    }


    // =====================================================
    // DATABASE VALUE HELPERS
    // =====================================================

    private String mapReporterTypeForDatabase(
            String reporterType
    ) {

        if (reporterType == null) {
            return "";
        }

        switch (reporterType.trim().toLowerCase(Locale.US)) {

            case "victim":
                return "victim";

            case "companion":
                return "companion";

            case "informant":
                return "informant";

            default:
                return "";
        }
    }


    private String mapPlaceTypeForDatabase(
            String placeType
    ) {

        if (placeType == null) {
            return "";
        }

        switch (placeType.trim().toLowerCase(Locale.US)) {

            case "victim home":
                return "victim_home";

            case "respondent home":
            case "perpetrator home":
                return "perpetrator_home";

            case "conjugal home":
                return "conjugal_home";

            case "school":
                return "school";

            case "workplace":
                return "workplace";

            case "public transport":
                return "public_transport";

            case "public place":
                return "public_place";

            case "evacuation area":
                return "evacuation_area";

            case "mall / hotel":
                return "mall_or_hotel";

            case "other":
                return "other";

            default:
                return "";
        }
    }


    private String formatIncidentDateForDatabase(
            String displayDate
    ) {

        if (displayDate == null
                || displayDate.trim().isEmpty()) {
            return "";
        }

        try {

            SimpleDateFormat displayFormat =
                    new SimpleDateFormat(
                            "MMMM d, yyyy",
                            Locale.getDefault()
                    );

            SimpleDateFormat databaseFormat =
                    new SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.US
                    );

            displayFormat.setLenient(false);

            return databaseFormat.format(
                    displayFormat.parse(
                            displayDate.trim()
                    )
            );

        } catch (ParseException e) {

            return "";
        }
    }


    private String formatIncidentTimeForDatabase(
            String displayTime
    ) {

        if (displayTime == null
                || displayTime.trim().isEmpty()) {
            return "";
        }

        try {

            SimpleDateFormat displayFormat =
                    new SimpleDateFormat(
                            "h:mm a",
                            Locale.getDefault()
                    );

            SimpleDateFormat databaseFormat =
                    new SimpleDateFormat(
                            "HH:mm:ss",
                            Locale.US
                    );

            displayFormat.setLenient(false);

            return databaseFormat.format(
                    displayFormat.parse(
                            displayTime.trim()
                    )
            );

        } catch (ParseException e) {

            return "";
        }
    }




    // =====================================================
    // CONDITIONAL / OTHER VALUE HELPERS
    // =====================================================

    private boolean isReporterVictim() {

        return reporterTypeDropdown
                .getText()
                .toString()
                .trim()
                .equalsIgnoreCase("Victim");
    }


    private String getIncidentTypeValue() {

        return incidentTypeDropdown
                .getText()
                .toString()
                .trim();
    }


    private String getReporterRelationshipValue() {

        String selected =
                reporterRelationshipDropdown
                        .getText()
                        .toString()
                        .trim();

        if (selected.equalsIgnoreCase("Other")) {

            return otherReporterRelationshipInput
                    .getText()
                    .toString()
                    .trim();
        }

        return selected;
    }


    private String getPerpetratorRelationshipValue(
            PerpetratorEntry entry
    ) {

        String selected =
                entry.relationshipDropdown
                        .getText()
                        .toString()
                        .trim();

        if (selected.equalsIgnoreCase("Other")) {

            return entry.otherRelationshipInput
                    .getText()
                    .toString()
                    .trim();
        }

        return selected;
    }


    private String getElectronicPlatformValue() {

        String selected =
                electronicPlatformDropdown
                        .getText()
                        .toString()
                        .trim();

        if (selected.equalsIgnoreCase("Other")) {

            return otherElectronicPlatformInput
                    .getText()
                    .toString()
                    .trim();
        }

        return selected;
    }


    private boolean hasAnyPerpetratorInformation() {

        return !perpetratorEntries.isEmpty();
    }


    // =====================================================
    // BUILD INCIDENT REPORT PAYLOAD
    // =====================================================

    private Map<String, Object> buildIncidentReportPayload() {

        SharedPreferences preferences =
                getSharedPreferences(
                        "HavenUser",
                        MODE_PRIVATE
                );

        String userId =
                preferences.getString(
                        "user_id",
                        ""
                );

        String barangayId =
                preferences.getString(
                        "barangay_id",
                        ""
                );

        String reporterTypeDisplay =
                reporterTypeDropdown
                        .getText()
                        .toString()
                        .trim();

        String placeTypeDisplay =
                placeTypeDropdown
                        .getText()
                        .toString()
                        .trim();

        boolean involvedElectronicMeans =
                electronicMeansRadioGroup
                        .getCheckedRadioButtonId()
                        == R.id.radioElectronicYes;

        Map<String, Object> report =
                new LinkedHashMap<>();

        report.put(
                "user_id",
                userId
        );

        report.put(
                "barangay_id",
                barangayId
        );

        report.put(
                "incident_type",
                getIncidentTypeValue()
        );

        report.put(
                "incident_subtype",
                incidentSubtypeInput
                        .getText()
                        .toString()
                        .trim()
        );

        report.put(
                "incident_date",
                formatIncidentDateForDatabase(
                        incidentDateInput
                                .getText()
                                .toString()
                                .trim()
                )
        );

        report.put(
                "incident_time",
                formatIncidentTimeForDatabase(
                        incidentTimeInput
                                .getText()
                                .toString()
                                .trim()
                )
        );

        report.put(
                "incident_barangay",
                incidentBarangayInput
                        .getText()
                        .toString()
                        .trim()
        );

        report.put(
                "incident_municipality",
                incidentMunicipalityInput
                        .getText()
                        .toString()
                        .trim()
        );

        report.put(
                "incident_address",
                incidentAddressInput
                        .getText()
                        .toString()
                        .trim()
        );

        report.put(
                "description",
                descriptionInput
                        .getText()
                        .toString()
                        .trim()
        );

        report.put(
                "reporter_type",
                mapReporterTypeForDatabase(
                        reporterTypeDisplay
                )
        );


        // =================================================
        // VICTIM SNAPSHOT
        //
        // For Companion / Informant reports, preserve the
        // victim information entered in this form.
        // For Victim reports, these remain null and Report
        // Details can fall back to the logged-in user's profile.
        // =================================================

        if (reporterTypeDisplay.equalsIgnoreCase("Victim")) {

            report.put("victim_name", null);
            report.put("victim_phone", null);
            report.put("victim_email", null);
            report.put("victim_address", null);

        } else {

            report.put(
                    "victim_name",
                    victimFullNameInput
                            .getText()
                            .toString()
                            .trim()
            );

            report.put(
                    "victim_phone",
                    victimPhoneInput
                            .getText()
                            .toString()
                            .trim()
            );

            String victimEmail =
                    victimEmailInput
                            .getText()
                            .toString()
                            .trim();

            report.put(
                    "victim_email",
                    victimEmail.isEmpty()
                            ? null
                            : victimEmail
            );

            report.put(
                    "victim_address",
                    victimAddressInput
                            .getText()
                            .toString()
                            .trim()
            );
        }

        if (reporterTypeDisplay.equalsIgnoreCase(
                "Victim"
        )) {

            report.put(
                    "reporter_name",
                    null
            );

            report.put(
                    "reporter_relationship",
                    null
            );

            report.put(
                    "reporter_contact",
                    null
            );

        } else {

            report.put(
                    "reporter_name",
                    reporterNameInput
                            .getText()
                            .toString()
                            .trim()
            );

            report.put(
                    "reporter_relationship",
                    getReporterRelationshipValue()
            );

            report.put(
                    "reporter_contact",
                    reporterContactInput
                            .getText()
                            .toString()
                            .trim()
            );
        }

        report.put(
                "incident_place_type",
                mapPlaceTypeForDatabase(
                        placeTypeDisplay
                )
        );

        if (placeTypeDisplay.equalsIgnoreCase(
                "Other"
        )) {

            report.put(
                    "incident_place_other",
                    otherPlaceInput
                            .getText()
                            .toString()
                            .trim()
            );

        } else {

            report.put(
                    "incident_place_other",
                    null
            );
        }

        report.put(
                "involved_electronic_means",
                involvedElectronicMeans
        );

        if (involvedElectronicMeans) {

            report.put(
                    "electronic_platform",
                    getElectronicPlatformValue()
            );

        } else {

            report.put(
                    "electronic_platform",
                    null
            );
        }

        return report;
    }


    // =====================================================
    // BUILD DRAFT INCIDENT REPORT PAYLOAD
    //
    // Drafts are allowed to be incomplete. This builder avoids sending
    // empty strings to PostgreSQL date/time/enum columns while preserving
    // every form value that can safely be stored.
    // =====================================================

    private Map<String, Object> buildDraftIncidentReportPayload() {

        SharedPreferences preferences =
                getSharedPreferences(
                        "HavenUser",
                        MODE_PRIVATE
                );

        String userId =
                preferences.getString(
                        "user_id",
                        ""
                );

        String barangayId =
                preferences.getString(
                        "barangay_id",
                        ""
                );

        Map<String, Object> report =
                new LinkedHashMap<>();

        report.put("user_id", userId);
        report.put("barangay_id", barangayId);
        report.put("report_status", "draft");

        String incidentType =
                incidentTypeDropdown
                        .getText()
                        .toString()
                        .trim();

        report.put(
                "incident_type",
                incidentType.isEmpty() ? null : incidentType
        );

        String incidentSubtype =
                incidentSubtypeInput
                        .getText()
                        .toString()
                        .trim();

        report.put(
                "incident_subtype",
                incidentSubtype.isEmpty() ? null : incidentSubtype
        );

        String displayDate =
                incidentDateInput
                        .getText()
                        .toString()
                        .trim();

        if (!displayDate.isEmpty()) {

            String databaseDate =
                    formatIncidentDateForDatabase(
                            displayDate
                    );

            if (!databaseDate.isEmpty()) {
                report.put(
                        "incident_date",
                        databaseDate
                );
            }
        }

        String displayTime =
                incidentTimeInput
                        .getText()
                        .toString()
                        .trim();

        if (!displayTime.isEmpty()) {

            String databaseTime =
                    formatIncidentTimeForDatabase(
                            displayTime
                    );

            if (!databaseTime.isEmpty()) {
                report.put(
                        "incident_time",
                        databaseTime
                );
            }
        }

        putNullableDraftText(
                report,
                "incident_barangay",
                incidentBarangayInput
        );

        putNullableDraftText(
                report,
                "incident_municipality",
                incidentMunicipalityInput
        );

        putNullableDraftText(
                report,
                "incident_address",
                incidentAddressInput
        );

        putNullableDraftText(
                report,
                "description",
                descriptionInput
        );

        String reporterTypeDisplay =
                reporterTypeDropdown
                        .getText()
                        .toString()
                        .trim();

        String reporterType =
                mapReporterTypeForDatabase(
                        reporterTypeDisplay
                );

        if (!reporterType.isEmpty()) {
            report.put(
                    "reporter_type",
                    reporterType
            );
        }

        if (reporterTypeDisplay.equalsIgnoreCase("Victim")) {

            report.put("victim_name", null);
            report.put("victim_phone", null);
            report.put("victim_email", null);
            report.put("victim_address", null);

            report.put("reporter_name", null);
            report.put("reporter_relationship", null);
            report.put("reporter_contact", null);

        } else {

            putNullableDraftText(
                    report,
                    "victim_name",
                    victimFullNameInput
            );

            putNullableDraftText(
                    report,
                    "victim_phone",
                    victimPhoneInput
            );

            putNullableDraftText(
                    report,
                    "victim_email",
                    victimEmailInput
            );

            putNullableDraftText(
                    report,
                    "victim_address",
                    victimAddressInput
            );

            putNullableDraftText(
                    report,
                    "reporter_name",
                    reporterNameInput
            );

            String reporterRelationship =
                    getReporterRelationshipValue();

            report.put(
                    "reporter_relationship",
                    reporterRelationship.isEmpty()
                            ? null
                            : reporterRelationship
            );

            putNullableDraftText(
                    report,
                    "reporter_contact",
                    reporterContactInput
            );
        }

        String placeTypeDisplay =
                placeTypeDropdown
                        .getText()
                        .toString()
                        .trim();

        String placeType =
                mapPlaceTypeForDatabase(
                        placeTypeDisplay
                );

        if (!placeType.isEmpty()) {
            report.put(
                    "incident_place_type",
                    placeType
            );
        }

        if (placeTypeDisplay.equalsIgnoreCase("Other")) {

            putNullableDraftText(
                    report,
                    "incident_place_other",
                    otherPlaceInput
            );

        } else {

            report.put(
                    "incident_place_other",
                    null
            );
        }

        boolean involvedElectronicMeans =
                electronicMeansRadioGroup
                        .getCheckedRadioButtonId()
                        == R.id.radioElectronicYes;

        report.put(
                "involved_electronic_means",
                involvedElectronicMeans
        );

        if (involvedElectronicMeans) {

            String electronicPlatform =
                    getElectronicPlatformValue();

            report.put(
                    "electronic_platform",
                    electronicPlatform.isEmpty()
                            ? null
                            : electronicPlatform
            );

        } else {

            report.put(
                    "electronic_platform",
                    null
            );
        }

        return report;
    }


    private void putNullableDraftText(
            Map<String, Object> report,
            String column,
            EditText input
    ) {

        String value =
                input == null
                        ? ""
                        : input
                          .getText()
                          .toString()
                          .trim();

        report.put(
                column,
                value.isEmpty()
                        ? null
                        : value
        );
    }


    // =====================================================
    // BUILD PERPETRATOR PAYLOAD
    // =====================================================

    private Map<String, Object> buildPerpetratorPayload(
            String reportId,
            PerpetratorEntry entry
    ) {

        Map<String, Object> perpetrator =
                new LinkedHashMap<>();

        perpetrator.put("report_id", reportId);

        String fullName =
                entry.fullNameInput.getText().toString().trim();
        perpetrator.put(
                "full_name",
                fullName.isEmpty() ? null : fullName
        );

        String ageText =
                entry.ageInput.getText().toString().trim();
        if (ageText.isEmpty()) {
            perpetrator.put("age", null);
        } else {
            perpetrator.put(
                    "age",
                    Integer.parseInt(ageText)
            );
        }

        String sex =
                entry.sexDropdown.getText().toString().trim();
        perpetrator.put(
                "sex",
                sex.isEmpty() ? null : sex
        );

        String occupation =
                entry.occupationInput.getText().toString().trim();
        perpetrator.put(
                "occupation",
                occupation.isEmpty() ? null : occupation
        );

        String relationship =
                getPerpetratorRelationshipValue(entry);
        perpetrator.put(
                "relationship_to_victim",
                relationship.isEmpty() ? null : relationship
        );

        String address =
                entry.addressInput.getText().toString().trim();
        perpetrator.put(
                "address",
                address.isEmpty() ? null : address
        );

        return perpetrator;
    }

    private List<Map<String, Object>> buildPerpetratorPayloads(
            String reportId
    ) {

        List<Map<String, Object>> payloads =
                new ArrayList<>();

        for (PerpetratorEntry entry : perpetratorEntries) {

            payloads.add(
                    buildPerpetratorPayload(
                            reportId,
                            entry
                    )
            );
        }

        return payloads;
    }

    // =====================================================
    // FORM SCROLL
    // =====================================================

    private ScrollView addReportScrollView;


    // =====================================================
    // BUTTONS
    // =====================================================

    private MaterialButton saveDraftButton;
    private MaterialButton reviewReportButton;


    // =====================================================
    // SUPABASE API
    // =====================================================

    private IncidentReportAPI incidentReportAPI;
    private PerpetratorAPI perpetratorAPI;
    private IncidentAttachmentStorageAPI incidentAttachmentStorageAPI;
    private ReportAttachmentAPI reportAttachmentAPI;

    /*
     * Stores the already-created incident report ID if the main report
     * insert succeeds but the perpetrator insert needs to be retried.
     * This prevents accidentally creating a duplicate incident report.
     */
    private String createdReportId = "";

    /*
     * True while the current save pipeline is saving an unfinished report.
     * The same perpetrator + attachment pipeline is reused so information
     * already entered by the user is preserved with the draft.
     */
    private boolean savingAsDraft = false;

    /*
     * Non-empty only when AddReportActivity was opened from DraftReportsActivity.
     * For this step we load and prefill the existing draft safely.
     */
    private String editingDraftId = "";



    // =====================================================
    // STRICT / CLEAN INPUT RULES
    //
    // These rules only control and clean user-entered form values.
    // They do not change report, draft, attachment, or Supabase logic.
    // =====================================================

    private void setupStrictInputRules() {

        // Phone numbers: digits only, maximum 11 digits while typing.
        applyPhoneInputRules(reporterContactInput);
        applyPhoneInputRules(victimPhoneInput);

        // Names: letters plus common name punctuation only.
        applyPersonNameInputRules(reporterNameInput);
        applyPersonNameInputRules(victimFullNameInput);

        // Clean/title-style text fields.
        applyCapitalizedWordsRule(otherReporterRelationshipInput);
        applyCapitalizedWordsRule(otherPlaceInput);
        applyCapitalizedWordsRule(otherElectronicPlatformInput);
        applyCapitalizedWordsRule(incidentBarangayInput);
        applyCapitalizedWordsRule(incidentMunicipalityInput);

        // Sentence-style fields.
        applyCapitalizedSentenceRule(incidentSubtypeInput);
        applyCapitalizedSentenceRule(victimAddressInput);
        applyCapitalizedSentenceRule(incidentAddressInput);
    }


    private void applyPhoneInputRules(EditText input) {

        if (input == null) {
            return;
        }

        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setKeyListener(
                DigitsKeyListener.getInstance("0123456789")
        );
        input.setFilters(
                new InputFilter[]{
                        new InputFilter.LengthFilter(11)
                }
        );
    }


    private void applyPersonNameInputRules(EditText input) {

        if (input == null) {
            return;
        }

        input.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_WORDS
        );

        input.setFilters(
                new InputFilter[]{
                        createPersonNameFilter(),
                        new InputFilter.LengthFilter(100)
                }
        );

        input.setOnFocusChangeListener(
                (view, hasFocus) -> {
                    if (!hasFocus) {
                        input.setText(
                                capitalizeWords(
                                        cleanSingleLineText(
                                                input.getText().toString()
                                        )
                                )
                        );
                        input.setSelection(input.getText().length());
                    }
                }
        );
    }


    private InputFilter createPersonNameFilter() {

        return new InputFilter() {
            @Override
            public CharSequence filter(
                    CharSequence source,
                    int start,
                    int end,
                    Spanned dest,
                    int dstart,
                    int dend
            ) {

                StringBuilder allowed = new StringBuilder();
                boolean changed = false;

                for (int i = start; i < end; i++) {

                    char c = source.charAt(i);

                    if (Character.isLetter(c)
                            || Character.isWhitespace(c)
                            || c == '-'
                            || c == '\''
                            || c == '.') {

                        allowed.append(c);

                    } else {
                        changed = true;
                    }
                }

                return changed
                        ? allowed.toString()
                        : null;
            }
        };
    }


    private void applyCapitalizedWordsRule(EditText input) {

        if (input == null) {
            return;
        }

        input.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_WORDS
        );

        input.setOnFocusChangeListener(
                (view, hasFocus) -> {
                    if (!hasFocus) {
                        input.setText(
                                capitalizeWords(
                                        cleanSingleLineText(
                                                input.getText().toString()
                                        )
                                )
                        );
                        input.setSelection(input.getText().length());
                    }
                }
        );
    }


    private void applyCapitalizedSentenceRule(EditText input) {

        if (input == null) {
            return;
        }

        input.setOnFocusChangeListener(
                (view, hasFocus) -> {
                    if (!hasFocus) {
                        input.setText(
                                capitalizeFirstLetter(
                                        cleanSingleLineText(
                                                input.getText().toString()
                                        )
                                )
                        );
                        input.setSelection(input.getText().length());
                    }
                }
        );
    }


    private void applyPerpetratorInputRules(
            PerpetratorEntry entry
    ) {

        if (entry == null) {
            return;
        }

        applyPersonNameInputRules(entry.fullNameInput);

        applyCapitalizedWordsRule(
                entry.otherRelationshipInput
        );

        if (entry.ageInput != null) {
            entry.ageInput.setInputType(
                    InputType.TYPE_CLASS_NUMBER
            );
            entry.ageInput.setKeyListener(
                    DigitsKeyListener.getInstance("0123456789")
            );
            entry.ageInput.setFilters(
                    new InputFilter[]{
                            new InputFilter.LengthFilter(3)
                    }
            );
        }

        applyCapitalizedWordsRule(entry.occupationInput);
        applyCapitalizedSentenceRule(entry.addressInput);
    }


    private String cleanSingleLineText(String value) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .replaceAll("\\s+", " ");
    }


    private String capitalizeWords(String value) {

        String clean = cleanSingleLineText(value);

        if (clean.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder(clean.length());
        boolean capitalizeNext = true;

        for (int i = 0; i < clean.length(); i++) {

            char c = clean.charAt(i);

            if (capitalizeNext && Character.isLetter(c)) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }

            if (Character.isWhitespace(c)
                    || c == '-'
                    || c == '\'') {
                capitalizeNext = true;
            }
        }

        return result.toString();
    }


    private String capitalizeFirstLetter(String value) {

        String clean = cleanSingleLineText(value);

        if (clean.isEmpty()) {
            return "";
        }

        for (int i = 0; i < clean.length(); i++) {

            char c = clean.charAt(i);

            if (Character.isLetter(c)) {
                return clean.substring(0, i)
                        + Character.toUpperCase(c)
                        + clean.substring(i + 1);
            }
        }

        return clean;
    }


    private String capitalizeFirstLetterPreserveWhitespace(
            String value
    ) {

        if (value == null) {
            return "";
        }

        String clean = value.trim();

        if (clean.isEmpty()) {
            return "";
        }

        for (int i = 0; i < clean.length(); i++) {

            char c = clean.charAt(i);

            if (Character.isLetter(c)) {
                return clean.substring(0, i)
                        + Character.toUpperCase(c)
                        + clean.substring(i + 1);
            }
        }

        return clean;
    }


    private boolean isValidPersonName(String value) {

        String clean = cleanSingleLineText(value);

        return !clean.isEmpty()
                && clean.matches(
                "^[\\p{L}][\\p{L}\\s.'-]*$"
        );
    }


    private boolean isValidPhoneNumber(String value) {

        if (value == null) {
            return false;
        }

        return value.trim().matches("\\d{11}");
    }


    private boolean isValidEmailIfProvided(String value) {

        if (value == null
                || value.trim().isEmpty()) {
            return true;
        }

        return Patterns.EMAIL_ADDRESS
                .matcher(value.trim())
                .matches();
    }


    private void cleanFormInputValues() {

        // Main/static form values.
        reporterNameInput.setText(
                capitalizeWords(
                        cleanSingleLineText(
                                reporterNameInput.getText().toString()
                        )
                )
        );

        victimFullNameInput.setText(
                capitalizeWords(
                        cleanSingleLineText(
                                victimFullNameInput.getText().toString()
                        )
                )
        );

        otherReporterRelationshipInput.setText(
                capitalizeWords(
                        cleanSingleLineText(
                                otherReporterRelationshipInput
                                        .getText()
                                        .toString()
                        )
                )
        );

        otherPlaceInput.setText(
                capitalizeWords(
                        cleanSingleLineText(
                                otherPlaceInput.getText().toString()
                        )
                )
        );

        otherElectronicPlatformInput.setText(
                capitalizeWords(
                        cleanSingleLineText(
                                otherElectronicPlatformInput
                                        .getText()
                                        .toString()
                        )
                )
        );

        incidentBarangayInput.setText(
                capitalizeWords(
                        cleanSingleLineText(
                                incidentBarangayInput
                                        .getText()
                                        .toString()
                        )
                )
        );

        incidentMunicipalityInput.setText(
                capitalizeWords(
                        cleanSingleLineText(
                                incidentMunicipalityInput
                                        .getText()
                                        .toString()
                        )
                )
        );

        incidentSubtypeInput.setText(
                capitalizeFirstLetter(
                        cleanSingleLineText(
                                incidentSubtypeInput
                                        .getText()
                                        .toString()
                        )
                )
        );

        victimAddressInput.setText(
                capitalizeFirstLetter(
                        cleanSingleLineText(
                                victimAddressInput
                                        .getText()
                                        .toString()
                        )
                )
        );

        incidentAddressInput.setText(
                capitalizeFirstLetter(
                        cleanSingleLineText(
                                incidentAddressInput
                                        .getText()
                                        .toString()
                        )
                )
        );

        // Do not collapse line breaks in the incident description.
        String description =
                descriptionInput.getText().toString().trim();

        descriptionInput.setText(
                capitalizeFirstLetterPreserveWhitespace(description)
        );

        // Dynamic perpetrator values.
        for (PerpetratorEntry entry : perpetratorEntries) {

            entry.fullNameInput.setText(
                    capitalizeWords(
                            cleanSingleLineText(
                                    entry.fullNameInput
                                            .getText()
                                            .toString()
                            )
                    )
            );

            entry.otherRelationshipInput.setText(
                    capitalizeWords(
                            cleanSingleLineText(
                                    entry.otherRelationshipInput
                                            .getText()
                                            .toString()
                            )
                    )
            );

            entry.occupationInput.setText(
                    capitalizeWords(
                            cleanSingleLineText(
                                    entry.occupationInput
                                            .getText()
                                            .toString()
                            )
                    )
            );

            entry.addressInput.setText(
                    capitalizeFirstLetter(
                            cleanSingleLineText(
                                    entry.addressInput
                                            .getText()
                                            .toString()
                            )
                    )
            );
        }
    }


    // =====================================================
    // DATE / TIME
    // =====================================================

    private final Calendar selectedDateTime =
            Calendar.getInstance();


    // =====================================================
    // ON CREATE
    // =====================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.addreport_activity);

        incidentReportAPI =
                SupabaseClient
                        .getClient()
                        .create(IncidentReportAPI.class);

        perpetratorAPI =
                SupabaseClient
                        .getClient()
                        .create(PerpetratorAPI.class);

        incidentAttachmentStorageAPI =
                SupabaseClient
                        .getClient()
                        .create(IncidentAttachmentStorageAPI.class);

        reportAttachmentAPI =
                SupabaseClient
                        .getClient()
                        .create(ReportAttachmentAPI.class);

        initializeViews();

        setupStrictInputRules();

        setupBackButton();

        setupDropdowns();

        setupOtherDropdownBehaviors();

        setupDatePicker();

        setupTimePicker();

        setupReporterTypeBehavior();

        setupPerpetratorCountBehavior();

        setupPlaceTypeBehavior();

        setupElectronicMeansBehavior();

        setupDescriptionCounter();

        clearVictimInformation();

        setupButtons();

        handleDraftReopenIntent();
    }


    // =====================================================
    // REOPEN EXISTING DRAFT
    // =====================================================

    private void handleDraftReopenIntent() {

        Intent intent = getIntent();

        if (intent == null) {
            return;
        }

        String draftId =
                intent.getStringExtra(
                        "draft_id"
                );

        if (draftId == null
                || draftId.trim().isEmpty()) {

            return;
        }

        editingDraftId =
                draftId.trim();

        loadExistingDraft(
                editingDraftId
        );
    }


    private void loadExistingDraft(
            String draftId
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

        if (accessToken == null
                || accessToken.trim().isEmpty()
                || userId == null
                || userId.trim().isEmpty()) {

            Toast.makeText(
                    AddReportActivity.this,
                    "Unable to load this draft because your login session is missing.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        setReportActionButtonsEnabled(
                false
        );

        incidentReportAPI
                .getReportDetails(
                        "Bearer " + accessToken,
                        "eq." + draftId,
                        "eq." + userId,
                        "id,case_number,user_id,barangay_id,"
                                + "incident_type,incident_subtype,incident_date,incident_time,"
                                + "incident_barangay,incident_municipality,incident_address,"
                                + "description,report_status,reporter_type,reporter_name,"
                                + "reporter_relationship,reporter_contact,victim_name,"
                                + "victim_phone,victim_email,victim_address,"
                                + "incident_place_type,incident_place_other,"
                                + "involved_electronic_means,electronic_platform"
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

                                    setReportActionButtonsEnabled(
                                            true
                                    );

                                    Toast.makeText(
                                            AddReportActivity.this,
                                            "Unable to load this draft.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }

                                Map<String, Object> draft =
                                        response.body().get(0);

                                String status =
                                        getDraftMapString(
                                                draft,
                                                "report_status"
                                        )
                                                .trim()
                                                .toLowerCase(Locale.US);

                                if (!status.equals(
                                        "draft"
                                )) {

                                    setReportActionButtonsEnabled(
                                            true
                                    );

                                    Toast.makeText(
                                            AddReportActivity.this,
                                            "This report is no longer a draft.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    finish();
                                    return;
                                }

                                populateDraftForm(
                                        draft
                                );

                                loadDraftPerpetrators(
                                        draftId,
                                        accessToken
                                );

                                loadDraftAttachments(
                                        draftId,
                                        accessToken
                                );
                            }

                            @Override
                            public void onFailure(
                                    Call<List<Map<String, Object>>> call,
                                    Throwable t
                            ) {

                                setReportActionButtonsEnabled(
                                        true
                                );

                                Toast.makeText(
                                        AddReportActivity.this,
                                        "Unable to load this draft. Check your connection.",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }


    private void populateDraftForm(
            Map<String, Object> draft
    ) {

        incidentTypeDropdown.setText(
                getDraftMapString(draft, "incident_type"),
                false
        );

        incidentSubtypeInput.setText(
                getDraftMapString(draft, "incident_subtype")
        );

        incidentDateInput.setText(
                formatDraftDateForDisplay(
                        getDraftMapString(draft, "incident_date")
                )
        );

        incidentTimeInput.setText(
                formatDraftTimeForDisplay(
                        getDraftMapString(draft, "incident_time")
                )
        );

        incidentBarangayInput.setText(
                getDraftMapString(draft, "incident_barangay")
        );

        incidentMunicipalityInput.setText(
                getDraftMapString(draft, "incident_municipality")
        );

        incidentAddressInput.setText(
                getDraftMapString(draft, "incident_address")
        );

        descriptionInput.setText(
                getDraftMapString(draft, "description")
        );


        // REPORTER TYPE

        String reporterTypeDisplay =
                mapReporterTypeForDisplay(
                        getDraftMapString(
                                draft,
                                "reporter_type"
                        )
                );

        reporterTypeDropdown.setText(
                reporterTypeDisplay,
                false
        );

        applyLoadedReporterTypeVisibility(
                reporterTypeDisplay
        );

        reporterNameInput.setText(
                getDraftMapString(draft, "reporter_name")
        );

        reporterContactInput.setText(
                getDraftMapString(draft, "reporter_contact")
        );

        applyLoadedReporterRelationship(
                getDraftMapString(
                        draft,
                        "reporter_relationship"
                )
        );


        // VICTIM INFORMATION

        victimFullNameInput.setText(
                getDraftMapString(draft, "victim_name")
        );

        victimPhoneInput.setText(
                getDraftMapString(draft, "victim_phone")
        );

        victimEmailInput.setText(
                getDraftMapString(draft, "victim_email")
        );

        victimAddressInput.setText(
                getDraftMapString(draft, "victim_address")
        );


        // PLACE TYPE

        String placeTypeDisplay =
                mapPlaceTypeForDisplay(
                        getDraftMapString(
                                draft,
                                "incident_place_type"
                        )
                );

        placeTypeDropdown.setText(
                placeTypeDisplay,
                false
        );

        if (placeTypeDisplay.equalsIgnoreCase(
                "Other"
        )) {

            otherPlaceLayout.setVisibility(
                    View.VISIBLE
            );

            otherPlaceInput.setText(
                    getDraftMapString(
                            draft,
                            "incident_place_other"
                    )
            );

        } else {

            otherPlaceLayout.setVisibility(
                    View.GONE
            );

            otherPlaceInput.setText(
                    ""
            );
        }


        // ELECTRONIC / ONLINE INVOLVEMENT

        boolean involvedElectronicMeans =
                getDraftMapBoolean(
                        draft,
                        "involved_electronic_means"
                );

        if (involvedElectronicMeans) {

            electronicMeansRadioGroup.check(
                    R.id.radioElectronicYes
            );

            String electronicPlatform =
                    getDraftMapString(
                            draft,
                            "electronic_platform"
                    );

            applyLoadedElectronicPlatform(
                    electronicPlatform
            );

        } else {

            electronicMeansRadioGroup.check(
                    R.id.radioElectronicNo
            );
        }

        addReportScrollView.post(
                () -> addReportScrollView.scrollTo(0, 0)
        );
    }


    private void loadDraftPerpetrators(
            String draftId,
            String accessToken
    ) {

        perpetratorAPI
                .getPerpetratorByReportId(
                        "Bearer " + accessToken,
                        "eq." + draftId,
                        "id,report_id,full_name,age,sex,occupation,relationship_to_victim,address",
                        "100"
                )
                .enqueue(
                        new Callback<List<Map<String, Object>>>() {

                            @Override
                            public void onResponse(
                                    Call<List<Map<String, Object>>> call,
                                    Response<List<Map<String, Object>>> response
                            ) {

                                if (response.isSuccessful()
                                        && response.body() != null) {

                                    populateDraftPerpetrators(
                                            response.body()
                                    );
                                }

                                // IMPORTANT FOR THIS STEP:
                                // the draft can now be reviewed and edited on screen,
                                // but updating the existing database row is connected next.
                                setReportActionButtonsEnabled(
                                        true
                                );
                            }

                            @Override
                            public void onFailure(
                                    Call<List<Map<String, Object>>> call,
                                    Throwable t
                            ) {

                                // Main draft fields are still usable even if the
                                // perpetrator request temporarily fails.
                                setReportActionButtonsEnabled(
                                        true
                                );
                            }
                        }
                );
    }


    private void populateDraftPerpetrators(
            List<Map<String, Object>> perpetrators
    ) {

        int count =
                perpetrators == null
                        ? 0
                        : perpetrators.size();

        perpetratorCountInput.setText(
                count == 0
                        ? ""
                        : String.valueOf(count)
        );

        if (count == 0) {
            return;
        }

        for (int index = 0;
             index < count
                     && index < perpetratorEntries.size();
             index++) {

            Map<String, Object> perpetrator =
                    perpetrators.get(index);

            PerpetratorEntry entry =
                    perpetratorEntries.get(index);

            entry.fullNameInput.setText(
                    getDraftMapString(
                            perpetrator,
                            "full_name"
                    )
            );

            entry.ageInput.setText(
                    getDraftNumericText(
                            perpetrator.get(
                                    "age"
                            )
                    )
            );

            entry.sexDropdown.setText(
                    getDraftMapString(
                            perpetrator,
                            "sex"
                    ),
                    false
            );

            entry.occupationInput.setText(
                    getDraftMapString(
                            perpetrator,
                            "occupation"
                    )
            );

            entry.addressInput.setText(
                    getDraftMapString(
                            perpetrator,
                            "address"
                    )
            );

            applyLoadedPerpetratorRelationship(
                    entry,
                    getDraftMapString(
                            perpetrator,
                            "relationship_to_victim"
                    )
            );
        }
    }


    // =====================================================
    // LOAD / MANAGE EXISTING DRAFT ATTACHMENTS
    // =====================================================

    private void loadDraftAttachments(
            String draftId,
            String accessToken
    ) {

        reportAttachmentAPI
                .getReportAttachments(
                        "Bearer " + accessToken,
                        "eq." + draftId,
                        "id,report_id,file_url,file_type,file_name,mime_type,file_size,uploaded_at",
                        "uploaded_at.asc"
                )
                .enqueue(
                        new Callback<List<Map<String, Object>>>() {

                            @Override
                            public void onResponse(
                                    Call<List<Map<String, Object>>> call,
                                    Response<List<Map<String, Object>>> response
                            ) {

                                if (!response.isSuccessful()
                                        || response.body() == null) {

                                    return;
                                }

                                /*
                                 * Preserve newly selected local attachments if the
                                 * user somehow picked one before this request finished.
                                 * Remove only previously loaded persisted entries first.
                                 */
                                for (int index =
                                     selectedAttachments.size() - 1;
                                     index >= 0;
                                     index--) {

                                    if (selectedAttachments
                                            .get(index)
                                            .persistedFromDraft) {

                                        selectedAttachments.remove(
                                                index
                                        );
                                    }
                                }

                                for (Map<String, Object> attachment
                                        : response.body()) {

                                    String metadataId =
                                            getDraftMapString(
                                                    attachment,
                                                    "id"
                                            );

                                    String storagePath =
                                            getDraftMapString(
                                                    attachment,
                                                    "file_url"
                                            );

                                    String fileName =
                                            getDraftMapString(
                                                    attachment,
                                                    "file_name"
                                            );

                                    String fileType =
                                            getDraftMapString(
                                                    attachment,
                                                    "file_type"
                                            );

                                    String mimeType =
                                            getDraftMapString(
                                                    attachment,
                                                    "mime_type"
                                            );

                                    long fileSize =
                                            getDraftMapLong(
                                                    attachment.get(
                                                            "file_size"
                                                    )
                                            );

                                    AttachmentItem item =
                                            new AttachmentItem(
                                                    null,
                                                    fileName.isEmpty()
                                                            ? "Attachment"
                                                            : fileName,
                                                    mimeType.isEmpty()
                                                            ? "application/octet-stream"
                                                            : mimeType,
                                                    fileType.isEmpty()
                                                            ? "document"
                                                            : fileType,
                                                    fileSize
                                            );

                                    item.metadataId =
                                            metadataId;

                                    item.storagePath =
                                            storagePath;

                                    /*
                                     * These were already uploaded and recorded
                                     * during the original draft save.
                                     */
                                    item.storageUploaded = true;
                                    item.metadataSaved = true;
                                    item.persistedFromDraft = true;

                                    selectedAttachments.add(
                                            item
                                    );
                                }

                                renderSelectedAttachments();
                            }

                            @Override
                            public void onFailure(
                                    Call<List<Map<String, Object>>> call,
                                    Throwable t
                            ) {
                                // Keep the rest of the reopened draft usable.
                            }
                        }
                );
    }


    private long getDraftMapLong(
            Object value
    ) {

        if (value == null) {
            return -1L;
        }

        if (value instanceof Number) {

            return ((Number) value)
                    .longValue();
        }

        try {

            return Long.parseLong(
                    String.valueOf(value)
                            .trim()
            );

        } catch (Exception ignored) {

            return -1L;
        }
    }


    private void removePersistedDraftAttachment(
            AttachmentItem item
    ) {

        if (item == null
                || !item.persistedFromDraft) {

            return;
        }

        if (editingDraftId == null
                || editingDraftId.trim().isEmpty()) {

            showRedToast(
                    "This saved attachment can only be removed while editing a draft."
            );

            return;
        }

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

        if (accessToken == null
                || accessToken.trim().isEmpty()) {

            showRedToast(
                    "Your login session is missing. Please log in again."
            );

            return;
        }

        if (item.storagePath == null
                || item.storagePath.trim().isEmpty()
                || item.metadataId == null
                || item.metadataId.trim().isEmpty()) {

            showRedToast(
                    "Unable to identify this saved attachment."
            );

            return;
        }

        setReportActionButtonsEnabled(
                false
        );

        /*
         * IMPORTANT ORDER:
         * Delete the Storage object FIRST.
         *
         * The Storage DELETE RLS policy below verifies the matching
         * report_attachments row. If metadata were deleted first,
         * Storage could no longer prove that this file belongs to
         * the user's draft.
         */
        incidentAttachmentStorageAPI
                .deleteIncidentAttachment(
                        "Bearer " + accessToken,
                        item.storagePath
                )
                .enqueue(
                        new Callback<Void>() {

                            @Override
                            public void onResponse(
                                    Call<Void> call,
                                    Response<Void> response
                            ) {

                                if (!response.isSuccessful()) {

                                    setReportActionButtonsEnabled(
                                            true
                                    );

                                    showRedToast(
                                            "Unable to remove the saved attachment file."
                                    );

                                    return;
                                }

                                deleteDraftAttachmentMetadata(
                                        accessToken,
                                        item
                                );
                            }

                            @Override
                            public void onFailure(
                                    Call<Void> call,
                                    Throwable t
                            ) {

                                setReportActionButtonsEnabled(
                                        true
                                );

                                showRedToast(
                                        "Unable to remove the saved attachment. Check your connection."
                                );
                            }
                        }
                );
    }


    private void deleteDraftAttachmentMetadata(
            String accessToken,
            AttachmentItem item
    ) {

        reportAttachmentAPI
                .deleteReportAttachment(
                        "Bearer " + accessToken,
                        "eq." + item.metadataId,
                        "eq." + editingDraftId
                )
                .enqueue(
                        new Callback<Void>() {

                            @Override
                            public void onResponse(
                                    Call<Void> call,
                                    Response<Void> response
                            ) {

                                setReportActionButtonsEnabled(
                                        true
                                );

                                if (!response.isSuccessful()) {

                                    showRedToast(
                                            "The file was removed, but its attachment record could not be deleted."
                                    );

                                    return;
                                }

                                selectedAttachments.remove(
                                        item
                                );

                                renderSelectedAttachments();

                                Toast.makeText(
                                        AddReportActivity.this,
                                        "Attachment removed.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }

                            @Override
                            public void onFailure(
                                    Call<Void> call,
                                    Throwable t
                            ) {

                                setReportActionButtonsEnabled(
                                        true
                                );

                                showRedToast(
                                        "The file was removed, but its attachment record could not be deleted."
                                );
                            }
                        }
                );
    }


    private void applyLoadedReporterTypeVisibility(
            String reporterTypeDisplay
    ) {

        if (reporterTypeDisplay.equalsIgnoreCase(
                "Victim"
        )) {

            reporterExtraFields.setVisibility(
                    View.GONE
            );

            victimInformationCard.setVisibility(
                    View.GONE
            );

        } else if (reporterTypeDisplay.equalsIgnoreCase(
                "Companion"
        ) || reporterTypeDisplay.equalsIgnoreCase(
                "Informant"
        )) {

            reporterExtraFields.setVisibility(
                    View.VISIBLE
            );

            victimInformationCard.setVisibility(
                    View.VISIBLE
            );

        } else {

            reporterExtraFields.setVisibility(
                    View.GONE
            );

            victimInformationCard.setVisibility(
                    View.GONE
            );
        }
    }


    private void applyLoadedReporterRelationship(
            String relationship
    ) {

        if (relationship == null
                || relationship.trim().isEmpty()) {

            reporterRelationshipDropdown.setText(
                    "",
                    false
            );

            otherReporterRelationshipLayout.setVisibility(
                    View.GONE
            );

            otherReporterRelationshipInput.setText(
                    ""
            );

            return;
        }

        if (isKnownRelationship(
                relationship
        )) {

            reporterRelationshipDropdown.setText(
                    relationship,
                    false
            );

            otherReporterRelationshipLayout.setVisibility(
                    View.GONE
            );

            otherReporterRelationshipInput.setText(
                    ""
            );

        } else {

            reporterRelationshipDropdown.setText(
                    "Other",
                    false
            );

            otherReporterRelationshipLayout.setVisibility(
                    View.VISIBLE
            );

            otherReporterRelationshipInput.setText(
                    relationship
            );
        }
    }


    private void applyLoadedPerpetratorRelationship(
            PerpetratorEntry entry,
            String relationship
    ) {

        if (relationship == null
                || relationship.trim().isEmpty()) {

            entry.relationshipDropdown.setText(
                    "",
                    false
            );

            entry.otherRelationshipLayout.setVisibility(
                    View.GONE
            );

            entry.otherRelationshipInput.setText(
                    ""
            );

            return;
        }

        if (isKnownRelationship(
                relationship
        )) {

            entry.relationshipDropdown.setText(
                    relationship,
                    false
            );

            entry.otherRelationshipLayout.setVisibility(
                    View.GONE
            );

            entry.otherRelationshipInput.setText(
                    ""
            );

        } else {

            entry.relationshipDropdown.setText(
                    "Other",
                    false
            );

            entry.otherRelationshipLayout.setVisibility(
                    View.VISIBLE
            );

            entry.otherRelationshipInput.setText(
                    relationship
            );
        }
    }


    private boolean isKnownRelationship(
            String relationship
    ) {

        String[] knownRelationships = {
                "Spouse",
                "Former Spouse",
                "Live-in Partner",
                "Former Live-in Partner",
                "Dating Partner",
                "Former Dating Partner",
                "Family Member",
                "Friend",
                "Neighbor"
        };

        for (String known : knownRelationships) {

            if (known.equalsIgnoreCase(
                    relationship.trim()
            )) {

                return true;
            }
        }

        return false;
    }


    private void applyLoadedElectronicPlatform(
            String platform
    ) {

        if (platform == null
                || platform.trim().isEmpty()) {

            electronicPlatformDropdown.setText(
                    "",
                    false
            );

            otherElectronicPlatformLayout.setVisibility(
                    View.GONE
            );

            otherElectronicPlatformInput.setText(
                    ""
            );

            return;
        }

        String[] knownPlatforms = {
                "Facebook",
                "Messenger",
                "Instagram",
                "TikTok",
                "SMS / Text Message",
                "Phone Call",
                "Email"
        };

        boolean known = false;

        for (String item : knownPlatforms) {

            if (item.equalsIgnoreCase(
                    platform.trim()
            )) {

                known = true;
                break;
            }
        }

        if (known) {

            electronicPlatformDropdown.setText(
                    platform,
                    false
            );

            otherElectronicPlatformLayout.setVisibility(
                    View.GONE
            );

            otherElectronicPlatformInput.setText(
                    ""
            );

        } else {

            electronicPlatformDropdown.setText(
                    "Other",
                    false
            );

            otherElectronicPlatformLayout.setVisibility(
                    View.VISIBLE
            );

            otherElectronicPlatformInput.setText(
                    platform
            );
        }
    }


    private String mapReporterTypeForDisplay(
            String value
    ) {

        if (value == null) {
            return "";
        }

        switch (value.trim().toLowerCase(Locale.US)) {

            case "victim":
                return "Victim";

            case "companion":
                return "Companion";

            case "informant":
                return "Informant";

            default:
                return "";
        }
    }


    private String mapPlaceTypeForDisplay(
            String value
    ) {

        if (value == null) {
            return "";
        }

        switch (value.trim().toLowerCase(Locale.US)) {

            case "victim_home":
                return "Victim Home";

            case "perpetrator_home":
                return "Perpetrator Home";

            case "conjugal_home":
                return "Conjugal Home";

            case "school":
                return "School";

            case "workplace":
                return "Workplace";

            case "public_transport":
                return "Public Transport";

            case "public_place":
                return "Public Place";

            case "evacuation_area":
                return "Evacuation Area";

            case "mall_or_hotel":
                return "Mall / Hotel";

            case "other":
                return "Other";

            default:
                return "";
        }
    }


    private String formatDraftDateForDisplay(
            String databaseDate
    ) {

        if (databaseDate == null
                || databaseDate.trim().isEmpty()) {

            return "";
        }

        try {

            SimpleDateFormat databaseFormat =
                    new SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.US
                    );

            SimpleDateFormat displayFormat =
                    new SimpleDateFormat(
                            "MMMM d, yyyy",
                            Locale.getDefault()
                    );

            databaseFormat.setLenient(
                    false
            );

            return displayFormat.format(
                    databaseFormat.parse(
                            databaseDate.trim()
                    )
            );

        } catch (Exception ignored) {

            return databaseDate;
        }
    }


    private String formatDraftTimeForDisplay(
            String databaseTime
    ) {

        if (databaseTime == null
                || databaseTime.trim().isEmpty()) {

            return "";
        }

        String clean =
                databaseTime.trim();

        int dotIndex =
                clean.indexOf('.');

        if (dotIndex > 0) {
            clean = clean.substring(0, dotIndex);
        }

        try {

            SimpleDateFormat databaseFormat =
                    new SimpleDateFormat(
                            "HH:mm:ss",
                            Locale.US
                    );

            SimpleDateFormat displayFormat =
                    new SimpleDateFormat(
                            "h:mm a",
                            Locale.getDefault()
                    );

            databaseFormat.setLenient(
                    false
            );

            return displayFormat.format(
                    databaseFormat.parse(
                            clean
                    )
            );

        } catch (Exception ignored) {

            return databaseTime;
        }
    }


    private String getDraftMapString(
            Map<String, Object> data,
            String key
    ) {

        if (data == null) {
            return "";
        }

        Object value =
                data.get(
                        key
                );

        if (value == null) {
            return "";
        }

        String text =
                String.valueOf(
                        value
                ).trim();

        if (text.equalsIgnoreCase(
                "null"
        )) {

            return "";
        }

        return text;
    }


    private boolean getDraftMapBoolean(
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

        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        if (value == null) {
            return false;
        }

        return Boolean.parseBoolean(
                String.valueOf(value)
        );
    }


    private String getDraftNumericText(
            Object value
    ) {

        if (value == null) {
            return "";
        }

        if (value instanceof Number) {

            double number =
                    ((Number) value).doubleValue();

            if (number == Math.rint(number)) {

                return String.valueOf(
                        (long) number
                );
            }
        }

        String text =
                String.valueOf(
                        value
                ).trim();

        if (text.endsWith(
                ".0"
        )) {

            return text.substring(
                    0,
                    text.length() - 2
            );
        }

        return text;
    }


    // =====================================================
    // INITIALIZE VIEWS
    // =====================================================

    private void initializeViews() {

        // INCIDENT

        incidentTypeDropdown =
                findViewById(R.id.incidentTypeDropdown);

        incidentSubtypeInput =
                findViewById(R.id.incidentSubtypeInput);

        incidentDateInput =
                findViewById(R.id.incidentDateInput);

        incidentTimeInput =
                findViewById(R.id.incidentTimeInput);


        // VICTIM

        victimInformationCard =
                findViewById(R.id.victimInformationCard);

        victimFullNameInput =
                findViewById(R.id.victimFullNameInput);

        victimPhoneInput =
                findViewById(R.id.victimPhoneInput);

        victimEmailInput =
                findViewById(R.id.victimEmailInput);

        victimAddressInput =
                findViewById(R.id.victimAddressInput);


        // REPORTER

        reporterTypeDropdown =
                findViewById(R.id.reporterTypeDropdown);

        reporterExtraFields =
                findViewById(R.id.reporterExtraFields);

        reporterNameInput =
                findViewById(R.id.reporterNameInput);

        reporterRelationshipDropdown =
                findViewById(R.id.reporterRelationshipDropdown);

        otherReporterRelationshipLayout =
                findViewById(R.id.otherReporterRelationshipLayout);

        otherReporterRelationshipInput =
                findViewById(R.id.otherReporterRelationshipInput);

        reporterContactInput =
                findViewById(R.id.reporterContactInput);


        // PERPETRATOR

        perpetratorCountInput =
                findViewById(
                        R.id.perpetratorCountInput
                );

        perpetratorEntriesContainer =
                findViewById(
                        R.id.perpetratorEntriesContainer
                );


        // LOCATION

        placeTypeDropdown =
                findViewById(R.id.placeTypeDropdown);

        otherPlaceLayout =
                findViewById(R.id.otherPlaceLayout);

        otherPlaceInput =
                findViewById(R.id.otherPlaceInput);

        incidentAddressInput =
                findViewById(R.id.incidentAddressInput);

        incidentBarangayInput =
                findViewById(R.id.incidentBarangayInput);

        incidentMunicipalityInput =
                findViewById(R.id.incidentMunicipalityInput);


        // ELECTRONIC

        electronicMeansRadioGroup =
                findViewById(R.id.electronicMeansRadioGroup);

        electronicPlatformLayout =
                findViewById(R.id.electronicPlatformLayout);

        electronicPlatformDropdown =
                findViewById(R.id.electronicPlatformDropdown);

        otherElectronicPlatformLayout =
                findViewById(R.id.otherElectronicPlatformLayout);

        otherElectronicPlatformInput =
                findViewById(R.id.otherElectronicPlatformInput);


        // DESCRIPTION

        descriptionInput =
                findViewById(R.id.descriptionInput);

        descriptionCounter =
                findViewById(R.id.descriptionCounter);


        // ATTACHMENTS

        attachmentPickerArea =
                findViewById(R.id.attachmentPickerArea);

        attachmentsListContainer =
                findViewById(R.id.attachmentsListContainer);


        // FORM SCROLL

        addReportScrollView =
                findViewById(
                        R.id.addReportScrollView
                );


        // BUTTONS

        saveDraftButton =
                findViewById(R.id.saveDraftButton);

        reviewReportButton =
                findViewById(R.id.reviewReportButton);
    }


    // =====================================================
    // BACK
    // =====================================================

    private void setupBackButton() {

        findViewById(R.id.backButton)
                .setOnClickListener(v -> finish());
    }


    // =====================================================
    // DROPDOWNS
    // =====================================================

    private void setupDropdowns() {

        String[] incidentTypes = {
                "Physical Violence",
                "Sexual Violence",
                "Psychological Violence",
                "Economic Abuse"
        };



        String[] reporterTypes = {
                "Victim",
                "Companion",
                "Informant"
        };


        String[] relationships = {
                "Spouse",
                "Former Spouse",
                "Live-in Partner",
                "Former Live-in Partner",
                "Dating Partner",
                "Former Dating Partner",
                "Family Member",
                "Friend",
                "Neighbor",
                "Other"
        };

        String[] placeTypes = {
                "Victim Home",
                "Perpetrator Home",
                "Conjugal Home",
                "School",
                "Workplace",
                "Public Transport",
                "Public Place",
                "Evacuation Area",
                "Mall / Hotel",
                "Other"
        };


        String[] platforms = {
                "Facebook",
                "Messenger",
                "Instagram",
                "TikTok",
                "SMS / Text Message",
                "Phone Call",
                "Email",
                "Other"
        };



        setDropdownItems(
                incidentTypeDropdown,
                incidentTypes
        );

        setDropdownItems(
                reporterTypeDropdown,
                reporterTypes
        );

        setDropdownItems(
                reporterRelationshipDropdown,
                relationships
        );

        setDropdownItems(
                placeTypeDropdown,
                placeTypes
        );

        setDropdownItems(
                electronicPlatformDropdown,
                platforms
        );

    }


    // =====================================================
    // "OTHER" DROPDOWN BEHAVIOR
    // =====================================================

    private void setupOtherDropdownBehaviors() {

        otherReporterRelationshipLayout.setVisibility(
                View.GONE
        );

        otherElectronicPlatformLayout.setVisibility(
                View.GONE
        );


        reporterRelationshipDropdown
                .setOnItemClickListener(
                        (parent, view, position, id) -> {

                            boolean isOther =
                                    reporterRelationshipDropdown
                                            .getText()
                                            .toString()
                                            .trim()
                                            .equalsIgnoreCase("Other");

                            otherReporterRelationshipLayout
                                    .setVisibility(
                                            isOther
                                                    ? View.VISIBLE
                                                    : View.GONE
                                    );

                            if (!isOther) {
                                otherReporterRelationshipInput.setText("");
                            }
                        }
                );


        electronicPlatformDropdown
                .setOnItemClickListener(
                        (parent, view, position, id) -> {

                            boolean isOther =
                                    electronicPlatformDropdown
                                            .getText()
                                            .toString()
                                            .trim()
                                            .equalsIgnoreCase("Other");

                            otherElectronicPlatformLayout
                                    .setVisibility(
                                            isOther
                                                    ? View.VISIBLE
                                                    : View.GONE
                                    );

                            if (!isOther) {
                                otherElectronicPlatformInput.setText("");
                            }
                        }
                );
    }


    private void setDropdownItems(
            AutoCompleteTextView dropdown,
            String[] items
    ) {

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        R.layout.dropdown_item_compact,
                        items
                );

        dropdown.setAdapter(adapter);
    }
    // =====================================================
    // DATE
    // =====================================================

    private void setupDatePicker() {

        incidentDateInput.setFocusable(false);

        incidentDateInput.setOnClickListener(v -> {

            DatePickerDialog dialog =
                    new DatePickerDialog(
                            AddReportActivity.this,
                            (view, year, month, dayOfMonth) -> {

                                selectedDateTime.set(
                                        Calendar.YEAR,
                                        year
                                );

                                selectedDateTime.set(
                                        Calendar.MONTH,
                                        month
                                );

                                selectedDateTime.set(
                                        Calendar.DAY_OF_MONTH,
                                        dayOfMonth
                                );

                                SimpleDateFormat formatter =
                                        new SimpleDateFormat(
                                                "MMMM d, yyyy",
                                                Locale.getDefault()
                                        );

                                incidentDateInput.setText(
                                        formatter.format(
                                                selectedDateTime.getTime()
                                        )
                                );
                            },

                            selectedDateTime.get(
                                    Calendar.YEAR
                            ),

                            selectedDateTime.get(
                                    Calendar.MONTH
                            ),

                            selectedDateTime.get(
                                    Calendar.DAY_OF_MONTH
                            )
                    );

            dialog.show();
        });
    }


    // =====================================================
    // TIME
    // =====================================================

    private void setupTimePicker() {

        incidentTimeInput.setFocusable(false);

        incidentTimeInput.setOnClickListener(v -> {

            TimePickerDialog dialog =
                    new TimePickerDialog(
                            AddReportActivity.this,
                            (view, hourOfDay, minute) -> {

                                selectedDateTime.set(
                                        Calendar.HOUR_OF_DAY,
                                        hourOfDay
                                );

                                selectedDateTime.set(
                                        Calendar.MINUTE,
                                        minute
                                );

                                SimpleDateFormat formatter =
                                        new SimpleDateFormat(
                                                "h:mm a",
                                                Locale.getDefault()
                                        );

                                incidentTimeInput.setText(
                                        formatter.format(
                                                selectedDateTime.getTime()
                                        )
                                );
                            },

                            selectedDateTime.get(
                                    Calendar.HOUR_OF_DAY
                            ),

                            selectedDateTime.get(
                                    Calendar.MINUTE
                            ),

                            false
                    );

            dialog.show();
        });
    }


    // =====================================================
    // REPORTER TYPE
    // =====================================================

    private void setupReporterTypeBehavior() {

        // Nothing selected initially.
        reporterExtraFields.setVisibility(
                View.GONE
        );

        // Victim information is hidden until the reporter type
        // tells us whether a separate victim form is needed.
        victimInformationCard.setVisibility(
                View.GONE
        );

        reporterTypeDropdown
                .setOnItemClickListener(
                        (parent, view, position, id) -> {

                            String reporterType =
                                    reporterTypeDropdown
                                            .getText()
                                            .toString()
                                            .trim();

                            if (reporterType.equalsIgnoreCase(
                                    "Victim"
                            )) {

                                // Reporter is the victim:
                                // no separate reporter details and no
                                // duplicate victim-information form.
                                reporterExtraFields.setVisibility(
                                        View.GONE
                                );

                                victimInformationCard.setVisibility(
                                        View.GONE
                                );

                                reporterNameInput.setText("");

                                reporterRelationshipDropdown.setText(
                                        "",
                                        false
                                );

                                otherReporterRelationshipLayout
                                        .setVisibility(
                                                View.GONE
                                        );

                                otherReporterRelationshipInput.setText("");

                                reporterContactInput.setText("");

                            } else if (reporterType.equalsIgnoreCase(
                                    "Companion"
                            ) || reporterType.equalsIgnoreCase(
                                    "Informant"
                            )) {

                                // Reporter is someone other than the victim:
                                // collect reporter details first, then victim details.
                                reporterExtraFields.setVisibility(
                                        View.VISIBLE
                                );

                                victimInformationCard.setVisibility(
                                        View.VISIBLE
                                );

                            } else {

                                reporterExtraFields.setVisibility(
                                        View.GONE
                                );

                                victimInformationCard.setVisibility(
                                        View.GONE
                                );
                            }
                        }
                );
    }

    // =====================================================
    // DYNAMIC PERPETRATOR COUNT
    // =====================================================

    private void setupPerpetratorCountBehavior() {

        perpetratorCountInput.addTextChangedListener(
                new android.text.TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        String value =
                                s.toString().trim();

                        if (value.isEmpty()) {
                            rebuildPerpetratorEntries(0);
                            return;
                        }

                        try {

                            int number =
                                    Integer.parseInt(value);

                            if (number < 0) {
                                rebuildPerpetratorEntries(0);
                                return;
                            }

                            rebuildPerpetratorEntries(number);

                        } catch (NumberFormatException e) {

                            rebuildPerpetratorEntries(0);
                        }
                    }

                    @Override
                    public void afterTextChanged(
                            android.text.Editable s
                    ) {
                    }
                }
        );
    }


    private void rebuildPerpetratorEntries(
            int count
    ) {

        perpetratorEntries.clear();
        perpetratorEntriesContainer.removeAllViews();

        pendingPerpetratorPayloads.clear();
        nextPerpetratorIndexToSubmit = 0;

        for (int index = 0; index < count; index++) {

            addPerpetratorEntry(index + 1);
        }
    }


    private void addPerpetratorEntry(
            int number
    ) {

        View entryView =
                LayoutInflater
                        .from(this)
                        .inflate(
                                R.layout.perpetrator_entry_item,
                                perpetratorEntriesContainer,
                                false
                        );

        PerpetratorEntry entry =
                new PerpetratorEntry();

        entry.root = entryView;
        entry.title =
                entryView.findViewById(
                        R.id.textPerpetratorEntryTitle
                );
        entry.fullNameInput =
                entryView.findViewById(
                        R.id.perpetratorEntryNameInput
                );
        entry.relationshipDropdown =
                entryView.findViewById(
                        R.id.perpetratorEntryRelationshipDropdown
                );
        entry.otherRelationshipLayout =
                entryView.findViewById(
                        R.id.perpetratorEntryOtherRelationshipLayout
                );
        entry.otherRelationshipInput =
                entryView.findViewById(
                        R.id.perpetratorEntryOtherRelationshipInput
                );
        entry.ageInput =
                entryView.findViewById(
                        R.id.perpetratorEntryAgeInput
                );
        entry.sexDropdown =
                entryView.findViewById(
                        R.id.perpetratorEntrySexDropdown
                );
        entry.occupationInput =
                entryView.findViewById(
                        R.id.perpetratorEntryOccupationInput
                );
        entry.addressInput =
                entryView.findViewById(
                        R.id.perpetratorEntryAddressInput
                );

        applyPerpetratorInputRules(entry);

        entry.title.setText(
                "Perpetrator " + number
        );

        String[] relationships = {
                "Spouse",
                "Former Spouse",
                "Live-in Partner",
                "Former Live-in Partner",
                "Dating Partner",
                "Former Dating Partner",
                "Family Member",
                "Friend",
                "Neighbor",
                "Other"
        };

        String[] sexOptions = {
                "Male",
                "Female"
        };

        setDropdownItems(
                entry.relationshipDropdown,
                relationships
        );

        setDropdownItems(
                entry.sexDropdown,
                sexOptions
        );

        entry.otherRelationshipLayout.setVisibility(
                View.GONE
        );

        entry.relationshipDropdown
                .setOnItemClickListener(
                        (parent, view, position, id) -> {

                            boolean isOther =
                                    entry.relationshipDropdown
                                            .getText()
                                            .toString()
                                            .trim()
                                            .equalsIgnoreCase("Other");

                            entry.otherRelationshipLayout
                                    .setVisibility(
                                            isOther
                                                    ? View.VISIBLE
                                                    : View.GONE
                                    );

                            if (!isOther) {
                                entry.otherRelationshipInput.setText("");
                            }
                        }
                );

        perpetratorEntries.add(entry);
        perpetratorEntriesContainer.addView(entryView);
    }


    // =====================================================
    // PLACE TYPE
    // =====================================================

    private void setupPlaceTypeBehavior() {

        otherPlaceLayout.setVisibility(
                View.GONE
        );

        placeTypeDropdown
                .setOnItemClickListener(
                        (parent, view, position, id) -> {

                            String placeType =
                                    placeTypeDropdown
                                            .getText()
                                            .toString()
                                            .trim();

                            if (placeType.equalsIgnoreCase(
                                    "Other"
                            )) {

                                otherPlaceLayout.setVisibility(
                                        View.VISIBLE
                                );

                            } else {

                                otherPlaceLayout.setVisibility(
                                        View.GONE
                                );

                                otherPlaceInput.setText("");
                            }
                        }
                );
    }


    // =====================================================
    // ELECTRONIC INVOLVEMENT
    // =====================================================

    private void setupElectronicMeansBehavior() {

        electronicPlatformLayout.setVisibility(
                View.GONE
        );

        electronicMeansRadioGroup
                .setOnCheckedChangeListener(
                        (group, checkedId) -> {

                            if (checkedId ==
                                    R.id.radioElectronicYes) {

                                electronicPlatformLayout
                                        .setVisibility(
                                                View.VISIBLE
                                        );

                            } else {

                                electronicPlatformLayout
                                        .setVisibility(
                                                View.GONE
                                        );

                                electronicPlatformDropdown
                                        .setText(
                                                "",
                                                false
                                        );

                                otherElectronicPlatformLayout
                                        .setVisibility(
                                                View.GONE
                                        );

                                otherElectronicPlatformInput.setText("");
                            }
                        }
                );
    }



    // =====================================================
    // LOAD VICTIM INFORMATION
    // =====================================================

    private void loadVictimInformation() {

        // Do not copy the logged-in account into Victim Information.
        // Companion/Informant reporters may be reporting for another person.
        clearVictimInformation();
    }

    private void clearVictimInformation() {

        victimFullNameInput.setText("");
        victimPhoneInput.setText("");
        victimEmailInput.setText("");
        victimAddressInput.setText("");
    }


    // =====================================================
    // DESCRIPTION COUNTER
    // =====================================================

    private void setupDescriptionCounter() {

        descriptionInput.addTextChangedListener(
                new android.text.TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        descriptionCounter.setText(
                                s.length() + "/1000"
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            android.text.Editable s
                    ) {
                    }
                }
        );
    }


    // =====================================================
    // BUTTONS
    // =====================================================

    private void setupButtons() {

        attachmentPickerArea
                .setOnClickListener(v -> openAttachmentPicker());

        saveDraftButton
                .setOnClickListener(v -> saveDraftReport());


        reviewReportButton
                .setOnClickListener(v -> {

                    cleanFormInputValues();

                    if (!validateRequiredFields()) {
                        return;
                    }

                    submitIncidentReport();
                });
    }


    // =====================================================
    // ENABLE / DISABLE REPORT ACTION BUTTONS
    // =====================================================

    private void setReportActionButtonsEnabled(
            boolean enabled
    ) {

        saveDraftButton.setEnabled(
                enabled
        );

        reviewReportButton.setEnabled(
                enabled
        );
    }


    // =====================================================
    // SAVE DRAFT TO SUPABASE
    //
    // No required-field validation is used here because a draft is
    // intentionally allowed to be incomplete.
    // =====================================================

    private void saveDraftReport() {

        // Drafts may stay incomplete, but any entered values are cleaned
        // before they are stored.
        cleanFormInputValues();

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

        String barangayId =
                preferences.getString(
                        "barangay_id",
                        ""
                );

        if (accessToken == null
                || accessToken.trim().isEmpty()) {

            Toast.makeText(
                    AddReportActivity.this,
                    "Your login session is missing. Please log in again.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (userId == null
                || userId.trim().isEmpty()) {

            Toast.makeText(
                    AddReportActivity.this,
                    "Unable to identify your account.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (barangayId == null
                || barangayId.trim().isEmpty()) {

            Toast.makeText(
                    AddReportActivity.this,
                    "Unable to identify your assigned barangay.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (editingDraftId != null
                && !editingDraftId.trim().isEmpty()) {

            Map<String, Object> draftUpdate =
                    buildDraftIncidentReportPayload();

            updateExistingDraftReport(
                    accessToken,
                    userId,
                    draftUpdate,
                    true
            );

            return;
        }

        savingAsDraft = true;

        /*
         * Retry the child-data pipeline if the draft row was already
         * created but a perpetrator or attachment failed to save.
         */
        if (createdReportId != null
                && !createdReportId.trim().isEmpty()) {

            setReportActionButtonsEnabled(
                    false
            );

            if (pendingPerpetratorPayloads == null
                    || pendingPerpetratorPayloads.isEmpty()) {

                pendingPerpetratorPayloads =
                        buildPerpetratorPayloads(
                                createdReportId
                        );
            }

            if (!pendingPerpetratorPayloads.isEmpty()) {

                submitPerpetrators(
                        accessToken
                );

            } else {

                submitAttachments(
                        accessToken
                );
            }

            return;
        }

        Map<String, Object> draft =
                buildDraftIncidentReportPayload();

        setReportActionButtonsEnabled(
                false
        );

        incidentReportAPI
                .createIncidentReport(
                        "Bearer " + accessToken,
                        draft
                )
                .enqueue(
                        new Callback<List<Map<String, Object>>>() {

                            @Override
                            public void onResponse(
                                    Call<List<Map<String, Object>>> call,
                                    Response<List<Map<String, Object>>> response
                            ) {

                                if (response.isSuccessful()
                                        && response.body() != null
                                        && !response.body().isEmpty()) {

                                    Map<String, Object> createdDraft =
                                            response.body().get(0);

                                    Object reportIdObject =
                                            createdDraft.get(
                                                    "id"
                                            );

                                    if (reportIdObject == null
                                            || reportIdObject
                                            .toString()
                                            .trim()
                                            .isEmpty()) {

                                        savingAsDraft = false;

                                        setReportActionButtonsEnabled(
                                                true
                                        );

                                        Toast.makeText(
                                                AddReportActivity.this,
                                                "Draft was saved, but its ID was not returned.",
                                                Toast.LENGTH_LONG
                                        ).show();

                                        return;
                                    }

                                    createdReportId =
                                            reportIdObject
                                                    .toString()
                                                    .trim();

                                    pendingPerpetratorPayloads =
                                            buildPerpetratorPayloads(
                                                    createdReportId
                                            );

                                    nextPerpetratorIndexToSubmit = 0;
                                    nextAttachmentIndexToSubmit = 0;

                                    if (!pendingPerpetratorPayloads.isEmpty()) {

                                        submitPerpetrators(
                                                accessToken
                                        );

                                    } else {

                                        submitAttachments(
                                                accessToken
                                        );
                                    }

                                    return;
                                }

                                savingAsDraft = false;

                                setReportActionButtonsEnabled(
                                        true
                                );

                                String errorMessage =
                                        "Unable to save draft.";

                                try {

                                    if (response.errorBody() != null) {

                                        String errorBody =
                                                response.errorBody().string();

                                        if (errorBody != null
                                                && !errorBody.trim().isEmpty()) {

                                            errorMessage =
                                                    "Unable to save draft. "
                                                            + errorBody;
                                        }
                                    }

                                } catch (Exception ignored) {
                                }

                                Toast.makeText(
                                        AddReportActivity.this,
                                        errorMessage,
                                        Toast.LENGTH_LONG
                                ).show();
                            }


                            @Override
                            public void onFailure(
                                    Call<List<Map<String, Object>>> call,
                                    Throwable t
                            ) {

                                savingAsDraft = false;

                                setReportActionButtonsEnabled(
                                        true
                                );

                                String message =
                                        "Unable to save draft. Check your connection.";

                                if (t != null
                                        && t.getMessage() != null
                                        && !t.getMessage().trim().isEmpty()) {

                                    message =
                                            "Unable to save draft. "
                                                    + t.getMessage();
                                }

                                Toast.makeText(
                                        AddReportActivity.this,
                                        message,
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }


    // =====================================================
    // UPDATE EXISTING DRAFT
    //
    // Reopened drafts must PATCH the same incident_reports row.
    // This prevents duplicate reports.
    //
    // Existing perpetrator rows are preserved in this step instead
    // of being inserted again. Newly selected attachments can still
    // be added after the main draft row updates successfully.
    // =====================================================

    private void updateExistingDraftReport(
            String accessToken,
            String userId,
            Map<String, Object> payload,
            boolean keepAsDraft
    ) {

        if (editingDraftId == null
                || editingDraftId.trim().isEmpty()) {

            Toast.makeText(
                    AddReportActivity.this,
                    "Unable to identify the draft to update.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        savingAsDraft =
                keepAsDraft;

        setReportActionButtonsEnabled(
                false
        );

        incidentReportAPI
                .updateIncidentReport(
                        "Bearer " + accessToken,
                        "eq." + editingDraftId,
                        "eq." + userId,
                        payload
                )
                .enqueue(
                        new Callback<List<Map<String, Object>>>() {

                            @Override
                            public void onResponse(
                                    Call<List<Map<String, Object>>> call,
                                    Response<List<Map<String, Object>>> response
                            ) {

                                if (response.isSuccessful()
                                        && response.body() != null
                                        && !response.body().isEmpty()) {

                                    createdReportId =
                                            editingDraftId;

                                    pendingPerpetratorPayloads.clear();
                                    nextPerpetratorIndexToSubmit = 0;
                                    nextAttachmentIndexToSubmit = 0;

                                    /*
                                     * IMPORTANT:
                                     * Do not call submitPerpetrators() here.
                                     * Those rows already belong to this draft and
                                     * POSTing them again would create duplicates.
                                     *
                                     * Newly picked attachments are safe to continue
                                     * through the existing attachment pipeline.
                                     */
                                    if (!selectedAttachments.isEmpty()) {

                                        submitAttachments(
                                                accessToken
                                        );

                                    } else {

                                        finishSuccessfulSubmission();
                                    }

                                    return;
                                }

                                savingAsDraft = false;

                                setReportActionButtonsEnabled(
                                        true
                                );

                                String errorMessage =
                                        keepAsDraft
                                                ? "Unable to update draft."
                                                : "Unable to submit draft.";

                                try {

                                    if (response.errorBody() != null) {

                                        String errorBody =
                                                response.errorBody().string();

                                        if (errorBody != null
                                                && !errorBody.trim().isEmpty()) {

                                            errorMessage =
                                                    errorMessage
                                                            + " "
                                                            + errorBody;
                                        }
                                    }

                                } catch (Exception ignored) {
                                }

                                Toast.makeText(
                                        AddReportActivity.this,
                                        errorMessage,
                                        Toast.LENGTH_LONG
                                ).show();
                            }


                            @Override
                            public void onFailure(
                                    Call<List<Map<String, Object>>> call,
                                    Throwable t
                            ) {

                                savingAsDraft = false;

                                setReportActionButtonsEnabled(
                                        true
                                );

                                String message =
                                        keepAsDraft
                                                ? "Unable to update draft. Check your connection."
                                                : "Unable to submit draft. Check your connection.";

                                if (t != null
                                        && t.getMessage() != null
                                        && !t.getMessage().trim().isEmpty()) {

                                    message =
                                            message
                                                    + " "
                                                    + t.getMessage();
                                }

                                Toast.makeText(
                                        AddReportActivity.this,
                                        message,
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }


    // =====================================================
    // SUBMIT INCIDENT REPORT TO SUPABASE
    // =====================================================

    private void submitIncidentReport() {

        savingAsDraft = false;

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

        String barangayId =
                preferences.getString(
                        "barangay_id",
                        ""
                );

        if (accessToken == null
                || accessToken.trim().isEmpty()) {

            Toast.makeText(
                    AddReportActivity.this,
                    "Your login session is missing. Please log in again.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (userId == null
                || userId.trim().isEmpty()) {

            Toast.makeText(
                    AddReportActivity.this,
                    "Unable to identify your account.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (barangayId == null
                || barangayId.trim().isEmpty()) {

            Toast.makeText(
                    AddReportActivity.this,
                    "Unable to identify your assigned barangay.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        /*
         * If the incident report was already created during a previous
         * attempt but saving the perpetrator failed, retry only the
         * perpetrator insert. This avoids duplicate incident reports.
         */
        if (createdReportId != null
                && !createdReportId.trim().isEmpty()) {

            setReportActionButtonsEnabled(false);

            if (pendingPerpetratorPayloads == null
                    || pendingPerpetratorPayloads.isEmpty()) {

                pendingPerpetratorPayloads =
                        buildPerpetratorPayloads(
                                createdReportId
                        );
            }

            if (!pendingPerpetratorPayloads.isEmpty()) {

                submitPerpetrators(
                        accessToken
                );

            } else {

                submitAttachments(accessToken);
            }

            return;
        }

        String databaseDate =
                formatIncidentDateForDatabase(
                        incidentDateInput
                                .getText()
                                .toString()
                                .trim()
                );

        if (databaseDate.isEmpty()) {

            Toast.makeText(
                    AddReportActivity.this,
                    "Unable to process the incident date.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String databaseTime =
                formatIncidentTimeForDatabase(
                        incidentTimeInput
                                .getText()
                                .toString()
                                .trim()
                );

        if (databaseTime.isEmpty()) {

            Toast.makeText(
                    AddReportActivity.this,
                    "Unable to process the incident time.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String reporterType =
                mapReporterTypeForDatabase(
                        reporterTypeDropdown
                                .getText()
                                .toString()
                                .trim()
                );

        if (reporterType.isEmpty()) {

            Toast.makeText(
                    AddReportActivity.this,
                    "Invalid reporter type.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String placeType =
                mapPlaceTypeForDatabase(
                        placeTypeDropdown
                                .getText()
                                .toString()
                                .trim()
                );

        if (placeType.isEmpty()) {

            Toast.makeText(
                    AddReportActivity.this,
                    "Please select the incident place type.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Map<String, Object> report =
                buildIncidentReportPayload();

        if (editingDraftId != null
                && !editingDraftId.trim().isEmpty()) {

            report.put(
                    "report_status",
                    "submitted"
            );

            updateExistingDraftReport(
                    accessToken,
                    userId,
                    report,
                    false
            );

            return;
        }

        setReportActionButtonsEnabled(false);

        incidentReportAPI
                .createIncidentReport(
                        "Bearer " + accessToken,
                        report
                )
                .enqueue(
                        new Callback<List<Map<String, Object>>>() {

                            @Override
                            public void onResponse(
                                    Call<List<Map<String, Object>>> call,
                                    Response<List<Map<String, Object>>> response
                            ) {

                                if (response.isSuccessful()
                                        && response.body() != null
                                        && !response.body().isEmpty()) {

                                    Map<String, Object> createdReport =
                                            response.body().get(0);

                                    Object reportIdObject =
                                            createdReport.get("id");

                                    if (reportIdObject == null
                                            || reportIdObject
                                            .toString()
                                            .trim()
                                            .isEmpty()) {

                                        setReportActionButtonsEnabled(true);

                                        Toast.makeText(
                                                AddReportActivity.this,
                                                "Report was created, but its ID was not returned.",
                                                Toast.LENGTH_LONG
                                        ).show();

                                        return;
                                    }

                                    createdReportId =
                                            reportIdObject
                                                    .toString()
                                                    .trim();

                                    pendingPerpetratorPayloads =
                                            buildPerpetratorPayloads(
                                                    createdReportId
                                            );

                                    nextPerpetratorIndexToSubmit = 0;
                                    nextAttachmentIndexToSubmit = 0;

                                    if (!pendingPerpetratorPayloads.isEmpty()) {

                                        submitPerpetrators(
                                                accessToken
                                        );

                                    } else {

                                        submitAttachments(accessToken);
                                    }

                                    return;
                                }

                                setReportActionButtonsEnabled(true);

                                String errorMessage =
                                        "Unable to submit report.";

                                try {

                                    if (response.errorBody() != null) {

                                        String errorBody =
                                                response.errorBody().string();

                                        if (errorBody != null
                                                && !errorBody.trim().isEmpty()) {

                                            errorMessage =
                                                    "Unable to submit report. "
                                                            + errorBody;
                                        }
                                    }

                                } catch (Exception ignored) {
                                }

                                Toast.makeText(
                                        AddReportActivity.this,
                                        errorMessage,
                                        Toast.LENGTH_LONG
                                ).show();
                            }


                            @Override
                            public void onFailure(
                                    Call<List<Map<String, Object>>> call,
                                    Throwable t
                            ) {

                                setReportActionButtonsEnabled(true);

                                String message =
                                        "Unable to submit report. Check your connection.";

                                if (t != null
                                        && t.getMessage() != null
                                        && !t.getMessage().trim().isEmpty()) {

                                    message =
                                            "Unable to submit report. "
                                                    + t.getMessage();
                                }

                                Toast.makeText(
                                        AddReportActivity.this,
                                        message,
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }


    // =====================================================
    // SUBMIT PERPETRATOR TO SUPABASE
    // =====================================================

    private void submitPerpetrators(
            String accessToken
    ) {

        if (perpetratorAPI == null) {

            setReportActionButtonsEnabled(true);

            showRedToast(
                    "Report was created, but the perpetrator service is unavailable."
            );

            return;
        }

        if (pendingPerpetratorPayloads == null
                || pendingPerpetratorPayloads.isEmpty()) {

            submitAttachments(accessToken);
            return;
        }

        if (nextPerpetratorIndexToSubmit
                >= pendingPerpetratorPayloads.size()) {

            submitAttachments(accessToken);
            return;
        }

        Map<String, Object> perpetrator =
                pendingPerpetratorPayloads.get(
                        nextPerpetratorIndexToSubmit
                );

        perpetratorAPI
                .createPerpetrator(
                        "Bearer " + accessToken,
                        perpetrator
                )
                .enqueue(
                        new Callback<List<Map<String, Object>>>() {

                            @Override
                            public void onResponse(
                                    Call<List<Map<String, Object>>> call,
                                    Response<List<Map<String, Object>>> response
                            ) {

                                if (response.isSuccessful()
                                        && response.body() != null
                                        && !response.body().isEmpty()) {

                                    nextPerpetratorIndexToSubmit++;

                                    submitPerpetrators(
                                            accessToken
                                    );

                                    return;
                                }

                                setReportActionButtonsEnabled(true);

                                String errorMessage =
                                        "Report was created, but perpetrator "
                                                + (nextPerpetratorIndexToSubmit + 1)
                                                + " could not be saved.";

                                try {

                                    if (response.errorBody() != null) {

                                        String errorBody =
                                                response.errorBody().string();

                                        if (errorBody != null
                                                && !errorBody.trim().isEmpty()) {

                                            errorMessage =
                                                    errorMessage
                                                            + " "
                                                            + errorBody;
                                        }
                                    }

                                } catch (Exception ignored) {
                                }

                                showRedToast(
                                        errorMessage
                                );
                            }

                            @Override
                            public void onFailure(
                                    Call<List<Map<String, Object>>> call,
                                    Throwable t
                            ) {

                                setReportActionButtonsEnabled(true);

                                String message =
                                        "Report was created, but perpetrator "
                                                + (nextPerpetratorIndexToSubmit + 1)
                                                + " could not be saved. Check your connection.";

                                if (t != null
                                        && t.getMessage() != null
                                        && !t.getMessage().trim().isEmpty()) {

                                    message =
                                            message
                                                    + " "
                                                    + t.getMessage();
                                }

                                showRedToast(
                                        message
                                );
                            }
                        }
                );
    }


    // =====================================================
    // ATTACHMENT PICKER
    // =====================================================

    private final ActivityResultLauncher<Intent> attachmentPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {

                        if (result.getResultCode() != RESULT_OK
                                || result.getData() == null) {
                            return;
                        }

                        Intent data = result.getData();

                        if (data.getClipData() != null) {

                            ClipData clipData = data.getClipData();

                            for (int i = 0;
                                 i < clipData.getItemCount();
                                 i++) {

                                if (selectedAttachments.size()
                                        >= MAX_ATTACHMENTS) {

                                    Toast.makeText(
                                            AddReportActivity.this,
                                            "Maximum of "
                                                    + MAX_ATTACHMENTS
                                                    + " attachments allowed.",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    break;
                                }

                                addSelectedAttachment(
                                        clipData
                                                .getItemAt(i)
                                                .getUri()
                                );
                            }

                        } else if (data.getData() != null) {

                            if (selectedAttachments.size()
                                    >= MAX_ATTACHMENTS) {

                                Toast.makeText(
                                        AddReportActivity.this,
                                        "Maximum of "
                                                + MAX_ATTACHMENTS
                                                + " attachments allowed.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            addSelectedAttachment(
                                    data.getData()
                            );
                        }
                    }
            );


    private void openAttachmentPicker() {

        Intent intent =
                new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.setType("*/*");

        intent.putExtra(
                Intent.EXTRA_ALLOW_MULTIPLE,
                true
        );

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        String[] mimeTypes = {
                "image/*",
                "video/*",
                "audio/*",
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "text/plain"
        };

        intent.putExtra(
                Intent.EXTRA_MIME_TYPES,
                mimeTypes
        );

        attachmentPickerLauncher.launch(
                intent
        );
    }


    private void addSelectedAttachment(
            Uri uri
    ) {

        if (uri == null) {
            return;
        }

        for (AttachmentItem item : selectedAttachments) {

            /*
             * Saved attachments loaded from Supabase do not have a local Uri.
             * Only newly selected local attachments have item.uri.
             *
             * Without this null check, reopening a draft that already has a
             * saved attachment and then selecting another file causes:
             * NullPointerException: item.uri.equals(uri)
             */
            if (item.uri != null
                    && item.uri.equals(uri)) {

                Toast.makeText(
                        AddReportActivity.this,
                        "This file is already selected.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }
        }

        String mimeType =
                getContentResolver().getType(uri);

        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        String fileType =
                getAttachmentType(
                        mimeType
                );

        if (fileType.isEmpty()) {

            Toast.makeText(
                    AddReportActivity.this,
                    "Unsupported file type.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String fileName =
                getAttachmentFileName(uri);

        long fileSize =
                getAttachmentFileSize(uri);

        try {

            getContentResolver()
                    .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );

        } catch (Exception ignored) {
        }

        selectedAttachments.add(
                new AttachmentItem(
                        uri,
                        fileName,
                        mimeType,
                        fileType,
                        fileSize
                )
        );

        renderSelectedAttachments();
    }


    private String getAttachmentType(
            String mimeType
    ) {

        if (mimeType == null) {
            return "";
        }

        String lower =
                mimeType.toLowerCase(Locale.US);

        if (lower.startsWith("image/")) {
            return "image";
        }

        if (lower.startsWith("video/")) {
            return "video";
        }

        if (lower.startsWith("audio/")) {
            return "audio";
        }

        if (lower.equals("application/pdf")
                || lower.equals("application/msword")
                || lower.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                || lower.equals("application/vnd.ms-excel")
                || lower.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                || lower.equals("application/vnd.ms-powerpoint")
                || lower.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation")
                || lower.startsWith("text/")) {

            return "document";
        }

        return "";
    }


    private String getAttachmentFileName(
            Uri uri
    ) {

        String fileName = "attachment";

        Cursor cursor = null;

        try {

            cursor =
                    getContentResolver().query(
                            uri,
                            new String[]{OpenableColumns.DISPLAY_NAME},
                            null,
                            null,
                            null
                    );

            if (cursor != null
                    && cursor.moveToFirst()) {

                int columnIndex =
                        cursor.getColumnIndex(
                                OpenableColumns.DISPLAY_NAME
                        );

                if (columnIndex >= 0) {

                    String value =
                            cursor.getString(
                                    columnIndex
                            );

                    if (value != null
                            && !value.trim().isEmpty()) {

                        fileName = value.trim();
                    }
                }
            }

        } catch (Exception ignored) {

        } finally {

            if (cursor != null) {
                cursor.close();
            }
        }

        return fileName;
    }


    private long getAttachmentFileSize(
            Uri uri
    ) {

        long fileSize = -1L;

        Cursor cursor = null;

        try {

            cursor =
                    getContentResolver().query(
                            uri,
                            new String[]{OpenableColumns.SIZE},
                            null,
                            null,
                            null
                    );

            if (cursor != null
                    && cursor.moveToFirst()) {

                int columnIndex =
                        cursor.getColumnIndex(
                                OpenableColumns.SIZE
                        );

                if (columnIndex >= 0
                        && !cursor.isNull(columnIndex)) {

                    fileSize =
                            cursor.getLong(
                                    columnIndex
                            );
                }
            }

        } catch (Exception ignored) {

        } finally {

            if (cursor != null) {
                cursor.close();
            }
        }

        return fileSize;
    }


    private String formatAttachmentFileSize(
            long bytes
    ) {

        if (bytes < 0) {
            return "Size unavailable";
        }

        if (bytes < 1024) {
            return bytes + " B";
        }

        double kb =
                bytes / 1024.0;

        if (kb < 1024) {
            return String.format(
                    Locale.US,
                    "%.1f KB",
                    kb
            );
        }

        double mb =
                kb / 1024.0;

        return String.format(
                Locale.US,
                "%.1f MB",
                mb
        );
    }


    private int dpToPx(
            int dp
    ) {

        return Math.round(
                dp * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }


    private void renderSelectedAttachments() {

        attachmentsListContainer.removeAllViews();

        for (int i = 0;
             i < selectedAttachments.size();
             i++) {

            AttachmentItem item =
                    selectedAttachments.get(i);

            LinearLayout row =
                    new LinearLayout(this);

            row.setOrientation(
                    LinearLayout.HORIZONTAL
            );

            row.setGravity(
                    android.view.Gravity.CENTER_VERTICAL
            );

            row.setPadding(
                    dpToPx(10),
                    dpToPx(9),
                    dpToPx(8),
                    dpToPx(9)
            );

            LinearLayout.LayoutParams rowParams =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );

            rowParams.topMargin =
                    dpToPx(7);

            row.setLayoutParams(
                    rowParams
            );

            row.setBackgroundColor(
                    Color.parseColor("#F9F6FF")
            );

            TextView details =
                    new TextView(this);

            String attachmentSourceLabel =
                    item.persistedFromDraft
                            ? "Saved attachment"
                            : "New attachment";

            details.setText(
                    item.fileName
                            + "\n"
                            + capitalizeAttachmentType(
                            item.fileType
                    )
                            + " • "
                            + formatAttachmentFileSize(
                            item.fileSize
                    )
                            + " • "
                            + attachmentSourceLabel
            );

            details.setTextColor(
                    Color.parseColor("#21115D")
            );

            details.setTextSize(10);

            details.setTypeface(
                    androidx.core.content.res.ResourcesCompat.getFont(
                            this,
                            R.font.poppins_regular
                    )
            );

            LinearLayout.LayoutParams detailsParams =
                    new LinearLayout.LayoutParams(
                            0,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            1f
                    );

            details.setLayoutParams(
                    detailsParams
            );

            TextView remove =
                    new TextView(this);

            remove.setText("Remove");
            remove.setTextColor(
                    Color.parseColor("#B3261E")
            );
            remove.setTextSize(10);
            remove.setPadding(
                    dpToPx(10),
                    dpToPx(7),
                    dpToPx(4),
                    dpToPx(7)
            );

            remove.setOnClickListener(v -> {

                if (item.persistedFromDraft) {

                    removePersistedDraftAttachment(
                            item
                    );

                    return;
                }

                selectedAttachments.remove(
                        item
                );

                renderSelectedAttachments();
            });

            row.addView(details);
            row.addView(remove);

            attachmentsListContainer.addView(
                    row
            );
        }
    }


    private String capitalizeAttachmentType(
            String value
    ) {

        if (value == null
                || value.trim().isEmpty()) {
            return "File";
        }

        String clean = value.trim();

        return clean.substring(0, 1)
                .toUpperCase(Locale.US)
                + clean.substring(1);
    }


    // =====================================================
    // ATTACHMENT SUBMISSION
    // =====================================================

    private void submitAttachments(
            String accessToken
    ) {

        if (selectedAttachments.isEmpty()) {
            finishSuccessfulSubmission();
            return;
        }

        if (nextAttachmentIndexToSubmit
                >= selectedAttachments.size()) {

            finishSuccessfulSubmission();
            return;
        }

        if (createdReportId == null
                || createdReportId.trim().isEmpty()) {

            setReportActionButtonsEnabled(true);

            showRedToast(
                    "Unable to upload attachments because the report ID is missing."
            );

            return;
        }

        SharedPreferences preferences =
                getSharedPreferences(
                        "HavenUser",
                        MODE_PRIVATE
                );

        String authId =
                preferences.getString(
                        "auth_id",
                        ""
                );

        if (authId == null
                || authId.trim().isEmpty()) {

            setReportActionButtonsEnabled(true);

            showRedToast(
                    "Report was created, but the authenticated user ID is missing for attachment upload."
            );

            return;
        }

        AttachmentItem item =
                selectedAttachments.get(
                        nextAttachmentIndexToSubmit
                );

        if (item.metadataSaved) {

            nextAttachmentIndexToSubmit++;

            submitAttachments(
                    accessToken
            );

            return;
        }

        if (item.storageUploaded) {

            saveAttachmentMetadata(
                    accessToken,
                    item
            );

            return;
        }

        if (item.storagePath == null
                || item.storagePath.trim().isEmpty()) {

            item.storagePath =
                    authId.trim()
                            + "/"
                            + createdReportId.trim()
                            + "/"
                            + UUID.randomUUID()
                            + "_"
                            + sanitizeStorageFileName(
                            item.fileName
                    );
        }

        uploadAttachmentToStorage(
                accessToken,
                item
        );
    }


    private void uploadAttachmentToStorage(
            String accessToken,
            AttachmentItem item
    ) {

        File temporaryFile = null;

        try {

            InputStream inputStream =
                    getContentResolver()
                            .openInputStream(
                                    item.uri
                            );

            if (inputStream == null) {
                throw new IllegalStateException(
                        "Unable to open selected attachment."
                );
            }

            temporaryFile =
                    File.createTempFile(
                            "haven_attachment_",
                            ".tmp",
                            getCacheDir()
                    );

            FileOutputStream outputStream =
                    new FileOutputStream(
                            temporaryFile
                    );

            byte[] buffer =
                    new byte[8192];

            int length;

            while ((length = inputStream.read(buffer)) != -1) {

                outputStream.write(
                        buffer,
                        0,
                        length
                );
            }

            outputStream.close();
            inputStream.close();

            MediaType mediaType =
                    MediaType.parse(
                            item.mimeType
                    );

            if (mediaType == null) {
                mediaType =
                        MediaType.parse(
                                "application/octet-stream"
                        );
            }

            RequestBody requestBody =
                    RequestBody.create(
                            mediaType,
                            temporaryFile
                    );

            File fileToDelete =
                    temporaryFile;

            incidentAttachmentStorageAPI
                    .uploadIncidentAttachment(
                            "Bearer " + accessToken,
                            item.storagePath,
                            requestBody
                    )
                    .enqueue(
                            new Callback<Void>() {

                                @Override
                                public void onResponse(
                                        Call<Void> call,
                                        Response<Void> response
                                ) {

                                    if (fileToDelete.exists()) {
                                        fileToDelete.delete();
                                    }

                                    if (response.isSuccessful()) {

                                        item.storageUploaded = true;

                                        saveAttachmentMetadata(
                                                accessToken,
                                                item
                                        );

                                        return;
                                    }

                                    setReportActionButtonsEnabled(true);

                                    String errorMessage =
                                            "Report was created, but attachment "
                                                    + (nextAttachmentIndexToSubmit + 1)
                                                    + " could not be uploaded.";

                                    try {

                                        if (response.errorBody() != null) {

                                            String errorBody =
                                                    response.errorBody().string();

                                            if (errorBody != null
                                                    && !errorBody.trim().isEmpty()) {

                                                errorMessage =
                                                        errorMessage
                                                                + " "
                                                                + errorBody;
                                            }
                                        }

                                    } catch (Exception ignored) {
                                    }

                                    showRedToast(
                                            errorMessage
                                    );
                                }

                                @Override
                                public void onFailure(
                                        Call<Void> call,
                                        Throwable t
                                ) {

                                    if (fileToDelete.exists()) {
                                        fileToDelete.delete();
                                    }

                                    setReportActionButtonsEnabled(true);

                                    String message =
                                            "Report was created, but attachment "
                                                    + (nextAttachmentIndexToSubmit + 1)
                                                    + " could not be uploaded. Check your connection.";

                                    if (t != null
                                            && t.getMessage() != null
                                            && !t.getMessage().trim().isEmpty()) {

                                        message =
                                                message
                                                        + " "
                                                        + t.getMessage();
                                    }

                                    showRedToast(
                                            message
                                    );
                                }
                            }
                    );

        } catch (Exception e) {

            if (temporaryFile != null
                    && temporaryFile.exists()) {
                temporaryFile.delete();
            }

            setReportActionButtonsEnabled(true);

            showRedToast(
                    "Unable to prepare attachment "
                            + (nextAttachmentIndexToSubmit + 1)
                            + ". "
                            + (e.getMessage() == null
                            ? ""
                            : e.getMessage())
            );
        }
    }


    private void saveAttachmentMetadata(
            String accessToken,
            AttachmentItem item
    ) {

        Map<String, Object> attachment =
                new LinkedHashMap<>();

        attachment.put(
                "report_id",
                createdReportId
        );

        attachment.put(
                "file_url",
                item.storagePath
        );

        attachment.put(
                "file_type",
                item.fileType
        );

        attachment.put(
                "file_name",
                item.fileName
        );

        attachment.put(
                "mime_type",
                item.mimeType
        );

        if (item.fileSize >= 0) {

            attachment.put(
                    "file_size",
                    item.fileSize
            );
        }

        reportAttachmentAPI
                .createReportAttachment(
                        "Bearer " + accessToken,
                        attachment
                )
                .enqueue(
                        new Callback<List<Map<String, Object>>>() {

                            @Override
                            public void onResponse(
                                    Call<List<Map<String, Object>>> call,
                                    Response<List<Map<String, Object>>> response
                            ) {

                                if (response.isSuccessful()
                                        && response.body() != null
                                        && !response.body().isEmpty()) {

                                    item.metadataSaved = true;

                                    nextAttachmentIndexToSubmit++;

                                    submitAttachments(
                                            accessToken
                                    );

                                    return;
                                }

                                setReportActionButtonsEnabled(true);

                                String errorMessage =
                                        "Attachment "
                                                + (nextAttachmentIndexToSubmit + 1)
                                                + " uploaded, but its report record could not be saved.";

                                try {

                                    if (response.errorBody() != null) {

                                        String errorBody =
                                                response.errorBody().string();

                                        if (errorBody != null
                                                && !errorBody.trim().isEmpty()) {

                                            errorMessage =
                                                    errorMessage
                                                            + " "
                                                            + errorBody;
                                        }
                                    }

                                } catch (Exception ignored) {
                                }

                                showRedToast(
                                        errorMessage
                                );
                            }

                            @Override
                            public void onFailure(
                                    Call<List<Map<String, Object>>> call,
                                    Throwable t
                            ) {

                                setReportActionButtonsEnabled(true);

                                String message =
                                        "Attachment "
                                                + (nextAttachmentIndexToSubmit + 1)
                                                + " uploaded, but its report record could not be saved. Check your connection.";

                                if (t != null
                                        && t.getMessage() != null
                                        && !t.getMessage().trim().isEmpty()) {

                                    message =
                                            message
                                                    + " "
                                                    + t.getMessage();
                                }

                                showRedToast(
                                        message
                                );
                            }
                        }
                );
    }


    private String sanitizeStorageFileName(
            String fileName
    ) {

        if (fileName == null
                || fileName.trim().isEmpty()) {

            return "attachment";
        }

        String clean =
                fileName.trim()
                        .replaceAll(
                                "[^A-Za-z0-9._-]",
                                "_"
                        );

        if (clean.isEmpty()) {
            return "attachment";
        }

        return clean;
    }


    private void finishSuccessfulSubmission() {

        boolean wasDraftSave =
                savingAsDraft;

        boolean wasExistingDraftUpdate =
                wasDraftSave
                        && editingDraftId != null
                        && !editingDraftId.trim().isEmpty();

        pendingPerpetratorPayloads.clear();
        nextPerpetratorIndexToSubmit = 0;
        nextAttachmentIndexToSubmit = 0;

        /*
         * IMPORTANT FOR REOPENED DRAFTS:
         *
         * When the user opens an existing draft, adds another attachment,
         * then taps Save as Draft, DO NOT close AddReportActivity.
         *
         * Keep the whole form on screen and convert successfully saved
         * attachment items into persisted draft attachments so they remain
         * visible immediately.
         */
        if (wasExistingDraftUpdate) {

            createdReportId =
                    editingDraftId;

            for (AttachmentItem item
                    : selectedAttachments) {

                if (item.metadataSaved
                        && item.storageUploaded) {

                    item.persistedFromDraft = true;
                }
            }

            savingAsDraft = false;

            setReportActionButtonsEnabled(
                    true
            );

            Toast.makeText(
                    AddReportActivity.this,
                    "Draft updated successfully.",
                    Toast.LENGTH_LONG
            ).show();

            /*
             * After successfully updating an existing draft,
             * close AddReportActivity and return to Drafts.
             */
            finish();

            return;
        }

        /*
         * Original behavior is preserved for:
         * 1. First-time Save as Draft
         * 2. Final report submission
         */
        createdReportId = "";

        selectedAttachments.clear();
        attachmentsListContainer.removeAllViews();

        savingAsDraft = false;

        setReportActionButtonsEnabled(
                true
        );

        Toast.makeText(
                AddReportActivity.this,
                wasDraftSave
                        ? "Draft saved successfully."
                        : "Report submitted successfully.",
                Toast.LENGTH_LONG
        ).show();

        finish();
    }


    // =====================================================
    // VALIDATION
    // =====================================================

    private boolean validateRequiredFields() {

        clearAllValidationErrors();

        if (incidentTypeDropdown.getText().toString().trim().isEmpty()) {
            return failRequired(
                    incidentTypeDropdown,
                    "Incident Type is required."
            );
        }

        if (incidentSubtypeInput.getText().toString().trim().isEmpty()) {
            return failRequired(
                    incidentSubtypeInput,
                    "Incident Subtype is required."
            );
        }

        if (incidentDateInput.getText().toString().trim().isEmpty()) {
            return failRequired(
                    incidentDateInput,
                    "Incident Date is required."
            );
        }

        if (incidentTimeInput.getText().toString().trim().isEmpty()) {
            return failRequired(
                    incidentTimeInput,
                    "Incident Time is required."
            );
        }

        String reporterType =
                reporterTypeDropdown.getText().toString().trim();

        if (reporterType.isEmpty()) {
            return failRequired(
                    reporterTypeDropdown,
                    "Reporter Type is required."
            );
        }

        if (reporterType.equalsIgnoreCase("Companion")
                || reporterType.equalsIgnoreCase("Informant")) {

            if (reporterNameInput.getText().toString().trim().isEmpty()) {
                return failRequired(
                        reporterNameInput,
                        "Reporter Name is required."
                );
            }

            if (!isValidPersonName(
                    reporterNameInput.getText().toString()
            )) {
                return failRequired(
                        reporterNameInput,
                        "Reporter Name can contain letters, spaces, apostrophes, hyphens, and periods only."
                );
            }

            String reporterRelationship =
                    reporterRelationshipDropdown
                            .getText()
                            .toString()
                            .trim();

            if (reporterRelationship.isEmpty()) {
                return failRequired(
                        reporterRelationshipDropdown,
                        "Reporter Relationship is required."
                );
            }

            if (reporterRelationship.equalsIgnoreCase("Other")
                    && otherReporterRelationshipInput
                    .getText()
                    .toString()
                    .trim()
                    .isEmpty()) {

                return failRequired(
                        otherReporterRelationshipInput,
                        "Please specify the reporter relationship."
                );
            }

            if (reporterContactInput.getText().toString().trim().isEmpty()) {
                return failRequired(
                        reporterContactInput,
                        "Reporter Contact Number is required."
                );
            }

            if (!isValidPhoneNumber(
                    reporterContactInput.getText().toString()
            )) {
                return failRequired(
                        reporterContactInput,
                        "Reporter Contact Number must contain exactly 11 digits."
                );
            }

            if (victimFullNameInput.getText().toString().trim().isEmpty()) {
                return failRequired(
                        victimFullNameInput,
                        "Victim Full Name is required."
                );
            }

            if (!isValidPersonName(
                    victimFullNameInput.getText().toString()
            )) {
                return failRequired(
                        victimFullNameInput,
                        "Victim Full Name can contain letters, spaces, apostrophes, hyphens, and periods only."
                );
            }

            if (victimPhoneInput.getText().toString().trim().isEmpty()) {
                return failRequired(
                        victimPhoneInput,
                        "Victim Phone Number is required."
                );
            }

            if (!isValidPhoneNumber(
                    victimPhoneInput.getText().toString()
            )) {
                return failRequired(
                        victimPhoneInput,
                        "Victim Phone Number must contain exactly 11 digits."
                );
            }

            // Victim Email Address is optional, but if provided it must be valid.
            if (!isValidEmailIfProvided(
                    victimEmailInput.getText().toString()
            )) {
                return failRequired(
                        victimEmailInput,
                        "Enter a valid email address."
                );
            }

            if (victimAddressInput.getText().toString().trim().isEmpty()) {
                return failRequired(
                        victimAddressInput,
                        "Victim Home Address is required."
                );
            }
        }

        String perpetratorCount =
                perpetratorCountInput.getText().toString().trim();

        if (perpetratorCount.isEmpty()) {
            return failRequired(
                    perpetratorCountInput,
                    "Number of Perpetrators is required."
            );
        }

        try {

            int count =
                    Integer.parseInt(perpetratorCount);

            if (count < 0) {
                return failRequired(
                        perpetratorCountInput,
                        "Number of perpetrators cannot be negative."
                );
            }

        } catch (NumberFormatException e) {

            return failRequired(
                    perpetratorCountInput,
                    "Enter a valid number of perpetrators."
            );
        }

        for (PerpetratorEntry entry : perpetratorEntries) {

            String perpetratorName =
                    entry.fullNameInput
                            .getText()
                            .toString()
                            .trim();

            if (!perpetratorName.isEmpty()
                    && !isValidPersonName(perpetratorName)) {
                return failRequired(
                        entry.fullNameInput,
                        "Perpetrator Name can contain letters, spaces, apostrophes, hyphens, and periods only."
                );
            }

            if (entry.relationshipDropdown
                    .getText()
                    .toString()
                    .trim()
                    .equalsIgnoreCase("Other")
                    && entry.otherRelationshipInput
                    .getText()
                    .toString()
                    .trim()
                    .isEmpty()) {

                return failRequired(
                        entry.otherRelationshipInput,
                        "Please specify the perpetrator relationship."
                );
            }

            String age =
                    entry.ageInput.getText().toString().trim();

            if (!age.isEmpty()) {
                try {
                    int ageValue = Integer.parseInt(age);

                    if (ageValue < 0 || ageValue > 120) {
                        return failRequired(
                                entry.ageInput,
                                "Perpetrator age must be between 0 and 120."
                        );
                    }

                } catch (NumberFormatException e) {
                    return failRequired(
                            entry.ageInput,
                            "Perpetrator age must contain numbers only."
                    );
                }
            }
        }

        if (placeTypeDropdown.getText().toString().trim().isEmpty()) {
            return failRequired(
                    placeTypeDropdown,
                    "Incident Place Type is required."
            );
        }

        if (placeTypeDropdown
                .getText()
                .toString()
                .trim()
                .equalsIgnoreCase("Other")
                && otherPlaceInput
                .getText()
                .toString()
                .trim()
                .isEmpty()) {

            return failRequired(
                    otherPlaceInput,
                    "Please specify the incident place."
            );
        }

        if (incidentAddressInput.getText().toString().trim().isEmpty()) {
            return failRequired(
                    incidentAddressInput,
                    "Incident Address is required."
            );
        }

        if (incidentBarangayInput.getText().toString().trim().isEmpty()) {
            return failRequired(
                    incidentBarangayInput,
                    "Incident Barangay is required."
            );
        }

        if (incidentMunicipalityInput.getText().toString().trim().isEmpty()) {
            return failRequired(
                    incidentMunicipalityInput,
                    "Incident Municipality is required."
            );
        }

        int electronicChoice =
                electronicMeansRadioGroup.getCheckedRadioButtonId();

        if (electronicChoice == -1) {
            return failRequired(
                    electronicMeansRadioGroup,
                    "Electronic / Online Involvement is required."
            );
        }

        if (electronicChoice == R.id.radioElectronicYes) {

            String platform =
                    electronicPlatformDropdown
                            .getText()
                            .toString()
                            .trim();

            if (platform.isEmpty()) {
                return failRequired(
                        electronicPlatformDropdown,
                        "Electronic Platform is required."
                );
            }

            if (platform.equalsIgnoreCase("Other")
                    && otherElectronicPlatformInput
                    .getText()
                    .toString()
                    .trim()
                    .isEmpty()) {

                return failRequired(
                        otherElectronicPlatformInput,
                        "Please specify the electronic platform."
                );
            }
        }

        if (descriptionInput.getText().toString().trim().isEmpty()) {
            return failRequired(
                    descriptionInput,
                    "Incident Description is required."
            );
        }

        return true;
    }

    private void showRequiredMessage(
            String message
    ) {

        showRedToast(message);
    }


    private boolean failRequired(
            View target,
            String message
    ) {

        scrollToField(target);

        TextInputLayout textInputLayout =
                findParentTextInputLayout(target);

        if (textInputLayout != null) {

            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(message);

        } else {

            // Radio groups and other non-TextInputLayout controls
            // still use the red message toast.
            showRedToast(message);
        }

        if (target != null) {
            target.requestFocus();
        }

        return false;
    }


    private TextInputLayout findParentTextInputLayout(
            View target
    ) {

        if (target == null) {
            return null;
        }

        View current = target;

        while (current != null) {

            ViewParent parent = current.getParent();

            if (parent instanceof TextInputLayout) {
                return (TextInputLayout) parent;
            }

            current =
                    parent instanceof View
                            ? (View) parent
                            : null;
        }

        return null;
    }


    private void clearAllValidationErrors() {

        if (addReportScrollView == null) {
            return;
        }

        clearTextInputErrorsRecursive(
                addReportScrollView
        );
    }


    private void clearTextInputErrorsRecursive(
            View view
    ) {

        if (view instanceof TextInputLayout) {

            TextInputLayout layout =
                    (TextInputLayout) view;

            layout.setError(null);
            layout.setErrorEnabled(false);
        }

        if (view instanceof ViewGroup) {

            ViewGroup group =
                    (ViewGroup) view;

            for (int index = 0;
                 index < group.getChildCount();
                 index++) {

                clearTextInputErrorsRecursive(
                        group.getChildAt(index)
                );
            }
        }
    }


    private void scrollToField(
            View target
    ) {

        if (target == null || addReportScrollView == null) {
            return;
        }

        target.post(() -> {

            int y = 0;
            View current = target;

            while (current != null
                    && current != addReportScrollView) {

                y += current.getTop();

                ViewParent parent =
                        current.getParent();

                current =
                        parent instanceof View
                                ? (View) parent
                                : null;
            }

            addReportScrollView.smoothScrollTo(
                    0,
                    Math.max(
                            0,
                            y - dpToPx(110)
                    )
            );
        });
    }


    private void showRedToast(
            String message
    ) {

        Toast toast =
                Toast.makeText(
                        AddReportActivity.this,
                        message,
                        Toast.LENGTH_SHORT
                );

        View toastView =
                toast.getView();

        if (toastView != null) {

            toastView.setBackgroundTintList(
                    ColorStateList.valueOf(
                            Color.parseColor("#C62828")
                    )
            );

            TextView messageView =
                    toastView.findViewById(
                            android.R.id.message
                    );

            if (messageView != null) {
                messageView.setTextColor(Color.WHITE);
            }
        }

        toast.show();
    }

}

