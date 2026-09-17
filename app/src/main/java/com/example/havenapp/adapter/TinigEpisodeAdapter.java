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
import com.example.havenapp.model.TinigEpisode;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TinigEpisodeAdapter
        extends RecyclerView.Adapter<TinigEpisodeAdapter.EpisodeViewHolder> {

    private final Context context;
    private final List<TinigEpisode> episodes;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public TinigEpisodeAdapter(
            Context context,
            List<TinigEpisode> episodes
    ) {

        this.context = context;
        this.episodes = episodes;
    }


    // =====================================================
    // CREATE CARD
    // =====================================================

    @NonNull
    @Override
    public EpisodeViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(
                                R.layout.item_tinig_episode,
                                parent,
                                false
                        );

        return new EpisodeViewHolder(view);
    }


    // =====================================================
    // DISPLAY EPISODE DATA
    // =====================================================

    @Override
    public void onBindViewHolder(
            @NonNull EpisodeViewHolder holder,
            int position
    ) {

        TinigEpisode episode =
                episodes.get(position);


        // -------------------------
        // TITLE
        // -------------------------

        String title =
                episode.getTitle();

        if (title == null
                || title.trim().isEmpty()) {

            holder.txtEpisodeTitle.setText(
                    "Tinig ng Kababaihan"
            );

        } else {

            holder.txtEpisodeTitle.setText(
                    title
            );
        }


        // -------------------------
        // DATE
        // -------------------------

        holder.txtEpisodeDate.setText(
                formatPublishedDate(
                        episode.getPublished_at()
                )
        );


        // -------------------------
        // DURATION
        // -------------------------

        holder.txtEpisodeDuration.setText(
                formatDuration(
                        episode.getDuration_seconds()
                )
        );


        // -------------------------
        // THUMBNAIL
        // -------------------------

        String thumbnailUrl =
                episode.getThumbnail_url();


        if (thumbnailUrl != null
                && !thumbnailUrl.trim().isEmpty()
                && thumbnailUrl.startsWith("http")) {

            Glide.with(context)
                    .load(thumbnailUrl)
                    .centerCrop()
                    .into(holder.imgEpisodeThumbnail);

        } else {

            holder.imgEpisodeThumbnail
                    .setImageDrawable(null);
        }


        // -------------------------
        // OPEN YOUTUBE
        // -------------------------

        holder.itemView.setOnClickListener(v ->

                openYouTubeEpisode(
                        episode.getYoutube_url()
                )
        );
    }


    // =====================================================
    // ITEM COUNT
    // =====================================================

    @Override
    public int getItemCount() {

        return episodes != null
                ? episodes.size()
                : 0;
    }


    // =====================================================
    // OPEN YOUTUBE SAFELY
    // =====================================================

    private void openYouTubeEpisode(
            String youtubeUrl
    ) {

        if (youtubeUrl == null
                || youtubeUrl.trim().isEmpty()) {

            Toast.makeText(
                    context,
                    "Episode link is unavailable.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        Uri uri;

        try {

            uri = Uri.parse(
                    youtubeUrl.trim()
            );

        } catch (Exception e) {

            Toast.makeText(
                    context,
                    "Invalid episode link.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        String scheme =
                uri.getScheme();


        if (scheme == null
                || (!scheme.equalsIgnoreCase("https")
                && !scheme.equalsIgnoreCase("http"))) {

            Toast.makeText(
                    context,
                    "Invalid episode link.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            uri
                    );

            context.startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    context,
                    "Unable to open this episode.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }


    // =====================================================
    // FORMAT DURATION
    // =====================================================

    private String formatDuration(
            Integer durationSeconds
    ) {

        if (durationSeconds == null
                || durationSeconds <= 0) {

            return "";
        }


        int totalMinutes =
                durationSeconds / 60;


        if (totalMinutes < 60) {

            return totalMinutes + " min";
        }


        int hours =
                totalMinutes / 60;

        int minutes =
                totalMinutes % 60;


        if (minutes == 0) {

            return hours + " hr";
        }


        return hours
                + " hr "
                + minutes
                + " min";
    }


    // =====================================================
    // FORMAT SUPABASE DATE
    // =====================================================

    private String formatPublishedDate(
            String publishedAt
    ) {

        if (publishedAt == null
                || publishedAt.trim().isEmpty()) {

            return "";
        }


        try {

            /*
             * Supabase normally returns something like:
             *
             * 2026-08-20T16:00:00+00:00
             *
             * or:
             *
             * 2026-08-20T16:00:00.000000+00:00
             *
             * We only need the YYYY-MM-DD portion.
             */

            if (publishedAt.length() >= 10) {

                String dateOnly =
                        publishedAt.substring(
                                0,
                                10
                        );


                SimpleDateFormat input =
                        new SimpleDateFormat(
                                "yyyy-MM-dd",
                                Locale.US
                        );


                SimpleDateFormat output =
                        new SimpleDateFormat(
                                "MMM d, yyyy",
                                Locale.US
                        );


                Date date =
                        input.parse(
                                dateOnly
                        );


                if (date != null) {

                    return output.format(
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

    static class EpisodeViewHolder
            extends RecyclerView.ViewHolder {

        ImageView imgEpisodeThumbnail;

        TextView txtEpisodeTitle;
        TextView txtEpisodeDate;
        TextView txtEpisodeDuration;


        EpisodeViewHolder(
                @NonNull View itemView
        ) {

            super(itemView);


            imgEpisodeThumbnail =
                    itemView.findViewById(
                            R.id.imgEpisodeThumbnail
                    );


            txtEpisodeTitle =
                    itemView.findViewById(
                            R.id.txtEpisodeTitle
                    );


            txtEpisodeDate =
                    itemView.findViewById(
                            R.id.txtEpisodeDate
                    );


            txtEpisodeDuration =
                    itemView.findViewById(
                            R.id.txtEpisodeDuration
                    );
        }
    }
}