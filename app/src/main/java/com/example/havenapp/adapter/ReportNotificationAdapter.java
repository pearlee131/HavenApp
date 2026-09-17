package com.example.havenapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.havenapp.R;
import com.example.havenapp.model.ReportNotification;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ReportNotificationAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // =========================================================
    // VIEW TYPES
    // =========================================================

    private static final int VIEW_TYPE_DATE_HEADER = 0;
    private static final int VIEW_TYPE_NOTIFICATION = 1;


    // =========================================================
    // CLICK LISTENER
    // =========================================================

    public interface OnNotificationClickListener {

        void onNotificationClick(
                ReportNotification notification
        );
    }


    // =========================================================
    // DISPLAY ITEM
    //
    // Each RecyclerView position is either:
    // 1. a date header, or
    // 2. a real ReportNotification.
    // =========================================================

    private static class DisplayItem {

        private final boolean header;

        private final String headerText;

        private final ReportNotification notification;


        private DisplayItem(
                boolean header,
                String headerText,
                ReportNotification notification
        ) {

            this.header = header;
            this.headerText = headerText;
            this.notification = notification;
        }


        static DisplayItem createHeader(
                String headerText
        ) {

            return new DisplayItem(
                    true,
                    headerText,
                    null
            );
        }


        static DisplayItem createNotification(
                ReportNotification notification
        ) {

            return new DisplayItem(
                    false,
                    null,
                    notification
            );
        }
    }


    // =========================================================
    // DATA
    // =========================================================

    /*
     * Original notification objects.
     *
     * We keep these so NotificationsActivity can still update
     * the same ReportNotification object after marking it read.
     */

    private final List<ReportNotification> notifications =
            new ArrayList<>();


    /*
     * RecyclerView display list.
     *
     * Example:
     *
     * Today
     * Notification
     * Notification
     * Yesterday
     * Notification
     * September 15, 2026
     * Notification
     */

    private final List<DisplayItem> displayItems =
            new ArrayList<>();


    private final OnNotificationClickListener listener;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ReportNotificationAdapter(
            OnNotificationClickListener listener
    ) {

        this.listener = listener;
    }


    // =========================================================
    // UPDATE LIST
    // =========================================================

    public void setNotifications(
            List<ReportNotification> newNotifications
    ) {

        notifications.clear();

        displayItems.clear();


        if (newNotifications != null) {

            notifications.addAll(
                    newNotifications
            );
        }


        buildGroupedDisplayItems();

        notifyDataSetChanged();
    }


    // =========================================================
    // BUILD GROUPED DISPLAY LIST
    // =========================================================

    private void buildGroupedDisplayItems() {

        displayItems.clear();


        String previousGroupKey =
                null;


        for (ReportNotification notification : notifications) {

            if (notification == null) {
                continue;
            }


            Date notificationDate =
                    parseNotificationDate(
                            notification.getCreatedAt()
                    );


            String groupKey =
                    getDateGroupKey(
                            notificationDate
                    );


            String groupTitle =
                    getDateGroupTitle(
                            notificationDate
                    );


            /*
             * Add a new date header only when the date group
             * changes.
             *
             * Notifications are already loaded newest-first
             * from Supabase using created_at.desc.
             */

            if (previousGroupKey == null
                    || !previousGroupKey.equals(groupKey)) {

                displayItems.add(
                        DisplayItem.createHeader(
                                groupTitle
                        )
                );


                previousGroupKey =
                        groupKey;
            }


            displayItems.add(
                    DisplayItem.createNotification(
                            notification
                    )
            );
        }
    }


    // =========================================================
    // ITEM VIEW TYPE
    // =========================================================

    @Override
    public int getItemViewType(
            int position
    ) {

        DisplayItem item =
                displayItems.get(position);


        if (item.header) {

            return VIEW_TYPE_DATE_HEADER;
        }


        return VIEW_TYPE_NOTIFICATION;
    }


    // =========================================================
    // CREATE VIEW HOLDER
    // =========================================================

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        if (viewType == VIEW_TYPE_DATE_HEADER) {

            View view =
                    LayoutInflater
                            .from(parent.getContext())
                            .inflate(
                                    R.layout.item_notification_date_header,
                                    parent,
                                    false
                            );


            return new DateHeaderViewHolder(
                    view
            );
        }


        View view =
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(
                                R.layout.item_report_notification,
                                parent,
                                false
                        );


        return new NotificationViewHolder(
                view
        );
    }


    // =========================================================
    // BIND VIEW HOLDER
    // =========================================================

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position
    ) {

        DisplayItem displayItem =
                displayItems.get(position);


        // -----------------------------------------------------
        // DATE HEADER
        // -----------------------------------------------------

        if (holder instanceof DateHeaderViewHolder) {

            DateHeaderViewHolder headerHolder =
                    (DateHeaderViewHolder) holder;


            headerHolder
                    .txtNotificationDateHeader
                    .setText(
                            safeText(
                                    displayItem.headerText,
                                    ""
                            )
                    );


            return;
        }


        // -----------------------------------------------------
        // NOTIFICATION CARD
        // -----------------------------------------------------

        if (!(holder instanceof NotificationViewHolder)) {
            return;
        }


        ReportNotification notification =
                displayItem.notification;


        if (notification == null) {
            return;
        }


        NotificationViewHolder notificationHolder =
                (NotificationViewHolder) holder;


        bindNotification(
                notificationHolder,
                notification
        );
    }


    // =========================================================
    // BIND NOTIFICATION
    // =========================================================

    private void bindNotification(
            NotificationViewHolder holder,
            ReportNotification notification
    ) {

        // -----------------------------------------------------
        // TITLE
        // -----------------------------------------------------

        holder.txtNotificationTitle.setText(
                safeText(
                        notification.getTitle(),
                        "Report Update"
                )
        );


        // -----------------------------------------------------
        // MESSAGE
        // -----------------------------------------------------

        holder.txtNotificationMessage.setText(
                safeText(
                        notification.getMessage(),
                        ""
                )
        );


        // -----------------------------------------------------
        // CASE NUMBER
        // -----------------------------------------------------

        String caseNumber =
                notification.getCaseNumber();


        if (caseNumber == null
                || caseNumber.trim().isEmpty()) {

            holder.txtNotificationCaseNumber
                    .setVisibility(
                            View.GONE
                    );

        } else {

            holder.txtNotificationCaseNumber
                    .setVisibility(
                            View.VISIBLE
                    );


            holder.txtNotificationCaseNumber
                    .setText(
                            caseNumber.trim()
                    );
        }


        // -----------------------------------------------------
        // TIME
        // -----------------------------------------------------

        holder.txtNotificationTime.setText(
                formatNotificationTime(
                        notification.getCreatedAt()
                )
        );


        // -----------------------------------------------------
        // READ / UNREAD
        // -----------------------------------------------------

        if (notification.isRead()) {

            // Already read

            holder.viewUnreadDot
                    .setVisibility(
                            View.GONE
                    );


            holder.cardReportNotification
                    .setCardBackgroundColor(
                            0xFFFDFBFF
                    );


            holder.cardReportNotification
                    .setStrokeColor(
                            0xFFEEE9F6
                    );


            holder.txtNotificationTitle
                    .setAlpha(
                            0.78f
                    );

            holder.txtNotificationMessage
                    .setAlpha(
                            0.72f
                    );

            holder.txtNotificationCaseNumber
                    .setAlpha(
                            0.72f
                    );

            holder.txtNotificationTime
                    .setAlpha(
                            0.72f
                    );

        } else {

            // Unread

            holder.viewUnreadDot
                    .setVisibility(
                            View.VISIBLE
                    );


            holder.cardReportNotification
                    .setCardBackgroundColor(
                            0xFFFFFFFF
                    );


            holder.cardReportNotification
                    .setStrokeColor(
                            0xFFE4D9F7
                    );


            holder.txtNotificationTitle
                    .setAlpha(
                            1.0f
                    );

            holder.txtNotificationMessage
                    .setAlpha(
                            1.0f
                    );

            holder.txtNotificationCaseNumber
                    .setAlpha(
                            1.0f
                    );

            holder.txtNotificationTime
                    .setAlpha(
                            1.0f
                    );
        }


        // -----------------------------------------------------
        // CLICK
        // -----------------------------------------------------

        holder.cardReportNotification
                .setOnClickListener(
                        v -> {

                            if (listener != null) {

                                listener.onNotificationClick(
                                        notification
                                );
                            }
                        }
                );
    }


    // =========================================================
    // ITEM COUNT
    // =========================================================

    @Override
    public int getItemCount() {

        return displayItems.size();
    }


    // =========================================================
    // SAFE TEXT
    // =========================================================

    private String safeText(
            String value,
            String fallback
    ) {

        if (value == null
                || value.trim().isEmpty()) {

            return fallback;
        }


        return value.trim();
    }


    // =========================================================
    // DATE GROUP KEY
    //
    // This is deliberately separate from the visible title.
    // It makes sure notifications from the same local calendar
    // date are grouped together.
    // =========================================================

    private String getDateGroupKey(
            Date date
    ) {

        if (date == null) {

            return "unknown";
        }


        SimpleDateFormat keyFormat =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                );


        keyFormat.setTimeZone(
                TimeZone.getDefault()
        );


        return keyFormat.format(
                date
        );
    }


    // =========================================================
    // DATE GROUP TITLE
    // =========================================================

    private String getDateGroupTitle(
            Date date
    ) {

        if (date == null) {

            return "Earlier";
        }


        Calendar notificationCalendar =
                Calendar.getInstance();

        notificationCalendar.setTime(
                date
        );


        Calendar todayCalendar =
                Calendar.getInstance();


        // -----------------------------------------------------
        // TODAY
        // -----------------------------------------------------

        if (isSameDay(
                notificationCalendar,
                todayCalendar
        )) {

            return "Today";
        }


        // -----------------------------------------------------
        // YESTERDAY
        // -----------------------------------------------------

        Calendar yesterdayCalendar =
                Calendar.getInstance();

        yesterdayCalendar.add(
                Calendar.DAY_OF_YEAR,
                -1
        );


        if (isSameDay(
                notificationCalendar,
                yesterdayCalendar
        )) {

            return "Yesterday";
        }


        // -----------------------------------------------------
        // OLDER DATE
        // Example: September 15, 2026
        // -----------------------------------------------------

        SimpleDateFormat dateFormat =
                new SimpleDateFormat(
                        "MMMM d, yyyy",
                        Locale.getDefault()
                );


        dateFormat.setTimeZone(
                TimeZone.getDefault()
        );


        return dateFormat.format(
                date
        );
    }


    // =========================================================
    // SAME LOCAL CALENDAR DAY
    // =========================================================

    private boolean isSameDay(
            Calendar first,
            Calendar second
    ) {

        return first.get(Calendar.YEAR)
                == second.get(Calendar.YEAR)

                &&

                first.get(Calendar.DAY_OF_YEAR)
                        == second.get(Calendar.DAY_OF_YEAR);
    }


    // =========================================================
    // FORMAT NOTIFICATION TIME
    // =========================================================

    private String formatNotificationTime(
            String createdAt
    ) {

        Date date =
                parseNotificationDate(
                        createdAt
                );


        if (date == null) {

            return "";
        }


        SimpleDateFormat outputFormat =
                new SimpleDateFormat(
                        "h:mm a",
                        Locale.getDefault()
                );


        outputFormat.setTimeZone(
                TimeZone.getDefault()
        );


        return outputFormat.format(
                date
        );
    }


    // =========================================================
    // PARSE SUPABASE TIMESTAMP
    // =========================================================

    private Date parseNotificationDate(
            String createdAt
    ) {

        if (createdAt == null
                || createdAt.trim().isEmpty()) {

            return null;
        }


        try {

            /*
             * Supabase example:
             *
             * 2026-09-17T12:29:16.337057+00:00
             *
             * Fractional seconds can contain more than
             * 3 digits, so normalize them first.
             */

            String normalized =
                    normalizeSupabaseTimestamp(
                            createdAt.trim()
                    );


            SimpleDateFormat inputFormat =
                    new SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                            Locale.US
                    );


            inputFormat.setTimeZone(
                    TimeZone.getTimeZone("UTC")
            );


            return inputFormat.parse(
                    normalized
            );


        } catch (Exception ignored) {

            return null;
        }
    }


    // =========================================================
    // NORMALIZE SUPABASE TIMESTAMP
    // =========================================================

    private String normalizeSupabaseTimestamp(
            String timestamp
    ) {

        String value =
                timestamp.trim();


        /*
         * Convert Z into +00:00 so XXX can parse it
         * consistently.
         */

        if (value.endsWith("Z")) {

            value =
                    value.substring(
                            0,
                            value.length() - 1
                    ) + "+00:00";
        }


        int tIndex =
                value.indexOf('T');


        if (tIndex < 0) {

            return value;
        }


        int plusIndex =
                value.indexOf(
                        '+',
                        tIndex
                );


        int minusIndex =
                value.indexOf(
                        '-',
                        tIndex
                );


        int timezoneIndex;


        if (plusIndex >= 0) {

            timezoneIndex =
                    plusIndex;

        } else {

            timezoneIndex =
                    minusIndex;
        }


        if (timezoneIndex < 0) {

            value =
                    value + "+00:00";


            timezoneIndex =
                    value.indexOf(
                            '+',
                            tIndex
                    );
        }


        String dateTimePart =
                value.substring(
                        0,
                        timezoneIndex
                );


        String timezonePart =
                value.substring(
                        timezoneIndex
                );


        int dotIndex =
                dateTimePart.indexOf('.');


        if (dotIndex >= 0) {

            String beforeFraction =
                    dateTimePart.substring(
                            0,
                            dotIndex
                    );


            String fraction =
                    dateTimePart.substring(
                            dotIndex + 1
                    );


            if (fraction.length() > 3) {

                fraction =
                        fraction.substring(
                                0,
                                3
                        );

            } else {

                while (fraction.length() < 3) {

                    fraction =
                            fraction + "0";
                }
            }


            dateTimePart =
                    beforeFraction
                            + "."
                            + fraction;

        } else {

            dateTimePart =
                    dateTimePart
                            + ".000";
        }


        return dateTimePart
                + timezonePart;
    }


    // =========================================================
    // DATE HEADER VIEW HOLDER
    // =========================================================

    static class DateHeaderViewHolder
            extends RecyclerView.ViewHolder {

        TextView txtNotificationDateHeader;


        public DateHeaderViewHolder(
                @NonNull View itemView
        ) {

            super(itemView);


            txtNotificationDateHeader =
                    itemView.findViewById(
                            R.id.txtNotificationDateHeader
                    );
        }
    }


    // =========================================================
    // NOTIFICATION VIEW HOLDER
    // =========================================================

    static class NotificationViewHolder
            extends RecyclerView.ViewHolder {

        MaterialCardView cardReportNotification;

        MaterialCardView cardNotificationIcon;

        ImageView imgNotificationIcon;

        TextView txtNotificationTitle;

        TextView txtNotificationMessage;

        TextView txtNotificationCaseNumber;

        TextView txtNotificationTime;

        View viewUnreadDot;


        public NotificationViewHolder(
                @NonNull View itemView
        ) {

            super(itemView);


            cardReportNotification =
                    itemView.findViewById(
                            R.id.cardReportNotification
                    );


            cardNotificationIcon =
                    itemView.findViewById(
                            R.id.cardNotificationIcon
                    );


            imgNotificationIcon =
                    itemView.findViewById(
                            R.id.imgNotificationIcon
                    );


            txtNotificationTitle =
                    itemView.findViewById(
                            R.id.txtNotificationTitle
                    );


            txtNotificationMessage =
                    itemView.findViewById(
                            R.id.txtNotificationMessage
                    );


            txtNotificationCaseNumber =
                    itemView.findViewById(
                            R.id.txtNotificationCaseNumber
                    );


            txtNotificationTime =
                    itemView.findViewById(
                            R.id.txtNotificationTime
                    );


            viewUnreadDot =
                    itemView.findViewById(
                            R.id.viewUnreadDot
                    );
        }
    }
}