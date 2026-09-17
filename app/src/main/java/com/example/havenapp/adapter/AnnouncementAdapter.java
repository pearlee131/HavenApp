package com.example.havenapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.havenapp.R;
import com.example.havenapp.model.Announcement;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class AnnouncementAdapter
        extends RecyclerView.Adapter<AnnouncementAdapter.AnnouncementViewHolder> {

    private final Context context;
    private final List<Announcement> announcements;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public AnnouncementAdapter(
            Context context,
            List<Announcement> announcements
    ) {

        this.context =
                context;

        this.announcements =
                announcements;
    }


    // =====================================================
    // CREATE VIEW HOLDER
    // =====================================================

    @NonNull
    @Override
    public AnnouncementViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(
                                R.layout.item_announcement,
                                parent,
                                false
                        );


        return new AnnouncementViewHolder(
                view
        );
    }


    // =====================================================
    // BIND ANNOUNCEMENT
    // =====================================================

    @Override
    public void onBindViewHolder(
            @NonNull AnnouncementViewHolder holder,
            int position
    ) {

        Announcement announcement =
                announcements.get(
                        position
                );


        // =================================================
        // SOURCE
        // =================================================

        holder.txtSource.setText(
                getShortSourceName(
                        announcement.getSourceName()
                )
        );


        // =================================================
        // CATEGORY
        // =================================================

        String category =
                announcement.getCategory();


        if (
                category == null
                        ||
                        category.trim().isEmpty()
        ) {

            holder.txtCategory.setText(
                    "Safety & Rights"
            );

        } else {

            holder.txtCategory.setText(
                    category
            );
        }


        // =================================================
        // TITLE
        // =================================================

        String title =
                announcement.getTitle();


        if (
                title == null
                        ||
                        title.trim().isEmpty()
        ) {

            holder.txtTitle.setText(
                    "Official Announcement"
            );

        } else {

            holder.txtTitle.setText(
                    title.trim()
            );
        }


        // =================================================
        // DATE
        // =================================================

        holder.txtDate.setText(
                formatPublishedDate(
                        announcement.getPublishedAt()
                )
        );


        // =================================================
        // THUMBNAIL
        // =================================================

        loadAnnouncementImage(
                holder.imgAnnouncement,
                announcement.getImageUrl()
        );


        // =================================================
        // OPEN OFFICIAL SOURCE
        // =================================================

        holder.itemView.setOnClickListener(
                v -> openOfficialSource(
                        announcement.getSourceUrl()
                )
        );
    }


    // =====================================================
    // ITEM COUNT
    // =====================================================

    @Override
    public int getItemCount() {

        return announcements.size();
    }


    // =====================================================
    // LOAD ANNOUNCEMENT IMAGE
    // =====================================================

    private void loadAnnouncementImage(
            ImageView imageView,
            String imageUrl
    ) {

        if (
                imageUrl == null
                        ||
                        imageUrl.trim().isEmpty()
        ) {

            // No image from official source.
            // Uses HavenApp placeholder.

            imageView.setImageResource(
                    R.drawable.bg_announcement_placeholder
            );

            return;
        }


        Glide
                .with(context)
                .load(
                        imageUrl
                )
                .placeholder(
                        R.drawable.bg_announcement_placeholder
                )
                .error(
                        R.drawable.bg_announcement_placeholder
                )
                .centerCrop()
                .into(
                        imageView
                );
    }


    // =====================================================
    // OPEN OFFICIAL ANNOUNCEMENT PAGE
    // =====================================================

    private void openOfficialSource(
            String sourceUrl
    ) {

        if (
                sourceUrl == null
                        ||
                        sourceUrl.trim().isEmpty()
        ) {

            Toast.makeText(
                    context,
                    "Official source is currently unavailable.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                    sourceUrl
                            )
                    );


            context.startActivity(
                    intent
            );


        } catch (Exception e) {

            Toast.makeText(
                    context,
                    "Unable to open the official source.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }


    // =====================================================
    // SHORT SOURCE NAME
    // =====================================================

    private String getShortSourceName(
            String sourceName
    ) {

        if (
                sourceName == null
        ) {

            return "Official Source";
        }


        String lower =
                sourceName
                        .toLowerCase(
                                Locale.ROOT
                        );


        if (
                lower.contains(
                        "inter-agency council"
                )
        ) {

            return "IACVAWC";
        }


        if (
                lower.contains(
                        "council for the welfare of children"
                )
        ) {

            return "CWC";
        }


        if (
                lower.contains(
                        "dilg"
                )
        ) {

            return "DILG VI";
        }


        if (
                lower.contains(
                        "dswd"
                )
        ) {

            return "DSWD VI";
        }


        return sourceName;
    }


    // =====================================================
    // FORMAT PUBLISHED DATE
    // =====================================================

    private String formatPublishedDate(
            String publishedAt
    ) {

        if (
                publishedAt == null
                        ||
                        publishedAt.trim().isEmpty()
        ) {

            return "";
        }


        try {

            String normalized =
                    publishedAt.trim();


            // -------------------------------------------------
            // Supabase example:
            //
            // 2026-09-02T03:11:59+00:00
            //
            // -------------------------------------------------

            if (
                    normalized.length()
                            >=
                            10
            ) {

                String dateOnly =
                        normalized.substring(
                                0,
                                10
                        );


                SimpleDateFormat inputFormat =
                        new SimpleDateFormat(
                                "yyyy-MM-dd",
                                Locale.US
                        );


                inputFormat.setTimeZone(
                        TimeZone.getTimeZone(
                                "UTC"
                        )
                );


                Date date =
                        inputFormat.parse(
                                dateOnly
                        );


                if (
                        date != null
                ) {

                    SimpleDateFormat outputFormat =
                            new SimpleDateFormat(
                                    "MMM d, yyyy",
                                    Locale.US
                            );


                    return outputFormat.format(
                            date
                    );
                }
            }


        } catch (Exception ignored) {

        }


        return publishedAt;
    }


    // =====================================================
    // VIEW HOLDER
    // =====================================================

    static class AnnouncementViewHolder
            extends RecyclerView.ViewHolder {

        ImageView imgAnnouncement;

        TextView txtSource;
        TextView txtCategory;
        TextView txtTitle;
        TextView txtDate;


        public AnnouncementViewHolder(
                @NonNull View itemView
        ) {

            super(
                    itemView
            );


            imgAnnouncement =
                    itemView.findViewById(
                            R.id.imgAnnouncement
                    );


            txtSource =
                    itemView.findViewById(
                            R.id.txtAnnouncementSource
                    );


            txtCategory =
                    itemView.findViewById(
                            R.id.txtAnnouncementCategory
                    );


            txtTitle =
                    itemView.findViewById(
                            R.id.txtAnnouncementTitle
                    );


            txtDate =
                    itemView.findViewById(
                            R.id.txtAnnouncementDate
                    );
        }
    }
}