package com.example.havenapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.havenapp.R;
import com.example.havenapp.model.EducationalArticle;

import java.util.List;


public class EducationalArticleAdapter
        extends RecyclerView.Adapter<
        EducationalArticleAdapter.EducationalArticleViewHolder> {


    // =====================================================
    // CLICK LISTENER
    // =====================================================

    public interface OnArticleClickListener {

        void onArticleClick(
                EducationalArticle article
        );
    }


    // =====================================================
    // DATA
    // =====================================================

    private final List<EducationalArticle> articleList;

    private final OnArticleClickListener
            onArticleClickListener;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public EducationalArticleAdapter(
            List<EducationalArticle> articleList,
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
    public EducationalArticleViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater
                        .from(
                                parent.getContext()
                        )
                        .inflate(
                                R.layout.item_educational_article,
                                parent,
                                false
                        );


        return new EducationalArticleViewHolder(
                view
        );
    }


    // =====================================================
    // BIND ARTICLE
    // =====================================================

    @Override
    public void onBindViewHolder(
            @NonNull EducationalArticleViewHolder holder,
            int position
    ) {

        EducationalArticle article =
                articleList.get(
                        position
                );


        // -------------------------------------------------
        // TITLE
        // -------------------------------------------------

        String title =
                article.getTitle();


        if (
                title == null
                        ||
                        title.trim().isEmpty()
        ) {

            title =
                    "Educational Article";
        }


        holder.txtArticleTitle.setText(
                title
        );


        // -------------------------------------------------
        // SUBTITLE
        // -------------------------------------------------

        String subtitle =
                article.getSubtitle();


        if (
                subtitle == null
                        ||
                        subtitle.trim().isEmpty()
        ) {

            holder.txtArticleSubtitle
                    .setVisibility(
                            View.GONE
                    );

        } else {

            holder.txtArticleSubtitle
                    .setVisibility(
                            View.VISIBLE
                    );


            holder.txtArticleSubtitle
                    .setText(
                            subtitle
                    );
        }


        // -------------------------------------------------
        // SOURCE
        // -------------------------------------------------

        String sourceName =
                article.getSourceName();


        if (
                sourceName == null
                        ||
                        sourceName.trim().isEmpty()
        ) {

            holder.txtArticleSource
                    .setVisibility(
                            View.GONE
                    );

        } else {

            holder.txtArticleSource
                    .setVisibility(
                            View.VISIBLE
                    );


            holder.txtArticleSource
                    .setText(
                            sourceName
                    );
        }


        // -------------------------------------------------
        // EDUCATIONAL ARTICLE ICON
        // -------------------------------------------------
        // Always display the original ic_articles drawable.
        // No Glide.
        // No thumbnail URL.
        // No purple tint.
        // -------------------------------------------------

        holder.imgArticleThumbnail
                .clearColorFilter();


        holder.imgArticleThumbnail
                .setImageTintList(
                        null
                );


        holder.imgArticleThumbnail
                .setImageResource(
                        R.drawable.ic_articles
                );


        int padding =
                (int) (
                        4
                                *
                                holder
                                        .itemView
                                        .getResources()
                                        .getDisplayMetrics()
                                        .density
                );


        holder.imgArticleThumbnail
                .setPadding(
                        padding,
                        padding,
                        padding,
                        padding
                );


        holder.imgArticleThumbnail
                .setScaleType(
                        ImageView.ScaleType.CENTER_INSIDE
                );


        // -------------------------------------------------
        // CARD CLICK
        // -------------------------------------------------

        holder.itemView
                .setOnClickListener(v -> {

                    if (
                            onArticleClickListener
                                    != null
                    ) {

                        onArticleClickListener
                                .onArticleClick(
                                        article
                                );
                    }
                });
    }


    // =====================================================
    // ITEM COUNT
    // =====================================================

    @Override
    public int getItemCount() {

        if (
                articleList == null
        ) {

            return 0;
        }


        return articleList.size();
    }


    // =====================================================
    // VIEW HOLDER
    // =====================================================

    static class EducationalArticleViewHolder
            extends RecyclerView.ViewHolder {


        ImageView imgArticleThumbnail;

        TextView txtArticleTitle;

        TextView txtArticleSubtitle;

        TextView txtArticleSource;


        public EducationalArticleViewHolder(
                @NonNull View itemView
        ) {

            super(
                    itemView
            );


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