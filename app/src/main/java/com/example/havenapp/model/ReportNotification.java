package com.example.havenapp.model;

import com.google.gson.annotations.SerializedName;

public class ReportNotification {

    @SerializedName("id")
    private String id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("report_id")
    private String reportId;

    @SerializedName("case_number")
    private String caseNumber;

    @SerializedName("notification_type")
    private String notificationType;

    @SerializedName("title")
    private String title;

    @SerializedName("message")
    private String message;

    @SerializedName("is_read")
    private boolean isRead;

    @SerializedName("created_at")
    private String createdAt;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ReportNotification() {
    }


    // =========================================================
    // GETTERS
    // =========================================================

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getReportId() {
        return reportId;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return isRead;
    }

    public String getCreatedAt() {
        return createdAt;
    }


    // =========================================================
    // SETTERS
    // =========================================================

    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}