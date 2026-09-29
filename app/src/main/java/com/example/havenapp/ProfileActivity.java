package com.example.havenapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.havenapp.api.SupabaseClient;
import com.example.havenapp.api.SupabaseConfig;
import com.example.havenapp.api.SupabaseStorageAPI;
import com.example.havenapp.api.SupabaseUserAPI;
import com.example.havenapp.model.Profile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.RequestBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private ImageView userImage;
    private ImageView editProfile;

    private TextView profileName;
    private TextView profileEmail;

    private LinearLayout privacyLayout;
    private LinearLayout aboutLayout;
    private LinearLayout helpLayout;
    private LinearLayout logoutLayout;

    private SupabaseStorageAPI storageAPI;
    private SupabaseUserAPI userAPI;

    private Uri selectedImageUri;


// ==========================
// IMAGE PICKER
// ==========================

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {

                        if (uri != null) {

                            selectedImageUri = uri;

                            // Show preview immediately
                            Glide.with(ProfileActivity.this)
                                    .load(uri)
                                    .circleCrop()
                                    .into(userImage);

                            // Upload image
                            uploadProfileImage(uri);
                        }
                    }
            );


// ==========================
// ON CREATE
// ==========================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.profile_activity);

        storageAPI = SupabaseClient
                .getClient()
                .create(SupabaseStorageAPI.class);

        userAPI = SupabaseClient
                .getClient()
                .create(SupabaseUserAPI.class);

        initializeViews();

        loadUserData();

        setupButtons();

        setupHopeAssistant();

        setupNavigation();

    }

    // ==========================
// FLOATING / MOVABLE HOPE ASSISTANT
// ==========================

    private void setupHopeAssistant() {

        View floatingChatbot =
                findViewById(
                        R.id.cardHopeAssistant
                );

        if (floatingChatbot == null) {
            return;
        }

        ChatbotFloatingHelper.setup(
                this,
                floatingChatbot
        );
    }


// ==========================
// INITIALIZE VIEWS
// ==========================

    private void initializeViews() {

        userImage =
                findViewById(R.id.userImage);

        editProfile =
                findViewById(R.id.editProfile);

        profileName =
                findViewById(R.id.profileName);

        profileEmail =
                findViewById(R.id.profileEmail);

        privacyLayout =
                findViewById(R.id.privacyLayout);

        aboutLayout =
                findViewById(R.id.aboutLayout);

        helpLayout =
                findViewById(R.id.helpLayout);

        logoutLayout =
                findViewById(R.id.logoutLayout);
    }


// ==========================
// LOAD USER DATA
// ==========================

    private void loadUserData() {

        SharedPreferences preferences =
                getSharedPreferences(
                        "HavenUser",
                        MODE_PRIVATE
                );

        String fullname =
                preferences.getString(
                        "fullname",
                        "User"
                );

        String email =
                preferences.getString(
                        "email",
                        "No Email"
                );

        profileName.setText(fullname);
        profileEmail.setText(email);


        // ==========================
        // LOAD SAVED IMAGE FIRST
        // ==========================

        String savedImageUrl =
                preferences.getString(
                        "profile_image",
                        ""
                );

        if (!savedImageUrl.isEmpty()) {

            Glide.with(this)
                    .load(savedImageUrl)
                    .circleCrop()
                    .into(userImage);

        } else {

            Glide.with(this)
                    .load(R.drawable.dp)
                    .into(userImage);
        }


        // ==========================
        // LOAD FROM SUPABASE
        // ==========================

        loadProfileImage();
    }


// ==========================
// LOAD PROFILE IMAGE
// ==========================

    private void loadProfileImage() {

        SharedPreferences preferences =
                getSharedPreferences(
                        "HavenUser",
                        MODE_PRIVATE
                );

        String token =
                preferences.getString(
                        "access_token",
                        ""
                );

        String email =
                preferences.getString(
                        "email",
                        ""
                );


        if (token.isEmpty() || email.isEmpty()) {

            return;
        }


        userAPI.getProfileByEmail(
                        "Bearer " + token,
                        "eq." + email
                )

                .enqueue(new Callback<List<Profile>>() {

                    @Override
                    public void onResponse(
                            Call<List<Profile>> call,
                            Response<List<Profile>> response
                    ) {

                        if (response.isSuccessful()
                                && response.body() != null
                                && !response.body().isEmpty()) {

                            Profile profile =
                                    response.body().get(0);

                            String imageUrl =
                                    profile.getProfile_image();


                            if (imageUrl != null
                                    && !imageUrl.isEmpty()) {


                                // Save latest URL locally
                                SharedPreferences prefs =
                                        getSharedPreferences(
                                                "HavenUser",
                                                MODE_PRIVATE
                                        );

                                prefs.edit()
                                        .putString(
                                                "profile_image",
                                                imageUrl
                                        )
                                        .apply();


                                // Display image
                                Glide.with(ProfileActivity.this)
                                        .load(imageUrl)
                                        .circleCrop()
                                        .into(userImage);
                            }
                        }
                    }


                    @Override
                    public void onFailure(
                            Call<List<Profile>> call,
                            Throwable t
                    ) {

                        android.util.Log.e(
                                "PROFILE_IMAGE",
                                "Load failed: "
                                        + t.getMessage()
                        );

                        // Keep local image if Supabase
                        // cannot be reached.
                    }
                });
    }


// ==========================
// UPLOAD PROFILE IMAGE
// ==========================

    private void uploadProfileImage(Uri uri) {

        try {

            SharedPreferences preferences =
                    getSharedPreferences(
                            "HavenUser",
                            MODE_PRIVATE
                    );


            String token =
                    preferences.getString(
                            "access_token",
                            ""
                    );


            String authId =
                    preferences.getString(
                            "auth_id",
                            ""
                    );


            if (token.isEmpty() || authId.isEmpty()) {

                Toast.makeText(
                        this,
                        "User session expired",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }


            // ==========================
            // GET MIME TYPE
            // ==========================

            String mimeType =
                    getContentResolver()
                            .getType(uri);


            if (mimeType == null
                    || !mimeType.startsWith("image/")) {

                mimeType = "image/jpeg";
            }


            // ==========================
            // GET FILE EXTENSION
            // ==========================

            String extension =
                    MimeTypeMap.getSingleton()
                            .getExtensionFromMimeType(
                                    mimeType
                            );


            if (extension == null) {

                extension = "jpg";
            }


            // ==========================
            // CREATE UNIQUE FILE NAME
            // ==========================

            String fileName =
                    UUID.randomUUID()
                            .toString()
                            + "."
                            + extension;


            // ==========================
            // OPEN SELECTED IMAGE
            // ==========================

            InputStream inputStream =
                    getContentResolver()
                            .openInputStream(uri);


            if (inputStream == null) {

                Toast.makeText(
                        this,
                        "Unable to open selected image",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }


            // ==========================
            // CREATE TEMPORARY FILE
            // ==========================

            File file =
                    new File(
                            getCacheDir(),
                            fileName
                    );


            FileOutputStream outputStream =
                    new FileOutputStream(file);


            byte[] buffer =
                    new byte[1024];

            int length;


            while (
                    (length =
                            inputStream.read(buffer)) > 0
            ) {

                outputStream.write(
                        buffer,
                        0,
                        length
                );
            }


            outputStream.close();
            inputStream.close();


            // ==========================
            // CREATE REQUEST BODY
            // ==========================

            RequestBody requestBody =
                    RequestBody.create(
                            MediaType.parse(mimeType),
                            file
                    );


            android.util.Log.d(
                    "UPLOAD",
                    "Uploading file: "
                            + fileName
                            + " | MIME: "
                            + mimeType
                            + " | Size: "
                            + file.length()
            );


            // ==========================
            // UPLOAD TO SUPABASE
            // ==========================
            //
            // IMPORTANT:
            // This matches your current
            // SupabaseStorageAPI.java:
            //
            // authorization
            // fileName
            // requestBody
            //
            // ==========================

            storageAPI.uploadProfileImage(
                            "Bearer " + token,
                            fileName,
                            requestBody
                    )

                    .enqueue(new Callback<Void>() {

                        @Override
                        public void onResponse(
                                Call<Void> call,
                                Response<Void> response
                        ) {

                            if (response.isSuccessful()) {

                                android.util.Log.d(
                                        "UPLOAD",
                                        "Upload successful"
                                );


                                // ==========================
                                // CREATE PUBLIC URL
                                // ==========================

                                String imageUrl =
                                        SupabaseConfig.BASE_URL
                                                + "storage/v1/object/public/profile-images/"
                                                + fileName;


                                // ==========================
                                // SAVE URL TO DATABASE
                                // ==========================

                                updateProfileImage(
                                        token,
                                        authId,
                                        imageUrl
                                );


                            } else {

                                try {

                                    String error = "";

                                    if (response.errorBody() != null) {

                                        error =
                                                response.errorBody()
                                                        .string();
                                    }


                                    android.util.Log.e(
                                            "UPLOAD",
                                            "Code = "
                                                    + response.code()
                                    );


                                    android.util.Log.e(
                                            "UPLOAD",
                                            error
                                    );


                                    Toast.makeText(
                                            ProfileActivity.this,
                                            "Upload Failed ("
                                                    + response.code()
                                                    + ")",
                                            Toast.LENGTH_LONG
                                    ).show();


                                } catch (Exception e) {

                                    android.util.Log.e(
                                            "UPLOAD",
                                            "Error reading response",
                                            e
                                    );
                                }
                            }
                        }


                        @Override
                        public void onFailure(
                                Call<Void> call,
                                Throwable t
                        ) {

                            android.util.Log.e(
                                    "UPLOAD",
                                    "Upload error",
                                    t
                            );


                            Toast.makeText(
                                    ProfileActivity.this,
                                    "Upload error",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    });


        } catch (Exception e) {

            android.util.Log.e(
                    "UPLOAD",
                    "Image processing error",
                    e
            );


            Toast.makeText(
                    this,
                    "Image error: "
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }


// ==========================
// UPDATE DATABASE IMAGE URL
// ==========================

    private void updateProfileImage(
            String token,
            String authId,
            String imageUrl
    ) {

        Map<String, String> body =
                new HashMap<>();


        body.put(
                "profile_image",
                imageUrl
        );


        userAPI.updateProfileImage(
                        "Bearer " + token,
                        "eq." + authId,
                        body
                )

                .enqueue(new Callback<List<Profile>>() {

                    @Override
                    public void onResponse(
                            Call<List<Profile>> call,
                            Response<List<Profile>> response
                    ) {

                        if (response.isSuccessful()) {


                            // ==========================
                            // SAVE IMAGE URL LOCALLY
                            // ==========================

                            SharedPreferences preferences =
                                    getSharedPreferences(
                                            "HavenUser",
                                            MODE_PRIVATE
                                    );


                            preferences.edit()
                                    .putString(
                                            "profile_image",
                                            imageUrl
                                    )
                                    .apply();


                            // ==========================
                            // DISPLAY IMAGE
                            // ==========================

                            Glide.with(ProfileActivity.this)
                                    .load(imageUrl)
                                    .circleCrop()
                                    .into(userImage);


                            Toast.makeText(
                                    ProfileActivity.this,
                                    "Profile updated",
                                    Toast.LENGTH_SHORT
                            ).show();


                        } else {

                            try {

                                String error = "";

                                if (response.errorBody() != null) {

                                    error =
                                            response.errorBody()
                                                    .string();
                                }


                                android.util.Log.e(
                                        "PROFILE_UPDATE",
                                        "Code = "
                                                + response.code()
                                );


                                android.util.Log.e(
                                        "PROFILE_UPDATE",
                                        error
                                );


                            } catch (Exception e) {

                                android.util.Log.e(
                                        "PROFILE_UPDATE",
                                        "Could not read error",
                                        e
                                );
                            }


                            Toast.makeText(
                                    ProfileActivity.this,
                                    "Database update failed",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }


                    @Override
                    public void onFailure(
                            Call<List<Profile>> call,
                            Throwable t
                    ) {

                        android.util.Log.e(
                                "PROFILE_UPDATE",
                                "Update error",
                                t
                        );


                        Toast.makeText(
                                ProfileActivity.this,
                                "Update error",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }


// ==========================
// BUTTON ACTIONS
// ==========================

    private void setupButtons() {

        editProfile.setOnClickListener(v ->
                imagePicker.launch("image/*")
        );


        privacyLayout.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            ProfileActivity.this,
                            PrivacySecurityActivity.class
                    );

            startActivity(intent);
        });


        aboutLayout.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            ProfileActivity.this,
                            AboutAppActivity.class
                    );

            startActivity(intent);
        });


        helpLayout.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            ProfileActivity.this,
                            HelpSupportActivity.class
                    );

            startActivity(intent);
        });

        // ==========================
        // LOGOUT
        // ==========================

        logoutLayout.setOnClickListener(v -> {

            SharedPreferences preferences =
                    getSharedPreferences(
                            "HavenUser",
                            MODE_PRIVATE
                    );


            preferences.edit()
                    .clear()
                    .apply();


            Intent intent =
                    new Intent(
                            ProfileActivity.this,
                            LoginActivity.class
                    );


            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );


            startActivity(intent);

            finish();
        });
    }

    private void setupNavigation() {

        // HOME
        findViewById(R.id.navHome)
                .setOnClickListener(v -> {

                    startActivity(
                            new Intent(
                                    ProfileActivity.this,
                                    HomeActivity.class
                            )
                    );

                    finish();
                });


        // SUPPORT
        findViewById(R.id.navSupport)
                .setOnClickListener(v -> {

                    startActivity(
                            new Intent(
                                    ProfileActivity.this,
                                    SupportActivity.class
                            )
                    );

                    finish();
                });


        // EXPLORE
        findViewById(R.id.navExplore)
                .setOnClickListener(v -> {

                    startActivity(
                            new Intent(
                                    ProfileActivity.this,
                                    ExploreActivity.class
                            )
                    );

                    finish();
                });


        // PROFILE
        findViewById(R.id.navProfile)
                .setOnClickListener(v -> {
                    // Already on Profile
                });
    }
}