package com.example.havenapp.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.havenapp.R;
import com.example.havenapp.model.EmergencyHotline;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EmergencyHotlineAdapter
        extends RecyclerView.Adapter<EmergencyHotlineAdapter.HotlineViewHolder> {

    private final Context context;
    private final List<EmergencyHotline> hotlineList;

    /*
     * Stores which hotline cards are currently expanded.
     * This prevents RecyclerView recycling from randomly
     * opening or closing cards.
     */
    private final Set<String> expandedHotlineIds =
            new HashSet<>();


    public EmergencyHotlineAdapter(
            Context context,
            List<EmergencyHotline> hotlineList
    ) {

        this.context = context;
        this.hotlineList = hotlineList;
    }


    @NonNull
    @Override
    public HotlineViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater.from(
                        parent.getContext()
                ).inflate(
                        R.layout.item_emergency_hotline,
                        parent,
                        false
                );

        return new HotlineViewHolder(view);
    }


    @Override
    public void onBindViewHolder(
            @NonNull HotlineViewHolder holder,
            int position
    ) {

        EmergencyHotline hotline =
                hotlineList.get(position);

        holder.txtHotlineName.setText(
                hotline.getName()
        );


        // =============================================
        // CHECK EXPANDED STATE
        // =============================================

        String hotlineKey =
                getHotlineKey(
                        hotline,
                        position
                );

        boolean isExpanded =
                expandedHotlineIds.contains(
                        hotlineKey
                );


        holder.layoutHotlineNumbers.setVisibility(
                isExpanded
                        ? View.VISIBLE
                        : View.GONE
        );


        holder.imgExpandHotline.setRotation(
                isExpanded
                        ? 180f
                        : 0f
        );


        // =============================================
        // BUILD PHONE NUMBER ROWS
        // =============================================

        holder.layoutHotlineNumbers.removeAllViews();

        if (isExpanded) {

            List<String> phoneNumbers =
                    hotline.getPhoneNumbers();

            if (phoneNumbers != null) {

                for (String phoneNumber : phoneNumbers) {

                    if (phoneNumber == null
                            || phoneNumber.trim().isEmpty()) {

                        continue;
                    }

                    View numberRow =
                            createNumberRow(
                                    phoneNumber.trim()
                            );

                    holder.layoutHotlineNumbers.addView(
                            numberRow
                    );
                }
            }
        }


        // =============================================
        // EXPAND / COLLAPSE
        // =============================================

        holder.layoutHotlineHeader.setOnClickListener(v -> {

            boolean currentlyExpanded =
                    expandedHotlineIds.contains(
                            hotlineKey
                    );

            if (currentlyExpanded) {

                expandedHotlineIds.remove(
                        hotlineKey
                );

            } else {

                expandedHotlineIds.add(
                        hotlineKey
                );
            }

            notifyItemChanged(
                    holder.getBindingAdapterPosition()
            );
        });
    }


    // =================================================
    // CREATE NUMBER + COPY ICON ROW
    // =================================================

    private View createNumberRow(
            String phoneNumber
    ) {

        LinearLayout row =
                new LinearLayout(
                        context
                );

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        row.setGravity(
                Gravity.CENTER_VERTICAL
        );


        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        rowParams.topMargin =
                dpToPx(4);

        rowParams.bottomMargin =
                dpToPx(4);

        row.setLayoutParams(
                rowParams
        );


        // =============================================
        // PHONE NUMBER
        // =============================================

        TextView txtNumber =
                new TextView(
                        context
                );

        txtNumber.setText(
                formatPhoneNumber(
                        phoneNumber
                )
        );

        txtNumber.setTextColor(
                Color.parseColor(
                        "#32118F"
                )
        );

        txtNumber.setTextSize(
                14
        );

        txtNumber.setGravity(
                Gravity.CENTER_VERTICAL
        );


        LinearLayout.LayoutParams numberParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                );

        txtNumber.setLayoutParams(
                numberParams
        );


        // =============================================
        // SMALL COPY ICON
        // =============================================

        ImageView btnCopy =
                new ImageView(
                        context
                );

        btnCopy.setImageResource(
                R.drawable.ic_copy
        );

        btnCopy.setPadding(
                dpToPx(6),
                dpToPx(6),
                dpToPx(6),
                dpToPx(6)
        );

        btnCopy.setContentDescription(
                "Copy number"
        );

        btnCopy.setClickable(
                true
        );

        btnCopy.setFocusable(
                true
        );


        LinearLayout.LayoutParams copyParams =
                new LinearLayout.LayoutParams(
                        dpToPx(30),
                        dpToPx(30)
                );

        btnCopy.setLayoutParams(
                copyParams
        );


        btnCopy.setOnClickListener(v ->
                copyPhoneNumber(
                        phoneNumber
                )
        );


        // =============================================
        // ADD VIEWS
        // =============================================

        row.addView(
                txtNumber
        );

        row.addView(
                btnCopy
        );


        return row;
    }


    // =================================================
    // COPY NUMBER
    // =================================================

    private void copyPhoneNumber(
            String phoneNumber
    ) {

        ClipboardManager clipboardManager =
                (ClipboardManager)
                        context.getSystemService(
                                Context.CLIPBOARD_SERVICE
                        );

        if (clipboardManager == null) {

            Toast.makeText(
                    context,
                    "Unable to copy number",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        ClipData clipData =
                ClipData.newPlainText(
                        "Emergency hotline number",
                        phoneNumber
                );


        clipboardManager.setPrimaryClip(
                clipData
        );


        Toast.makeText(
                context,
                "Number copied",
                Toast.LENGTH_SHORT
        ).show();
    }


    // =================================================
    // FORMAT PHONE NUMBER FOR DISPLAY
    // =================================================

    private String formatPhoneNumber(
            String phoneNumber
    ) {

        /*
         * Supabase:
         * 09639883134
         *
         * Display:
         * 0963 988 3134
         */

        String cleanNumber =
                phoneNumber.replaceAll(
                        "\\s+",
                        ""
                );


        if (cleanNumber.matches("\\d{11}")) {

            return cleanNumber.substring(0, 4)
                    + " "
                    + cleanNumber.substring(4, 7)
                    + " "
                    + cleanNumber.substring(7);
        }


        /*
         * If a future hotline uses a different
         * number format, display it unchanged
         * instead of risking incorrect formatting.
         */
        return phoneNumber;
    }


    // =================================================
    // HOTLINE KEY
    // =================================================

    private String getHotlineKey(
            EmergencyHotline hotline,
            int position
    ) {

        if (hotline.getId() != null
                && !hotline.getId().trim().isEmpty()) {

            return hotline.getId();
        }

        return "hotline_" + position;
    }


    // =================================================
    // DP → PX
    // =================================================

    private int dpToPx(
            int dp
    ) {

        return Math.round(
                dp
                        * context
                        .getResources()
                        .getDisplayMetrics()
                        .density
        );
    }


    // =================================================
    // ITEM COUNT
    // =================================================

    @Override
    public int getItemCount() {

        return hotlineList != null
                ? hotlineList.size()
                : 0;
    }


    // =================================================
    // VIEW HOLDER
    // =================================================

    static class HotlineViewHolder
            extends RecyclerView.ViewHolder {

        LinearLayout layoutHotlineHeader;
        LinearLayout layoutHotlineNumbers;

        TextView txtHotlineName;

        ImageView imgExpandHotline;


        HotlineViewHolder(
                @NonNull View itemView
        ) {

            super(itemView);

            layoutHotlineHeader =
                    itemView.findViewById(
                            R.id.layoutHotlineHeader
                    );

            layoutHotlineNumbers =
                    itemView.findViewById(
                            R.id.layoutHotlineNumbers
                    );

            txtHotlineName =
                    itemView.findViewById(
                            R.id.txtHotlineName
                    );

            imgExpandHotline =
                    itemView.findViewById(
                            R.id.imgExpandHotline
                    );
        }
    }
}