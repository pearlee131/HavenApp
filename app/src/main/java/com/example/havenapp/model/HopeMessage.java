package com.example.havenapp.model;

public class HopeMessage {

    // =====================================================
    // MESSAGE TYPES
    // =====================================================

    public static final int TYPE_WELCOME = 0;
    public static final int TYPE_HOPE = 1;
    public static final int TYPE_USER = 2;


    // =====================================================
    // BASIC MESSAGE DATA
    // =====================================================

    private final int type;

    private final String message;


    // =====================================================
    // OPTIONAL HOPE RESPONSE DATA
    // =====================================================

    /*
     * These fields are used only when Hope returns
     * a structured response.
     *
     * Normal user messages and the welcome message
     * can leave these values empty/null.
     */

    private String title;

    private String actionType;

    private String actionLabel;

    private String articleId;

    private String sourceName;

    private String sourceUrl;

    private String relatedQuestions;


    // =====================================================
    // EXISTING CONSTRUCTOR
    // =====================================================

    /*
     * IMPORTANT:
     *
     * Keep this constructor because ChatbotActivity and
     * the existing welcome/user-message code already use it.
     */

    public HopeMessage(
            int type,
            String message
    ) {

        this.type = type;
        this.message = message;
    }


    // =====================================================
    // BASIC GETTERS
    // =====================================================

    public int getType() {

        return type;
    }


    public String getMessage() {

        return message;
    }


    // =====================================================
    // OPTIONAL RESPONSE GETTERS
    // =====================================================

    public String getTitle() {

        return title;
    }


    public String getActionType() {

        return actionType;
    }


    public String getActionLabel() {

        return actionLabel;
    }


    public String getArticleId() {

        return articleId;
    }


    public String getSourceName() {

        return sourceName;
    }


    public String getSourceUrl() {

        return sourceUrl;
    }


    public String getRelatedQuestions() {

        return relatedQuestions;
    }


    // =====================================================
    // OPTIONAL RESPONSE SETTERS
    // =====================================================

    public void setTitle(
            String title
    ) {

        this.title = title;
    }


    public void setActionType(
            String actionType
    ) {

        this.actionType = actionType;
    }


    public void setActionLabel(
            String actionLabel
    ) {

        this.actionLabel = actionLabel;
    }


    public void setArticleId(
            String articleId
    ) {

        this.articleId = articleId;
    }


    public void setSourceName(
            String sourceName
    ) {

        this.sourceName = sourceName;
    }


    public void setSourceUrl(
            String sourceUrl
    ) {

        this.sourceUrl = sourceUrl;
    }


    public void setRelatedQuestions(
            String relatedQuestions
    ) {

        this.relatedQuestions = relatedQuestions;
    }


    // =====================================================
    // OPTIONAL DATA HELPERS
    // =====================================================

    public boolean hasAction() {

        return actionLabel != null
                && !actionLabel.trim().isEmpty();
    }


    public boolean hasSource() {

        return sourceName != null
                && !sourceName.trim().isEmpty();
    }


    public boolean hasSourceUrl() {

        return sourceUrl != null
                && !sourceUrl.trim().isEmpty();
    }


    public boolean hasRelatedQuestions() {

        return relatedQuestions != null
                && !relatedQuestions.trim().isEmpty();
    }


    // =====================================================
    // TYPE HELPERS
    // =====================================================

    public boolean isWelcome() {

        return type == TYPE_WELCOME;
    }


    public boolean isFromHope() {

        return type == TYPE_HOPE;
    }


    public boolean isFromUser() {

        return type == TYPE_USER;
    }
}