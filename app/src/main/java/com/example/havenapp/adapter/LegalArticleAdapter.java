package com.example.havenapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.havenapp.R;
import com.example.havenapp.model.LegalArticle;

import java.util.List;


public class LegalArticleAdapter
        extends RecyclerView.Adapter<LegalArticleAdapter.LegalArticleViewHolder> {


    // =====================================================
    // CLICK LISTENER
    // =====================================================

    public interface OnArticleClickListener {

        void onArticleClick(
                LegalArticle article
        );
    }


    // =====================================================
    // VARIABLES
    // =====================================================

    private final List<LegalArticle> articleList;

    private final OnArticleClickListener
            onArticleClickListener;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public LegalArticleAdapter(
            List<LegalArticle> articleList,
            OnArticleClickListener onArticleClickListener
    ) {

        this.articleList =
                articleList;

        this.onArticleClickListener =
                onArticleClickListener;
    }


    // =====================================================
    // CREATE VIEW HOLDER
    // =====================================================

    @NonNull
    @Override
    public LegalArticleViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(
                                R.layout.item_legal_article,
                                parent,
                                false
                        );

        return new LegalArticleViewHolder(
                view
        );
    }


    // =====================================================
    // BIND ARTICLE
    // =====================================================

    @Override
    public void onBindViewHolder(
            @NonNull LegalArticleViewHolder holder,
            int position
    ) {

        LegalArticle article =
                articleList.get(position);


        // =================================================
        // TITLE
        // =================================================

        String title =
                article.getTitle();

        if (title == null
                || title.trim().isEmpty()) {

            title =
                    "Legal Rights Article";
        }

        holder.txtArticleTitle.setText(
                title
        );


        // =================================================
        // SUBTITLE
        // =================================================

        String subtitle =
                article.getSubtitle();

        if (subtitle == null
                || subtitle.trim().isEmpty()) {

            holder.txtArticleSubtitle
                    .setVisibility(View.GONE);

        } else {

            holder.txtArticleSubtitle
                    .setVisibility(View.VISIBLE);

            holder.txtArticleSubtitle
                    .setText(subtitle);
        }


        // =================================================
        // SOURCE
        // =================================================

        String sourceName =
                article.getSourceName();

        if (sourceName == null
                || sourceName.trim().isEmpty()) {

            holder.txtArticleSource
                    .setVisibility(View.GONE);

        } else {

            holder.txtArticleSource
                    .setVisibility(View.VISIBLE);

            holder.txtArticleSource
                    .setText(sourceName);
        }


        // =================================================
        // THUMBNAIL
        // =================================================

        String thumbnailUrl =
                article.getThumbnailUrl();

        if (thumbnailUrl != null
                && !thumbnailUrl.trim().isEmpty()) {

            Glide.with(
                            holder.itemView.getContext()
                    )
                    .load(thumbnailUrl)
                    .placeholder(
                            R.drawable.ic_legalrights
                    )
                    .error(
                            R.drawable.ic_legalrights
                    )
                    .into(
                            holder.imgArticleThumbnail
                    );

        } else {

            // No thumbnail stored in Supabase.
            // Use HavenApp Legal Rights icon.

            holder.imgArticleThumbnail
                    .setImageResource(
                            R.drawable.ic_legalrights
                    );
        }


        // =================================================
        // WHOLE CARD CLICK
        // =================================================

        holder.itemView.setOnClickListener(
                v -> {

                    if (onArticleClickListener
                            != null) {

                        onArticleClickListener
                                .onArticleClick(
                                        article
                                );
                    }
                }
        );
    }


    // =====================================================
    // ITEM COUNT
    // =====================================================

    @Override
    public int getItemCount() {

        if (articleList == null) {
            return 0;
        }

        return articleList.size();
    }


    // =====================================================
    // VIEW HOLDER
    // =====================================================

    static class LegalArticleViewHolder
            extends RecyclerView.ViewHolder {


        ImageView
                imgArticleThumbnail;

        TextView
                txtArticleTitle,
                txtArticleSubtitle,
                txtArticleSource;


        public LegalArticleViewHolder(
                @NonNull View itemView
        ) {

            super(itemView);


            imgArticleThumbnail =
                    itemView.findViewById(
                            R.id.imgArticleThumbnail
                    );


            txtArticleTitle =
                    itemView.findViewById(
                            R.id.txtArticleTitle
                    );


            txtArticleSubtitle =
                    itemView.findViewById(
                            R.id.txtArticleSubtitle
                    );


            txtArticleSource =
                    itemView.findViewById(
                            R.id.txtArticleSource
                    );
        }
    }
}