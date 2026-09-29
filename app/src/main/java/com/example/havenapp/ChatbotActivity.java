package com.example.havenapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.havenapp.adapter.HopeMessageAdapter;
import com.example.havenapp.api.HopeChatAPI;
import com.example.havenapp.api.RefreshTokenRequest;
import com.example.havenapp.api.SupabaseAuthAPI;
import com.example.havenapp.api.SupabaseClient;
import com.example.havenapp.model.AuthResponse;
import com.example.havenapp.model.HopeArticle;
import com.example.havenapp.model.HopeChatRequest;
import com.example.havenapp.model.HopeChatResponse;
import com.example.havenapp.model.HopeMessage;
import com.example.havenapp.model.HopeResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatbotActivity extends AppCompatActivity {

    // =====================================================
    // UI
    // =====================================================

    private RecyclerView recyclerHopeMessages;

    private EditText edtHopeMessage;

    private ImageButton btnHopeSend;

    private ImageView btnHopeExit;


    // =====================================================
    // CHAT DATA
    // =====================================================

    /*
     * One Hope conversation for the current app session.
     *
     * Closing ChatbotActivity does NOT create a new chat and does NOT
     * clear the conversation. The same HopeMessage objects are reused,
     * so rich actions such as Read More, View Hotlines, and File Report
     * remain intact when Hope is reopened.
     */
    private static final List<HopeMessage> SESSION_MESSAGES =
            new ArrayList<>();

    private final List<HopeMessage> messages =
            SESSION_MESSAGES;

    private HopeMessageAdapter hopeMessageAdapter;


    // =====================================================
    // HOPE / AUTH API
    // =====================================================

    private HopeChatAPI hopeChatAPI;

    private SupabaseAuthAPI authAPI;

    private Call<HopeChatResponse> activeHopeCall;

    private Call<AuthResponse> activeRefreshCall;

    private boolean isHopeRequestRunning = false;


    // =====================================================
    // LIMITS
    // =====================================================

    private static final int MAX_QUESTION_LENGTH = 1000;

    /*
     * Article content can be very long.
     *
     * Hope only shows a short preview inside the chat.
     * The full article remains available through Read More.
     */
    private static final int ARTICLE_PREVIEW_LENGTH = 320;


    // =====================================================
    // ON CREATE
    // =====================================================

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_chatbot
        );


        setupViews();

        setupHopeAPI();

        setupMessages();

        setupSendButton();

        setupExitButton();

        showWelcome();
    }


    // =====================================================
    // SETUP VIEWS
    // =====================================================

    private void setupViews() {

        recyclerHopeMessages =
                findViewById(
                        R.id.recyclerHopeMessages
                );


        edtHopeMessage =
                findViewById(
                        R.id.edtHopeMessage
                );


        btnHopeSend =
                findViewById(
                        R.id.btnHopeSend
                );


        btnHopeExit =
                findViewById(
                        R.id.btnHopeExit
                );
    }


    // =====================================================
    // SETUP HOPE / AUTH API
    // =====================================================

    private void setupHopeAPI() {

        hopeChatAPI =
                SupabaseClient
                        .getClient()
                        .create(
                                HopeChatAPI.class
                        );


        authAPI =
                SupabaseClient
                        .getClient()
                        .create(
                                SupabaseAuthAPI.class
                        );
    }


    // =====================================================
    // SETUP MESSAGE LIST
    // =====================================================

    private void setupMessages() {

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(
                        this
                );


        recyclerHopeMessages.setLayoutManager(
                layoutManager
        );


        hopeMessageAdapter =
                new HopeMessageAdapter(
                        messages,
                        this::handleSuggestedQuestion
                );


        /*
         * Rich Hope response action buttons are handled
         * by ChatbotActivity rather than inside the adapter.
         */
        hopeMessageAdapter
                .setOnHopeActionClickListener(
                        this::handleHopeAction
                );


        recyclerHopeMessages.setAdapter(
                hopeMessageAdapter
        );
    }


    // =====================================================
    // SETUP SEND BUTTON
    // =====================================================

    private void setupSendButton() {

        btnHopeSend.setOnClickListener(
                v -> sendTypedMessage()
        );
    }


    // =====================================================
    // SETUP EXIT BUTTON
    // =====================================================

    private void setupExitButton() {

        btnHopeExit.setOnClickListener(
                v -> finish()
        );
    }


    // =====================================================
    // SEND TYPED MESSAGE
    // =====================================================

    private void sendTypedMessage() {

        if (isHopeRequestRunning) {

            return;
        }


        String question =
                edtHopeMessage
                        .getText()
                        .toString()
                        .trim();


        if (!isValidQuestion(question)) {

            return;
        }


        edtHopeMessage.setText("");


        processQuestion(
                question
        );
    }


    // =====================================================
    // SUGGESTED QUESTION
    // =====================================================

    private void handleSuggestedQuestion(
            String question
    ) {

        if (isHopeRequestRunning) {

            return;
        }


        if (question == null) {

            return;
        }


        String cleanQuestion =
                question.trim();


        if (!isValidQuestion(cleanQuestion)) {

            return;
        }


        processQuestion(
                cleanQuestion
        );
    }


    // =====================================================
    // VALIDATE QUESTION
    // =====================================================

    private boolean isValidQuestion(
            String question
    ) {

        if (question == null
                || question.trim().isEmpty()) {

            return false;
        }


        if (question.length()
                > MAX_QUESTION_LENGTH) {

            Toast.makeText(
                    this,
                    "Please keep your question under 1000 characters.",
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }


        return true;
    }


    // =====================================================
    // PROCESS QUESTION
    // =====================================================

    private void processQuestion(
            String question
    ) {

        removeWelcomeIfPresent();


        addUserMessage(
                question
        );


        /*
         * Keep the UI locked while the session is refreshed
         * and while Hope is answering.
         */
        setHopeLoading(
                true
        );


        refreshSessionAndAskHope(
                question
        );
    }


    // =====================================================
    // SHOW WELCOME
    // =====================================================

    private void showWelcome() {

        if (!messages.isEmpty()) {

            return;
        }


        HopeMessage welcome =
                new HopeMessage(
                        HopeMessage.TYPE_WELCOME,
                        null
                );


        messages.add(
                welcome
        );


        hopeMessageAdapter.notifyItemInserted(
                messages.size() - 1
        );
    }


    // =====================================================
    // REMOVE WELCOME
    // =====================================================

    private void removeWelcomeIfPresent() {

        if (messages.isEmpty()) {

            return;
        }


        HopeMessage firstMessage =
                messages.get(0);


        if (!firstMessage.isWelcome()) {

            return;
        }


        messages.remove(
                0
        );


        hopeMessageAdapter.notifyItemRemoved(
                0
        );
    }


    // =====================================================
    // REFRESH SESSION THEN ASK HOPE
    // =====================================================

    private void refreshSessionAndAskHope(
            String question
    ) {

        if (authAPI == null) {

            setHopeLoading(
                    false
            );


            addHopeMessage(
                    "I'm having trouble verifying your session right now. Please try again."
            );

            return;
        }


        SharedPreferences preferences =
                getSharedPreferences(
                        "HavenUser",
                        MODE_PRIVATE
                );


        String refreshToken =
                preferences.getString(
                        "refresh_token",
                        ""
                );


        /*
         * Hope should not use an expired access token
         * when the refresh token is unavailable.
         */
        if (refreshToken == null
                || refreshToken.trim().isEmpty()) {

            setHopeLoading(
                    false
            );


            addHopeMessage(
                    "Your login session has expired. Please log in again and try asking Hope."
            );

            return;
        }


        RefreshTokenRequest refreshRequest =
                new RefreshTokenRequest(
                        refreshToken.trim()
                );


        activeRefreshCall =
                authAPI.refreshToken(
                        "refresh_token",
                        refreshRequest
                );


        activeRefreshCall.enqueue(
                new Callback<AuthResponse>() {

                    @Override
                    public void onResponse(
                            Call<AuthResponse> call,
                            Response<AuthResponse> response
                    ) {

                        activeRefreshCall = null;


                        if (isFinishing()
                                || isDestroyed()) {

                            return;
                        }


                        // =========================================
                        // REFRESH FAILED
                        // =========================================

                        if (!response.isSuccessful()
                                || response.body() == null) {

                            setHopeLoading(
                                    false
                            );


                            if (response.code() == 400
                                    || response.code() == 401
                                    || response.code() == 403) {

                                addHopeMessage(
                                        "Your login session has expired. Please log in again and try asking Hope."
                                );

                            } else {

                                addHopeMessage(
                                        "I couldn't refresh your session right now. Please try again."
                                );
                            }


                            return;
                        }


                        AuthResponse authResponse =
                                response.body();


                        String newAccessToken =
                                authResponse.getAccess_token();


                        String newRefreshToken =
                                authResponse.getRefresh_token();


                        // =========================================
                        // ACCESS TOKEN MUST EXIST
                        // =========================================

                        if (newAccessToken == null
                                || newAccessToken.trim().isEmpty()) {

                            setHopeLoading(
                                    false
                            );


                            addHopeMessage(
                                    "Your login session could not be refreshed. Please log in again and try asking Hope."
                            );

                            return;
                        }


                        // =========================================
                        // SAVE FRESH SESSION
                        // =========================================

                        SharedPreferences.Editor editor =
                                preferences.edit();


                        editor.putString(
                                "access_token",
                                newAccessToken.trim()
                        );


                        /*
                         * Supabase may rotate the refresh token.
                         * Save it whenever a new one is returned.
                         */
                        if (newRefreshToken != null
                                && !newRefreshToken.trim().isEmpty()) {

                            editor.putString(
                                    "refresh_token",
                                    newRefreshToken.trim()
                            );
                        }


                        editor.apply();


                        // =========================================
                        // NOW ASK HOPE
                        // =========================================

                        sendQuestionToHope(
                                question,
                                newAccessToken.trim()
                        );
                    }


                    @Override
                    public void onFailure(
                            Call<AuthResponse> call,
                            Throwable t
                    ) {

                        activeRefreshCall = null;


                        if (call.isCanceled()) {

                            return;
                        }


                        if (isFinishing()
                                || isDestroyed()) {

                            return;
                        }


                        setHopeLoading(
                                false
                        );


                        /*
                         * Do not log refresh tokens or other
                         * authentication data.
                         */
                        addHopeMessage(
                                "I couldn't verify your session because of a connection problem. Please check your internet and try again."
                        );
                    }
                }
        );
    }


    // =====================================================
    // SEND QUESTION TO HOPE
    // =====================================================

    private void sendQuestionToHope(
            String question,
            String accessToken
    ) {

        if (hopeChatAPI == null) {

            setHopeLoading(
                    false
            );


            addHopeMessage(
                    "I'm having trouble connecting right now. Please try again."
            );

            return;
        }


        if (accessToken == null
                || accessToken.trim().isEmpty()) {

            setHopeLoading(
                    false
            );


            addHopeMessage(
                    "Your session could not be verified. Please log in again and try asking Hope."
            );

            return;
        }


        HopeChatRequest request =
                new HopeChatRequest(
                        question
                );


        activeHopeCall =
                hopeChatAPI.askHope(
                        "Bearer " +
                                accessToken.trim(),
                        request
                );


        activeHopeCall.enqueue(
                new Callback<HopeChatResponse>() {

                    @Override
                    public void onResponse(
                            Call<HopeChatResponse> call,
                            Response<HopeChatResponse> response
                    ) {

                        activeHopeCall = null;


                        setHopeLoading(
                                false
                        );


                        if (isFinishing()
                                || isDestroyed()) {

                            return;
                        }


                        // =========================================
                        // HTTP FAILURE
                        // =========================================

                        if (!response.isSuccessful()) {

                            handleHopeHttpError(
                                    response.code()
                            );

                            return;
                        }


                        HopeChatResponse responseBody =
                                response.body();


                        // =========================================
                        // EMPTY RESPONSE
                        // =========================================

                        if (responseBody == null) {

                            addHopeMessage(
                                    "I couldn't retrieve an answer right now. Please try again."
                            );

                            return;
                        }


                        // =========================================
                        // EDGE FUNCTION REPORTED FAILURE
                        // =========================================

                        if (!responseBody.isSuccess()) {

                            String error =
                                    responseBody.getError();


                            if (error != null
                                    && error
                                    .toLowerCase()
                                    .contains(
                                            "authentication"
                                    )) {

                                addHopeMessage(
                                        "Your session could not be verified. Please log in again and try asking Hope."
                                );

                            } else {

                                addHopeMessage(
                                        "I couldn't retrieve an answer right now. Please try again."
                                );
                            }


                            return;
                        }


                        HopeResponse hopeResponse =
                                responseBody.getResponse();


                        if (hopeResponse == null) {

                            addHopeMessage(
                                    "I couldn't retrieve an answer right now. Please try again."
                            );

                            return;
                        }


                        displayHopeResponse(
                                hopeResponse
                        );
                    }


                    @Override
                    public void onFailure(
                            Call<HopeChatResponse> call,
                            Throwable t
                    ) {

                        activeHopeCall = null;


                        setHopeLoading(
                                false
                        );


                        if (call.isCanceled()) {

                            return;
                        }


                        if (isFinishing()
                                || isDestroyed()) {

                            return;
                        }


                        /*
                         * Do not log the user's question,
                         * token, or Hope's response.
                         */
                        addHopeMessage(
                                "I couldn't connect right now. Please check your internet connection and try again."
                        );
                    }
                }
        );
    }


    // =====================================================
    // DISPLAY HOPE RESPONSE
    // =====================================================

    private void displayHopeResponse(
            HopeResponse response
    ) {

        if (response == null) {

            addHopeMessage(
                    "I couldn't retrieve an answer right now. Please try again."
            );

            return;
        }


        String actionType =
                cleanText(
                        response.getActionType()
                );


        String responseTitle =
                cleanText(
                        response.getTitle()
                );


        String responseMessage =
                cleanText(
                        response.getMessage()
                );


        HopeArticle article =
                response.getArticle();


        // =================================================
        // ARTICLE RESPONSE
        // =================================================

        if ("legal_article".equalsIgnoreCase(
                actionType
        )
                || "educational_article".equalsIgnoreCase(
                actionType
        )) {

            displayArticleResponse(
                    response,
                    article,
                    actionType,
                    responseTitle,
                    responseMessage
            );

            return;
        }


        // =================================================
        // EMERGENCY HOTLINES
        // =================================================

        if ("emergency_hotlines".equalsIgnoreCase(
                actionType
        )) {

            addActionResponse(
                    responseTitle,
                    responseMessage,
                    actionType,
                    "View Hotlines",
                    "You can view HavenApp's current emergency hotline directory for important local contact numbers."
            );

            return;
        }


        // =================================================
        // FILE A NEW REPORT
        // =================================================

        if ("file_report".equalsIgnoreCase(
                actionType
        )) {

            addActionResponse(
                    responseTitle,
                    responseMessage,
                    actionType,
                    "File a New Report",
                    "You can report an incident through HavenApp's reporting feature."
            );

            return;
        }


        // =================================================
        // EMERGENCY HELP
        // =================================================

        if ("emergency_help".equalsIgnoreCase(
                actionType
        )) {

            addActionResponse(
                    responseTitle,
                    responseMessage,
                    actionType,
                    "View Emergency Options",
                    "If you are in immediate danger, prioritize getting to a safer place if you can do so safely and use HavenApp's emergency resources."
            );

            return;
        }


        // =================================================
        // LEGAL RIGHTS PAGE
        // =================================================

        if ("legal_rights".equalsIgnoreCase(
                actionType
        )) {

            addActionResponse(
                    responseTitle,
                    responseMessage,
                    actionType,
                    "Open Legal Rights",
                    "You can find information about your legal rights and protections in HavenApp's Legal Rights section."
            );

            return;
        }


        // =================================================
        // EDUCATIONAL ARTICLES PAGE
        // =================================================

        if ("educational_articles".equalsIgnoreCase(
                actionType
        )) {

            addActionResponse(
                    responseTitle,
                    responseMessage,
                    actionType,
                    "View Articles",
                    "You can find awareness, safety, and educational resources in HavenApp's Educational Articles."
            );

            return;
        }


        // =================================================
        // LATEST ANNOUNCEMENTS
        // =================================================

        if ("latest_announcements".equalsIgnoreCase(
                actionType
        )) {

            addActionResponse(
                    responseTitle,
                    responseMessage,
                    actionType,
                    "View Announcements",
                    "You can view the latest announcements and updates in Explore."
            );

            return;
        }


        // =================================================
        // TINIG NG KABABAIHAN
        // =================================================

        if ("tinig_kababaihan".equalsIgnoreCase(
                actionType
        )) {

            addActionResponse(
                    responseTitle,
                    responseMessage,
                    actionType,
                    "View TNK Episodes",
                    "You can find Tinig ng Kababaihan episodes in Explore."
            );

            return;
        }


        // =================================================
        // EXPLORE
        // =================================================

        if ("explore".equalsIgnoreCase(
                actionType
        )) {

            addActionResponse(
                    responseTitle,
                    responseMessage,
                    actionType,
                    "Open Explore",
                    "You can find HavenApp's information resources in Explore."
            );

            return;
        }


        // =================================================
        // SUPPORT
        // =================================================

        if ("support".equalsIgnoreCase(
                actionType
        )) {

            addActionResponse(
                    responseTitle,
                    responseMessage,
                    actionType,
                    "Open Support",
                    "You can find reporting and support-related features in HavenApp's Support section."
            );

            return;
        }


        // =================================================
        // MY REPORTS
        // =================================================

        if ("my_reports".equalsIgnoreCase(
                actionType
        )) {

            addActionResponse(
                    responseTitle,
                    responseMessage,
                    actionType,
                    "View My Reports",
                    "You can view your available report records in My Reports."
            );

            return;
        }


        // =================================================
        // LOCATION HELP
        // =================================================

        if ("location_help".equalsIgnoreCase(
                actionType
        )) {

            addActionResponse(
                    responseTitle,
                    responseMessage,
                    actionType,
                    "Go to Home",
                    "HavenApp's location-sharing control is available on the Home screen."
            );

            return;
        }


        // =================================================
        // PANIC HELP
        // =================================================

        if ("panic_help".equalsIgnoreCase(
                actionType
        )) {

            addActionResponse(
                    responseTitle,
                    responseMessage,
                    actionType,
                    "Go to Home",
                    "HavenApp's Panic button is available on the Home screen."
            );

            return;
        }


        // =================================================
        // GUIDED HELP
        // =================================================

        if ("guided_help".equalsIgnoreCase(
                actionType
        )) {

            addInformationalResponse(
                    responseTitle,
                    responseMessage,
                    "I can help you figure out where to start. You can ask about safety, abuse, legal rights, reporting, or emergency contacts."
            );

            return;
        }


        // =================================================
        // ABOUT HOPE
        // =================================================

        if ("about_hope".equalsIgnoreCase(
                actionType
        )) {

            addInformationalResponse(
                    responseTitle,
                    responseMessage,
                    "I am Hope, HavenApp's information assistant. I can help you find trusted information, resources, and HavenApp features."
            );

            return;
        }


        // =================================================
        // NORMAL / FALLBACK RESPONSE
        // =================================================

        if (responseMessage.isEmpty()) {

            addHopeMessage(
                    "I couldn't find information for that question. Try asking about your rights, abuse, safety, reporting, or emergency hotlines."
            );

            return;
        }


        HopeMessage hopeMessage =
                new HopeMessage(
                        HopeMessage.TYPE_HOPE,
                        responseMessage
                );


        hopeMessage.setTitle(
                responseTitle
        );


        addHopeMessage(
                hopeMessage
        );
    }


    // =====================================================
    // ADD ACTION RESPONSE
    // =====================================================

    private void addActionResponse(
            String responseTitle,
            String responseMessage,
            String actionType,
            String actionLabel,
            String fallbackMessage
    ) {

        String message =
                cleanText(
                        responseMessage
                );


        if (message.isEmpty()) {

            message =
                    cleanText(
                            fallbackMessage
                    );
        }


        if (message.isEmpty()) {

            message =
                    "I found an option in HavenApp that may help.";
        }


        HopeMessage hopeMessage =
                new HopeMessage(
                        HopeMessage.TYPE_HOPE,
                        message
                );


        hopeMessage.setTitle(
                cleanText(
                        responseTitle
                )
        );


        hopeMessage.setActionType(
                cleanText(
                        actionType
                )
        );


        hopeMessage.setActionLabel(
                cleanText(
                        actionLabel
                )
        );


        addHopeMessage(
                hopeMessage
        );
    }


    // =====================================================
    // ADD INFORMATIONAL RESPONSE
    // =====================================================

    private void addInformationalResponse(
            String responseTitle,
            String responseMessage,
            String fallbackMessage
    ) {

        String message =
                cleanText(
                        responseMessage
                );


        if (message.isEmpty()) {

            message =
                    cleanText(
                            fallbackMessage
                    );
        }


        if (message.isEmpty()) {

            message =
                    "I can help you find information and resources available in HavenApp.";
        }


        HopeMessage hopeMessage =
                new HopeMessage(
                        HopeMessage.TYPE_HOPE,
                        message
                );


        hopeMessage.setTitle(
                cleanText(
                        responseTitle
                )
        );


        addHopeMessage(
                hopeMessage
        );
    }


    // =====================================================
    // DISPLAY ARTICLE RESPONSE
    // =====================================================

    private void displayArticleResponse(
            HopeResponse response,
            HopeArticle article,
            String actionType,
            String responseTitle,
            String responseMessage
    ) {

        // =================================================
        // ARTICLE MISSING
        // =================================================

        if (article == null) {

            if (!responseMessage.isEmpty()) {

                addHopeMessage(
                        responseMessage
                );

            } else {

                addHopeMessage(
                        "I couldn't retrieve the article details right now. Please try again."
                );
            }

            return;
        }


        String articleId =
                cleanText(
                        article.getId()
                );


        String articleTitle =
                cleanText(
                        article.getTitle()
                );


        String articleContent =
                cleanText(
                        article.getContent()
                );


        String sourceName =
                cleanText(
                        article.getSourceName()
                );


        String sourceUrl =
                cleanText(
                        article.getSourceUrl()
                );


        // =================================================
        // TITLE
        // =================================================

        String finalTitle =
                articleTitle;


        if (finalTitle.isEmpty()) {

            finalTitle =
                    responseTitle;
        }


        // =================================================
        // PREVIEW
        // =================================================

        String preview =
                createArticlePreview(
                        articleContent
                );


        /*
         * If content is unexpectedly missing,
         * use the backend's response text instead.
         */
        if (preview.isEmpty()) {

            preview =
                    responseMessage;
        }


        if (preview.isEmpty()) {

            preview =
                    "I found information related to your question.";
        }


        /*
         * Put the title into the visible message because
         * item_hope_message.xml currently has no separate
         * title TextView.
         */
        String visibleMessage;


        if (!finalTitle.isEmpty()) {

            visibleMessage =
                    finalTitle
                            + "\n\n"
                            + preview;

        } else {

            visibleMessage =
                    preview;
        }


        HopeMessage hopeMessage =
                new HopeMessage(
                        HopeMessage.TYPE_HOPE,
                        visibleMessage
                );


        hopeMessage.setTitle(
                finalTitle
        );


        hopeMessage.setActionType(
                actionType
        );


        hopeMessage.setArticleId(
                articleId
        );


        hopeMessage.setSourceName(
                sourceName
        );


        hopeMessage.setSourceUrl(
                sourceUrl
        );


        /*
         * Only show Read More when we have an article ID.
         */
        if (!articleId.isEmpty()) {

            hopeMessage.setActionLabel(
                    "Read More"
            );
        }


        /*
         * Do not invent related questions.
         * The backend does not currently return them.
         */
        addHopeMessage(
                hopeMessage
        );
    }


    // =====================================================
    // CREATE ARTICLE PREVIEW
    // =====================================================

    private String createArticlePreview(
            String content
    ) {

        String cleanContent =
                cleanText(
                        content
                );


        if (cleanContent.isEmpty()) {

            return "";
        }


        /*
         * Collapse excessive whitespace/newlines so the
         * preview remains compact inside a chat bubble.
         */
        cleanContent =
                cleanContent
                        .replaceAll(
                                "\\s+",
                                " "
                        )
                        .trim();


        if (cleanContent.length()
                <= ARTICLE_PREVIEW_LENGTH) {

            return cleanContent;
        }


        String shortened =
                cleanContent.substring(
                        0,
                        ARTICLE_PREVIEW_LENGTH
                );


        /*
         * Try not to cut a word in half.
         */
        int lastSpace =
                shortened.lastIndexOf(
                        " "
                );


        if (lastSpace > 220) {

            shortened =
                    shortened.substring(
                            0,
                            lastSpace
                    );
        }


        return shortened.trim()
                + "...";
    }


    // =====================================================
    // HOPE ACTION BUTTON
    // =====================================================

    private void handleHopeAction(
            HopeMessage message
    ) {

        if (message == null) {

            return;
        }


        String actionType =
                cleanText(
                        message.getActionType()
                );


        // =================================================
        // LEGAL ARTICLE
        // =================================================

        if ("legal_article".equalsIgnoreCase(
                actionType
        )) {

            openLegalArticle(
                    message
            );

            return;
        }


        // =================================================
        // EDUCATIONAL ARTICLE
        // =================================================

        if ("educational_article".equalsIgnoreCase(
                actionType
        )) {

            openEducationalArticle(
                    message
            );

            return;
        }


        // =================================================
        // HOTLINES
        // =================================================

        if ("emergency_hotlines".equalsIgnoreCase(
                actionType
        )) {

            openEmergencyHotlines();

            return;
        }


        // =================================================
        // EMERGENCY HELP
        // =================================================

        if ("emergency_help".equalsIgnoreCase(
                actionType
        )) {

            /*
             * This only opens the emergency hotline/resource
             * page.
             *
             * Hope does NOT:
             * - place a phone call
             * - activate Panic
             * - share location
             */
            openEmergencyHotlines();

            return;
        }


        // =================================================
        // FILE A NEW REPORT
        // =================================================

        if ("file_report".equalsIgnoreCase(
                actionType
        )) {

            openFileNewReport();

            return;
        }


        // =================================================
        // MY REPORTS
        // =================================================

        if ("my_reports".equalsIgnoreCase(
                actionType
        )) {

            openMyReports();

            return;
        }


        // =================================================
        // LEGAL RIGHTS
        // =================================================

        if ("legal_rights".equalsIgnoreCase(
                actionType
        )) {

            openLegalRights();

            return;
        }


        // =================================================
        // EDUCATIONAL ARTICLES
        // =================================================

        if ("educational_articles".equalsIgnoreCase(
                actionType
        )) {

            openEducationalArticles();

            return;
        }


        // =================================================
        // EXPLORE
        // =================================================

        if ("explore".equalsIgnoreCase(
                actionType
        )) {

            openExplore();

            return;
        }


        // =================================================
        // LATEST ANNOUNCEMENTS
        // =================================================

        if ("latest_announcements".equalsIgnoreCase(
                actionType
        )) {

            /*
             * Latest Announcements currently lives inside
             * ExploreActivity.
             *
             * We open Explore rather than inventing a separate
             * announcement Activity.
             */
            openExplore();

            return;
        }


        // =================================================
        // TINIG NG KABABAIHAN
        // =================================================

        if ("tinig_kababaihan".equalsIgnoreCase(
                actionType
        )) {

            /*
             * TNK currently lives inside ExploreActivity.
             */
            openExplore();

            return;
        }


        // =================================================
        // SUPPORT
        // =================================================

        if ("support".equalsIgnoreCase(
                actionType
        )) {

            openSupport();

            return;
        }


        // =================================================
        // LOCATION HELP
        // =================================================

        if ("location_help".equalsIgnoreCase(
                actionType
        )) {

            /*
             * This only opens HomeActivity.
             *
             * Hope does not enable location sharing.
             */
            openHome();

            return;
        }


        // =================================================
        // PANIC HELP
        // =================================================

        if ("panic_help".equalsIgnoreCase(
                actionType
        )) {

            /*
             * This only opens HomeActivity.
             *
             * Hope does not activate the Panic button.
             */
            openHome();

            return;
        }


        // =================================================
        // UNKNOWN ACTION
        // =================================================

        Toast.makeText(
                this,
                "This option is not available right now.",
                Toast.LENGTH_SHORT
        ).show();
    }


    // =====================================================
    // OPEN LEGAL ARTICLE
    // =====================================================

    private void openLegalArticle(
            HopeMessage message
    ) {

        String articleId =
                cleanText(
                        message.getArticleId()
                );


        if (articleId.isEmpty()) {

            Toast.makeText(
                    this,
                    "Article details are unavailable.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        Intent intent =
                new Intent(
                        ChatbotActivity.this,
                        LegalArticleDetailsActivity.class
                );


        /*
         * Use the article ID returned by Supabase.
         *
         * The details page remains responsible for loading
         * the current article from Supabase.
         */
        intent.putExtra(
                "article_id",
                articleId
        );


        startActivity(
                intent
        );
    }


    // =====================================================
    // OPEN EDUCATIONAL ARTICLE
    // =====================================================

    private void openEducationalArticle(
            HopeMessage message
    ) {

        String articleId =
                cleanText(
                        message.getArticleId()
                );


        if (articleId.isEmpty()) {

            Toast.makeText(
                    this,
                    "Article details are unavailable.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        Intent intent =
                new Intent(
                        ChatbotActivity.this,
                        EducationalArticleDetailsActivity.class
                );


        intent.putExtra(
                "article_id",
                articleId
        );


        startActivity(
                intent
        );
    }


    // =====================================================
    // OPEN EMERGENCY HOTLINES
    // =====================================================

    private void openEmergencyHotlines() {

        Intent intent =
                new Intent(
                        ChatbotActivity.this,
                        EmergencyHotlinesActivity.class
                );


        startActivity(
                intent
        );
    }


    // =====================================================
    // OPEN FILE A NEW REPORT
    // =====================================================

    private void openFileNewReport() {

        Intent intent =
                new Intent(
                        ChatbotActivity.this,
                        AddReportActivity.class
                );


        startActivity(
                intent
        );
    }


    // =====================================================
    // OPEN MY REPORTS
    // =====================================================

    private void openMyReports() {

        Intent intent =
                new Intent(
                        ChatbotActivity.this,
                        MyReportsActivity.class
                );


        startActivity(
                intent
        );
    }


    // =====================================================
    // OPEN LEGAL RIGHTS
    // =====================================================

    private void openLegalRights() {

        Intent intent =
                new Intent(
                        ChatbotActivity.this,
                        LegalRightsActivity.class
                );


        startActivity(
                intent
        );
    }


    // =====================================================
    // OPEN EDUCATIONAL ARTICLES
    // =====================================================

    private void openEducationalArticles() {

        Intent intent =
                new Intent(
                        ChatbotActivity.this,
                        EducationalArticlesActivity.class
                );


        startActivity(
                intent
        );
    }


    // =====================================================
    // OPEN EXPLORE
    // =====================================================

    private void openExplore() {

        Intent intent =
                new Intent(
                        ChatbotActivity.this,
                        ExploreActivity.class
                );


        startActivity(
                intent
        );
    }


    // =====================================================
    // OPEN SUPPORT
    // =====================================================

    private void openSupport() {

        Intent intent =
                new Intent(
                        ChatbotActivity.this,
                        SupportActivity.class
                );


        startActivity(
                intent
        );
    }


    // =====================================================
    // OPEN HOME
    // =====================================================

    private void openHome() {

        Intent intent =
                new Intent(
                        ChatbotActivity.this,
                        HomeActivity.class
                );


        startActivity(
                intent
        );
    }


    // =====================================================
    // CLEAN TEXT
    // =====================================================

    private String cleanText(
            String value
    ) {

        if (value == null) {

            return "";
        }


        return value.trim();
    }


    // =====================================================
    // HANDLE HOPE HTTP ERROR
    // =====================================================

    private void handleHopeHttpError(
            int statusCode
    ) {

        if (statusCode == 401
                || statusCode == 403) {

            addHopeMessage(
                    "Your session could not be verified. Please log in again and try asking Hope."
            );

            return;
        }


        if (statusCode == 400) {

            addHopeMessage(
                    "I couldn't process that question. Please try asking it another way."
            );

            return;
        }


        addHopeMessage(
                "Hope is temporarily unavailable. Please try again in a moment."
        );
    }


    // =====================================================
    // REQUEST LOADING STATE
    // =====================================================

    private void setHopeLoading(
            boolean loading
    ) {

        isHopeRequestRunning =
                loading;


        if (btnHopeSend != null) {

            btnHopeSend.setEnabled(
                    !loading
            );


            btnHopeSend.setAlpha(
                    loading
                            ? 0.5f
                            : 1.0f
            );
        }


        if (edtHopeMessage != null) {

            edtHopeMessage.setEnabled(
                    !loading
            );
        }
    }


    // =====================================================
    // ADD USER MESSAGE
    // =====================================================

    private void addUserMessage(
            String message
    ) {

        if (message == null
                || message.trim().isEmpty()) {

            return;
        }


        HopeMessage userMessage =
                new HopeMessage(
                        HopeMessage.TYPE_USER,
                        message.trim()
                );


        messages.add(
                userMessage
        );


        int insertedPosition =
                messages.size() - 1;


        hopeMessageAdapter.notifyItemInserted(
                insertedPosition
        );


        scrollToLatestMessage();
    }


    // =====================================================
    // ADD SIMPLE HOPE MESSAGE
    // =====================================================

    private void addHopeMessage(
            String message
    ) {

        if (message == null
                || message.trim().isEmpty()) {

            return;
        }


        HopeMessage hopeMessage =
                new HopeMessage(
                        HopeMessage.TYPE_HOPE,
                        message.trim()
                );


        addHopeMessage(
                hopeMessage
        );
    }


    // =====================================================
    // ADD STRUCTURED HOPE MESSAGE
    // =====================================================

    private void addHopeMessage(
            HopeMessage hopeMessage
    ) {

        if (hopeMessage == null) {

            return;
        }


        String message =
                hopeMessage.getMessage();


        if (message == null
                || message.trim().isEmpty()) {

            return;
        }


        messages.add(
                hopeMessage
        );


        int insertedPosition =
                messages.size() - 1;


        hopeMessageAdapter.notifyItemInserted(
                insertedPosition
        );


        scrollToLatestMessage();
    }


    // =====================================================
    // SCROLL TO LATEST MESSAGE
    // =====================================================

    private void scrollToLatestMessage() {

        if (messages.isEmpty()) {

            return;
        }


        recyclerHopeMessages.post(
                () ->
                        recyclerHopeMessages
                                .smoothScrollToPosition(
                                        messages.size() - 1
                                )
        );
    }


    // =====================================================
    // CLEANUP
    // =====================================================

    @Override
    protected void onDestroy() {

        /*
         * Cancel only Hope's refresh request.
         */
        if (activeRefreshCall != null) {

            activeRefreshCall.cancel();

            activeRefreshCall = null;
        }


        /*
         * Cancel only Hope's current chat request.
         */
        if (activeHopeCall != null) {

            activeHopeCall.cancel();

            activeHopeCall = null;
        }


        super.onDestroy();
    }
}
